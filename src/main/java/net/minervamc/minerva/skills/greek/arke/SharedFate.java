package net.minervamc.minerva.skills.greek.arke;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Arke PASSIVE engine — Shared Fate. Enemies linked into a chain share their fate: damaging one
 * echoes a fraction onto every other member of that chain. Chains are drawn as steady, desaturated
 * rainbow DUST lines between consecutive members; they expire after ~120 ticks or break if any two
 * members drift more than 10 blocks apart. The whole kit (Reel, Cinch) exists to keep linked
 * enemies clustered, because they want to spread to snap the links.
 */
public class SharedFate extends Skill {
    /** Desaturated, washed-out rainbow — Arke's signature palette (spatial gradient). */
    public static final Color[] PALETTE = {
        Color.fromRGB(150, 120, 120), Color.fromRGB(150, 145, 115),
        Color.fromRGB(120, 150, 120), Color.fromRGB(115, 130, 150),
        Color.fromRGB(140, 120, 150),
    };

    /** A single chain: who owns it, its ordered members, and when it expires (ms epoch). */
    public static class Chain {
        public UUID owner;
        public List<UUID> members;
        public long expire;

        public Chain(UUID owner, List<UUID> members, long expire) {
            this.owner = owner;
            this.members = members;
            this.expire = expire;
        }
    }

    private static final List<Chain> CHAINS = new ArrayList<>();
    /** Guard so an echo's own damage doesn't recursively re-trigger more echoes. */
    private static boolean ECHOING = false;
    private static boolean VISUALS_RUNNING = false;

    /** Resolve a UUID to a still-valid LivingEntity, or null. */
    private static LivingEntity resolve(UUID id) {
        Entity e = Bukkit.getEntity(id);
        if (e instanceof LivingEntity le && !le.isDead()) return le;
        return null;
    }

    /** True if two members of the chain have drifted past the break distance (10 blocks). */
    private static boolean chainTooStretched(Chain chain) {
        List<LivingEntity> live = new ArrayList<>();
        for (UUID id : chain.members) {
            LivingEntity le = resolve(id);
            if (le != null) live.add(le);
        }
        if (live.size() < 2) return true;
        for (int i = 0; i < live.size(); i++) {
            for (int j = i + 1; j < live.size(); j++) {
                if (live.get(i).getWorld() != live.get(j).getWorld()) return true;
                if (live.get(i).getLocation().distance(live.get(j).getLocation()) > 10.0) return true;
            }
        }
        return false;
    }

