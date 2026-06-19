package net.minervamc.minerva.skills.greek.arke;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Arke RLL (damage) — the chain capitalizer. Whip the aimed enemy; if it's chained, the hit
 * ECHOES along its Shared Fate link, dealing a fraction to each bound member. Against an unlinked
 * target it lands a modest single hit. Levels raise the echo, add a desaturate (WEAKNESS) debuff,
 * bounce to and link a fresh enemy, and refund cooldown on a kill.
 */
public class TarnishedLash extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 5500;
            case 4, 5 -> 5000;
            default -> 6000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tarnishedLash")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 6);
        if (target == null) {
            player.sendActionBar(ChatColor.GRAY + "Nothing in reach of your lash!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tarnishedLash", cooldown);
        cooldownAlarm(player, cooldown, "Tarnished Lash");

        double mainDmg = switch (level) {
            case 2 -> 9;
            case 3 -> 9;
            case 4 -> 10;
            case 5 -> 11;
            default -> 8;
        };
        double echoPct = switch (level) {
            case 2, 3 -> 0.5;
            case 4, 5 -> 0.55;
            default -> 0.4;
        };

        // whip arc telegraph from the caster to the target (desaturated chain-whip)
        lashArc(player, target);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.9f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 1.2f);

        double tHealthBefore = target.getHealth();
        damage(target, mainDmg, player);

        List<LivingEntity> chain = SharedFate.getChain(target);
        if (chain.size() >= 2) {
            // ECHO: ripple the hit down the chain to each OTHER member.
            double echoDmg = mainDmg * echoPct;
            for (LivingEntity le : chain) {
                if (le == target) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                if (PlayerStats.isSummoned(player, le)) continue;
                damage(le, echoDmg, player);
                echoPulse(target, le);
                if (level >= 3) {
                    // L3: echo also stamps a desaturate debuff — they deal less damage.
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                }
            }
        } else {
            // unlinked: just the single modest hit, with a flicker of impact
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, target.getHeight() / 2, 0),
                    8, 0.2, 0.3, 0.2, 0.05);
        }

        // L4: bounce to one nearby unlinked enemy and link it to the target.
        if (level >= 4) {
            LivingEntity bounce = nearestUnlinked(player, target, 6);
            if (bounce != null) {
                lashArc(target, bounce);
                damage(bounce, mainDmg * 0.5, player);
                List<LivingEntity> newLink = new ArrayList<>();
                newLink.add(target);
                newLink.add(bounce);
                SharedFate.link(player, newLink, level);
                bounce.getWorld().playSound(bounce.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8f, 1.1f);
            }
        }

        // L5: a kill refunds part of the cooldown.
        if (level >= 5 && (target.isDead() || target.getHealth() <= 0.1) && tHealthBefore > 0) {
            long refunded = cooldown - 2500;
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "tarnishedLash", Math.max(0, refunded));
            player.sendActionBar(ChatColor.GRAY + "The chain feeds you — Lash refreshed!");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.4f);
        }
    }

    /** Single-shot desaturated whip arc (bezier curve) from one entity to another. */
    private static void lashArc(Entity from, Entity to) {
        World w = from.getWorld();
        if (w != to.getWorld()) return;
        Vector a = (from instanceof Player p ? p.getEyeLocation().toVector()
                : from.getLocation().clone().add(0, from.getHeight() / 2, 0).toVector());
        Vector b = to.getLocation().clone().add(0, (to instanceof LivingEntity le ? le.getHeight() / 2 : 1), 0).toVector();
        Vector mid = a.clone().add(b).multiply(0.5).add(new Vector(0, 1.2, 0));
        List<Vector> pts = ParticleUtils.getNthBezierPoints(16, a, mid, b);
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            org.bukkit.Color c = SharedFate.PALETTE[(i * SharedFate.PALETTE.length) / Math.max(1, n)];
            w.spawnParticle(Particle.DUST, pts.get(i).toLocation(w), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, 1f));
        }
    }

    /** Single-shot echo pulse line between target and a linked member. */
    private static void echoPulse(LivingEntity from, LivingEntity to) {
        World w = from.getWorld();
        if (w != to.getWorld()) return;
        Vector a = from.getLocation().clone().add(0, from.getHeight() / 2, 0).toVector();
        Vector b = to.getLocation().clone().add(0, to.getHeight() / 2, 0).toVector();
        for (Vector v : ParticleUtils.getLinePoints(a, b, 0.5)) {
            w.spawnParticle(Particle.DUST, v.toLocation(w), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(SharedFate.PALETTE[4], 0.9f));
        }
        to.getWorld().spawnParticle(Particle.CRIT, b.toLocation(w), 5, 0.2, 0.3, 0.2, 0.05);
        w.playSound(to.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.7f, 0.8f);
    }

    /** Nearest enemy to {@code around} that is not already in {@code around}'s chain. */
    private static LivingEntity nearestUnlinked(Player player, LivingEntity around, double range) {
        List<LivingEntity> existing = SharedFate.getChain(around);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : around.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player || e == around) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (le.hasMetadata("NPC")) continue;
            if (existing.contains(le)) continue;
            double d = le.getLocation().distanceSquared(around.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Whip a target; the hit echoes down its chain.";
            case 2 -> ChatColor.GRAY + "Stronger echo along the chain.";
            case 3 -> ChatColor.GRAY + "The echo also weakens linked enemies.";
            case 4 -> ChatColor.GRAY + "Bounces to a fresh enemy and links it.";
            case 5 -> ChatColor.GRAY + "A kill refunds part of the cooldown.";
            default -> ChatColor.GRAY + "Whip a target; chained foes share the hit.";
        };
    }

    @Override
    public String toString() {
        return "tarnishedLash";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_NUGGET),
                ChatColor.GRAY + "" + ChatColor.BOLD + "[Tarnished Lash]",
                ChatColor.GRAY + "Whip your target. If it is " + ChatColor.WHITE + "chained" + ChatColor.GRAY + ", the",
                ChatColor.GRAY + "blow " + ChatColor.WHITE + "echoes" + ChatColor.GRAY + " along the link to every bound foe.",
                ChatColor.GRAY + "Otherwise, a single tarnished strike.");
    }
}
