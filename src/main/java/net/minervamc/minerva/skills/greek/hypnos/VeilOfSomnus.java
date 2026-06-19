package net.minervamc.minerva.skills.greek.hypnos;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hypnos RRL — a dream-zone anchored where the caster stands (~5s). Enemies inside steadily gain
 * Drowsiness and DARKNESS; party allies inside are mended. Steady indigo dome + ground mist.
 * Higher ranks grow the zone, accelerate the Drowsiness, shield allies, and entrench sleepers.
 */
public class VeilOfSomnus extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 15000;
            case 4, 5 -> 14000;
            default -> 16000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "veilOfSomnus")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "veilOfSomnus", cooldown);
        cooldownAlarm(player, cooldown, "Veil of Somnus");

        final double radius = switch (level) {
            case 2, 3 -> 5.5;
            case 4 -> 6.0;
            case 5 -> 6.5;
            default -> 5.0;
        };
        final int durTicks = 100;
        final Location center = player.getLocation().clone();
        final int fLevel = level;
        final double drowsyPerSec = level >= 3 ? 14 : 10; // L3 faster Drowsiness

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.7f);
        center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, 0.7f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || !player.isOnline()) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.7f);
                    cancel();
                    return;
                }
                World world = center.getWorld();

                // steady indigo dome (single sweep per ring set, no strobe)
                if (t % 5 == 0) {
                    for (double pitch = 0; pitch <= 90; pitch += 30) {
                        double r = radius * Math.cos(Math.toRadians(pitch));
                        double y = radius * Math.sin(Math.toRadians(pitch));
                        for (Vector v : ParticleUtils.getCirclePoints(r, Math.max(12, (int) (r * 5)))) {
                            world.spawnParticle(Particle.DUST, center.clone().add(v.getX(), y + 0.1, v.getZ()), 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(Sandman.DREAM[1], 1.1f));
                        }
                    }
                    // ground mist
                    for (Vector v : ParticleUtils.getFilledCirclePoints(radius, 14)) {
                        world.spawnParticle(Particle.DUST, center.clone().add(v).add(0, 0.15, 0), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(Sandman.DREAM[3], 1f));
                    }
                }

                // affect entities each second
                if (t % 20 == 0) {
                    for (Entity e : world.getNearbyEntities(center, radius, 3, radius)) {
                        if (e == player) {
                            // caster counts as an ally
                            applyAlly(player, fLevel);
                            continue;
                        }
                        if (!(e instanceof LivingEntity le)) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) {
                            applyAlly(le, fLevel);
                            continue;
                        }
                        if (PlayerStats.isSummoned(player, le)) continue;
                        // enemy
                        Sandman.addDrowsiness(player, le, drowsyPerSec, fLevel);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                        // L5: sleeping enemies inside are kept asleep against minor jostling
                        if (fLevel >= 5 && isAsleep(le)) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 6));
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static void applyAlly(LivingEntity ally, int level) {
        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0));
        if (level >= 4) ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60, 0));
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Raise a dream-zone: Drowsiness and Darkness on enemies, mending allies.";
            case 2 -> ChatColor.GRAY + "Larger zone.";
            case 3 -> ChatColor.GRAY + "Drowsiness builds faster inside.";
            case 4 -> ChatColor.GRAY + "Allies inside also gain Absorption.";
            case 5 -> ChatColor.GRAY + "Sleepers inside resist waking from minor damage.";
            default -> ChatColor.GRAY + "Raise a dream-zone over the area.";
        };
    }

    @Override
    public String toString() {
        return "veilOfSomnus";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SOUL_LANTERN),
                ChatColor.BLUE + "" + ChatColor.BOLD + "[Veil of Somnus]",
                ChatColor.GRAY + "Raise a " + ChatColor.BLUE + "dream-zone" + ChatColor.GRAY + " around you. Enemies inside drift",
                ChatColor.GRAY + "into Drowsiness and Darkness; your allies are mended.");
    }
}