    /**
     * REQUIRED HOOK — link the given targets (plus implicitly each other) into a new shared-fate
     * chain owned by {@code owner}. Higher levels keep the chain alive longer.
     */
    public static void link(Player owner, java.util.List<org.bukkit.entity.LivingEntity> targets, int level) {
        if (owner == null || targets == null || targets.isEmpty()) return;
        List<UUID> members = new ArrayList<>();
        for (LivingEntity le : targets) {
            if (le == null || le.isDead() || le == owner) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
            if (PlayerStats.isSummoned(owner, le)) continue;
            if (le.hasMetadata("NPC")) continue;
            if (!members.contains(le.getUniqueId())) members.add(le.getUniqueId());
        }
        if (members.size() < 2) return;

        long durationTicks = switch (level) {
            case 3 -> 180L;
            case 4, 5 -> 200L;
            default -> 120L;
        };
        long expire = System.currentTimeMillis() + durationTicks * 50L;
        CHAINS.add(new Chain(owner.getUniqueId(), members, expire));

        // L4+: links also slow members slightly while bound.
        if (level >= 4) {
            for (UUID id : members) {
                LivingEntity le = resolve(id);
                if (le != null) {
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, (int) durationTicks, 0));
                }
            }
        }

        startVisuals();
    }

    /** REQUIRED HOOK — every chain that contains the given member, flattened to live entities. */
    public static java.util.List<org.bukkit.entity.LivingEntity> getChain(org.bukkit.entity.LivingEntity member) {
        List<LivingEntity> out = new ArrayList<>();
        if (member == null) return out;
        UUID id = member.getUniqueId();
        for (Chain chain : CHAINS) {
            if (chain.members.contains(id)) {
                for (UUID m : chain.members) {
                    LivingEntity le = resolve(m);
                    if (le != null && !out.contains(le)) out.add(le);
                }
            }
        }
        return out;
    }

    /** REQUIRED HOOK — all living enemies currently chained by the given owner. */
    public static java.util.List<org.bukkit.entity.LivingEntity> getChainOf(Player owner) {
        List<LivingEntity> out = new ArrayList<>();
        if (owner == null) return out;
        UUID oid = owner.getUniqueId();
        for (Chain chain : CHAINS) {
            if (!chain.owner.equals(oid)) continue;
            for (UUID m : chain.members) {
                LivingEntity le = resolve(m);
                if (le != null && !out.contains(le)) out.add(le);
            }
        }
        return out;
    }

    /**
     * REQUIRED HOOK — called when the owner deals damage to a victim. If the victim is in one of
     * the owner's chains, echo a fraction of the damage to every OTHER member of that chain. The
     * ECHOING guard prevents the echo's own damage call from cascading into further echoes.
     */
    public static void onOwnerDealDamage(Player owner, org.bukkit.entity.LivingEntity victim, double dmg, int level) {
        if (ECHOING) return;
        if (owner == null || victim == null || victim.isDead()) return;

        UUID oid = owner.getUniqueId();
        double echoPct = 0.25 + 0.025 * level;
        double echoDmg = dmg * echoPct;
        if (echoDmg <= 0) return;

        ECHOING = true;
        try {
            for (Chain chain : CHAINS) {
                if (!chain.owner.equals(oid)) continue;
                if (!chain.members.contains(victim.getUniqueId())) continue;
                for (UUID m : chain.members) {
                    if (m.equals(victim.getUniqueId())) continue;
                    LivingEntity other = resolve(m);
                    if (other == null) continue;
                    if (other instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                    if (PlayerStats.isSummoned(owner, other)) continue;
                    damage(other, echoDmg, owner);
                    pulse(victim, other);
                }
            }
        } finally {
            ECHOING = false;
        }
    }

    /** A single-shot echo pulse: a desaturated dust line from victim to the linked member. */
    private static void pulse(LivingEntity from, LivingEntity to) {
        if (from.getWorld() != to.getWorld()) return;
        World w = from.getWorld();
        Vector a = from.getLocation().clone().add(0, from.getHeight() / 2, 0).toVector();
        Vector b = to.getLocation().clone().add(0, to.getHeight() / 2, 0).toVector();
        for (Vector v : ParticleUtils.getLinePoints(a, b, 0.5)) {
            w.spawnParticle(Particle.DUST, v.toLocation(w), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(PALETTE[2], 0.9f));
        }
        w.playSound(to.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.7f, 0.9f);
    }

    /** Background runnable: prunes dead/stretched/expired chains and draws the link lines. */
    private static void startVisuals() {
        if (VISUALS_RUNNING) return;
        VISUALS_RUNNING = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (CHAINS.isEmpty()) {
                    VISUALS_RUNNING = false;
                    cancel();
                    return;
                }
                long now = System.currentTimeMillis();
                Iterator<Chain> it = CHAINS.iterator();
                while (it.hasNext()) {
                    Chain chain = it.next();
                    if (now >= chain.expire || chainTooStretched(chain)) {
                        it.remove();
                        continue;
                    }
                    drawChain(chain);
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 2L);
    }

    /** Draw steady desaturated-rainbow dust lines between consecutive live members. */
    private static void drawChain(Chain chain) {
        List<LivingEntity> live = new ArrayList<>();
        for (UUID id : chain.members) {
            LivingEntity le = resolve(id);
            if (le != null) live.add(le);
        }
        if (live.size() < 2) return;
        for (int i = 0; i < live.size() - 1; i++) {
            LivingEntity x = live.get(i);
            LivingEntity y = live.get(i + 1);
            if (x.getWorld() != y.getWorld()) continue;
            World w = x.getWorld();
            Vector a = x.getLocation().clone().add(0, x.getHeight() / 2, 0).toVector();
            Vector b = y.getLocation().clone().add(0, y.getHeight() / 2, 0).toVector();
            // Spatial gradient: color picked by position along the line (steady, no per-frame flicker).
            List<Vector> pts = ParticleUtils.getLinePoints(a, b, 0.45);
            int n = pts.size();
            for (int k = 0; k < n; k++) {
                Color c = PALETTE[(k * PALETTE.length) / Math.max(1, n)];
                w.spawnParticle(Particle.DUST, pts.get(k).toLocation(w), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(c, 0.85f));
            }
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Linked enemies share fate: hurt one, hurt them all.";
            case 2 -> ChatColor.GRAY + "Stronger echo damage down the chain.";
            case 3 -> ChatColor.GRAY + "Chains hold together longer before fading.";
            case 4 -> ChatColor.GRAY + "Even longer chains; links also slow the bound.";
            case 5 -> ChatColor.GRAY + "Maximum echo: shared fate is inescapable while linked.";
            default -> ChatColor.GRAY + "Linked enemies share a fraction of damage taken.";
        };
    }

    @Override
    public String toString() {
        return "sharedFate";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_CHAIN),
                ChatColor.GRAY + "" + ChatColor.BOLD + "[Shared Fate]",
                ChatColor.GRAY + "Enemies bound into a " + ChatColor.WHITE + "chain" + ChatColor.GRAY + " share their fate —",
                ChatColor.GRAY + "damage to one " + ChatColor.WHITE + "echoes" + ChatColor.GRAY + " onto every other link.",
                ChatColor.GRAY + "Links snap if the bound drift too far apart.");
    }
}
