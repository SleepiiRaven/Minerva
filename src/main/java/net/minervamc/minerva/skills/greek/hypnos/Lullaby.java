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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hypnos RLR — a soothing lullaby. A frontal cone applies Drowsiness (boosted vs already-drowsy
 * foes) while a descending note-block melody plays. Soft-blue dust cone + drifting notes. Higher
 * ranks widen the cone, lengthen the song, and leave a lingering mist that keeps applying.
 */
public class Lullaby extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "lullaby")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "lullaby", cooldown);
        cooldownAlarm(player, cooldown, "Lullaby");

        double range = 7;
        double minDot = level >= 2 ? 0.45 : 0.6; // L2 widens the cone
        double baseDrowsy = level >= 3 ? 42 : 35; // L3 extra Drowsiness
        final int fLevel = level;

        playSong(player, level);

        // soft-blue DUST cone (spatial blue->indigo gradient by distance)
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        for (double d = 1; d <= range; d += 0.6) {
            int idx = (int) ((d / range) * (Sandman.DREAM.length - 1));
            Location ring = eye.clone().add(dir.clone().multiply(d));
            double r = 0.4 + d * 0.18;
            for (Vector v : ParticleUtils.getVerticalCirclePoints(r, eye.getPitch(), eye.getYaw(), 8)) {
                ring.getWorld().spawnParticle(Particle.DUST, ring.clone().add(v), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(Sandman.DREAM[idx], 1f));
            }
            ring.getWorld().spawnParticle(Particle.NOTE, ring, 1, 0.2, 0.2, 0.2, 0.4);
        }

        applyCone(player, range, minDot, baseDrowsy, fLevel);

        // L5: leaves a faint lingering mist that keeps applying for a short while
        if (level >= 5) {
            final Location anchor = eye.clone().add(dir.clone().multiply(range * 0.5));
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (t >= 60 || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    anchor.getWorld().spawnParticle(Particle.DUST, anchor.clone().add(0, 0.5, 0), 6, 1.5, 0.8, 1.5, 0,
                            new Particle.DustOptions(Sandman.DREAM[3], 1f));
                    if (t % 20 == 0) {
                        for (Entity e : anchor.getWorld().getNearbyEntities(anchor, 3, 2, 3)) {
                            if (!(e instanceof LivingEntity le) || e == player) continue;
                            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                            if (PlayerStats.isSummoned(player, le)) continue;
                            Sandman.addDrowsiness(player, le, 12, fLevel);
                        }
                    }
                    t += 5;
                }
            }.runTaskTimer(Minerva.getInstance(), 5L, 5L);
        }
    }

    private static void applyCone(Player player, double range, double minDot, double baseDrowsy, int level) {
        Vector dir = player.getEyeLocation().getDirection();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            Vector to = le.getLocation().toVector().subtract(player.getEyeLocation().toVector());
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            if (to.normalize().dot(dir) < minDot) continue;
            double amt = baseDrowsy;
            if (Sandman.getDrowsiness(le) > 0) amt *= 1.5; // x1.5 vs already-drowsy
            Sandman.addDrowsiness(player, le, amt, level);
        }
    }

    /** A soothing descending lullaby played over ~1 second on soft note-block instruments. */
    private static void playSong(Player player, int level) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, 1.5f);
        player.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 1.5f);
        final boolean longer = level >= 3; // L3 longer melody
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                Location l = player.getLocation();
                switch (step) {
                    case 0 -> {
                        player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.33f);
                        player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.7f, 1.33f);
                    }
                    case 1 -> player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.19f);
                    case 2 -> player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, 1.12f);
                    case 3 -> {
                        player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
                        player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.7f, 1f);
                        if (!longer) cancel();
                    }
                    case 4 -> player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 0.89f);
                    case 5 -> {
                        player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, 0.84f);
                        player.getWorld().playSound(l, Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 0.75f);
                        cancel();
                    }
                }
                step++;
            }
        }.runTaskTimer(Minerva.getInstance(), 4L, 4L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Sing a lullaby, building Drowsiness in a frontal cone.";
            case 2 -> ChatColor.GRAY + "Wider cone.";
            case 3 -> ChatColor.GRAY + "Longer melody and more Drowsiness.";
            case 4 -> ChatColor.GRAY + "Refined timing on the lullaby.";
            case 5 -> ChatColor.GRAY + "Leaves a lingering mist that keeps lulling enemies.";
            default -> ChatColor.GRAY + "Sing a lullaby that builds Drowsiness.";
        };
    }

    @Override
    public String toString() {
        return "lullaby";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.NOTE_BLOCK),
                ChatColor.BLUE + "" + ChatColor.BOLD + "[Lullaby]",
                ChatColor.GRAY + "Sing a soothing melody, washing " + ChatColor.BLUE + "Drowsiness",
                ChatColor.GRAY + "over the enemies before you — stronger on the already sleepy.");
    }
}
