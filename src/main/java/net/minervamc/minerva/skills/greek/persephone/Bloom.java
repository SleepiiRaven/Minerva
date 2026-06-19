package net.minervamc.minerva.skills.greek.persephone;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Persephone RLR — the spring half. Spend 1-3 seeds to grow a life patch that heals party allies
 * over 3s and leaves blossoms. Each Bloom registers a {@link BloomPatch} that Wither can later
 * detonate into a death-burst (the signature combo). Rising pink/green petals, steady — no strobe.
 */
public class Bloom extends Skill {
    /** A growing patch of life that Wither can convert into a death-burst. */
    public static class BloomPatch {
        public final Location center;
        public final double radius;
        public long expire;

        public BloomPatch(Location center, double radius, long expire) {
            this.center = center;
            this.radius = radius;
            this.expire = expire;
        }
    }

    public static final List<BloomPatch> PATCHES = new ArrayList<>();

    /** Register a patch at a location (used by Bloom itself and Descent's free-patch leveling). */
    public static BloomPatch registerPatch(Location center, double radius, long lifeMillis) {
        prunePatches();
        BloomPatch patch = new BloomPatch(center.clone(), radius, System.currentTimeMillis() + lifeMillis);
        PATCHES.add(patch);
        return patch;
    }

    private static void prunePatches() {
        long now = System.currentTimeMillis();
        PATCHES.removeIf(p -> now > p.expire);
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 7500;
            case 4, 5 -> 7000;
            default -> 8000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "bloom")) {
            onCooldown(player);
            return;
        }

        int available = SeedsOfTheUnderworld.getSeeds(player);
        if (available <= 0) {
            skillLocked(player, "you have no Pomegranate Seeds");
            return;
        }
        int seeds = Math.min(3, available);
        if (!SeedsOfTheUnderworld.spendSeeds(player, seeds)) {
            skillLocked(player, "you have no Pomegranate Seeds");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "bloom", cooldown);
        cooldownAlarm(player, cooldown, "Bloom");

        double radius = (level >= 4 ? 2.6 : 2.0) + seeds * 0.6;
        double healPerTick = (switch (level) {
            case 2 -> 2.0;
            case 3 -> 2.4;
            case 4, 5 -> 2.8;
            default -> 1.6;
        }) * seeds;
        final int fLevel = level;
        final Location center = player.getLocation().clone();

        registerPatch(center, radius, 12000L);

        player.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.1f);
        player.getWorld().playSound(center, Sound.BLOCK_MOSS_PLACE, 0.8f, 1.2f);

        // ground blossom ring (steady DUST flowers)
        for (Vector v : ParticleUtils.getCirclePoints(radius, Math.max(12, (int) (radius * 8)))) {
            Color c = SeedsOfTheUnderworld.PALETTE[v.getX() > 0 ? 0 : 1];
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, 1.1f));
        }

        // heal-over-time runnable (3 seconds, every 0.5s = 6 pulses)
        new BukkitRunnable() {
            int pulses = 0;
            @Override
            public void run() {
                if (pulses >= 6 || !player.isOnline()) {
                    cancel();
                    return;
                }
                // rising pink/green petals, steady
                for (Vector v : ParticleUtils.getFilledCirclePoints(radius, 10)) {
                    Location loc = center.clone().add(v).add(0, 0.2 + Math.random() * 0.6, 0);
                    center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 0, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, 0.6, 0), 4, radius * 0.4, 0.5, radius * 0.4, 0);

                List<Player> allies = alliesOf(player);
                for (Player ally : allies) {
                    if (!ally.isOnline()) continue;
                    if (ally.getLocation().distanceSquared(center) > radius * radius) continue;
                    if (ally.getHealth() <= 0) continue;
                    double max = ally.getMaxHealth();
                    ally.setHealth(Math.min(max, ally.getHealth() + healPerTick));
                    ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().clone().add(0, 1.2, 0), 4, 0.3, 0.4, 0.3, 0);
                    ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.4f, 1.3f);
                    if (fLevel >= 3) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 0));
                    }
                    if (fLevel >= 5) {
                        cleanseOne(ally);
                    }
                }
                pulses++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 10L);
    }

    private static List<Player> alliesOf(Player player) {
        List<Player> allies = new ArrayList<>();
        allies.add(player);
        List<Player> party = Party.partyList(player);
        if (party != null) {
            for (Player p : party) {
                if (p != null && !allies.contains(p)) allies.add(p);
            }
        }
        return allies;
    }

    private static void cleanseOne(Player ally) {
        for (PotionEffect eff : ally.getActivePotionEffects()) {
            PotionEffectType type = eff.getType();
            if (type.equals(PotionEffectType.SLOWNESS) || type.equals(PotionEffectType.WEAKNESS)
                    || type.equals(PotionEffectType.WITHER) || type.equals(PotionEffectType.POISON)
                    || type.equals(PotionEffectType.MINING_FATIGUE) || type.equals(PotionEffectType.BLINDNESS)
                    || type.equals(PotionEffectType.NAUSEA)) {
                ally.removePotionEffect(type);
                return;
            }
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Grow a life patch; heal allies inside over 3s.";
            case 2 -> ChatColor.GRAY + "Heals more per seed spent.";
            case 3 -> ChatColor.GRAY + "Blossoms grant allies brief Speed.";
            case 4 -> ChatColor.GRAY + "Larger patch.";
            case 5 -> ChatColor.GRAY + "Also cleanses one debuff from allies inside.";
            default -> ChatColor.GRAY + "Grow a healing patch of blossoms.";
        };
    }

    @Override
    public String toString() {
        return "bloom";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PINK_TULIP),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Bloom]",
                ChatColor.GRAY + "Spend up to " + ChatColor.LIGHT_PURPLE + "3 seeds" + ChatColor.GRAY + " to grow a patch of life,",
                ChatColor.GRAY + "healing party allies inside it over time and leaving",
                ChatColor.GREEN + "blossoms" + ChatColor.GRAY + " that " + ChatColor.DARK_PURPLE + "Wither" + ChatColor.GRAY + " can detonate.");
    }
}
