package net.minervamc.minerva.skills.greek.iris;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Iris RRR — momentum-redirect flight. Each tick the player's velocity is reset to a fixed speed
 * along their look direction (smooth soar, usable in the air). A SPATIAL rainbow ribbon trails
 * behind — each segment's hue is fixed by its world position, never time-cycled (photosensitivity
 * safe). On end, SLOW_FALLING keeps the landing safe. Higher ranks let the trail damage enemies
 * it touches and grant party allies brief flight.
 */
public class IrisFlight extends Skill {
    // Full spatial rainbow — a segment's hue is chosen by a fixed position-derived index, never time.
    private static final Color[] RAINBOW = {
        Color.fromRGB(235, 64, 52),   // red
        Color.fromRGB(245, 140, 40),  // orange
        Color.fromRGB(240, 214, 50),  // yellow
        Color.fromRGB(64, 200, 84),   // green
        Color.fromRGB(58, 150, 240),  // blue
        Color.fromRGB(110, 80, 220),  // indigo
        Color.fromRGB(170, 70, 220),  // violet
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 9500;
            case 4, 5 -> 9000;
            default -> 10000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "irisFlight")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "irisFlight", cooldown);
        cooldownAlarm(player, cooldown, "Iris's Flight");

        final int durTicks = level >= 2 ? 80 : 60;
        final double speed = level >= 4 ? 1.05 : 0.95;
        final int fLevel = level;

        // rising harp arpeggio on lift
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.25f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || player.isDead() || !player.isOnline()) {
                    if (player.isOnline()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 30, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.8f, 1.2f);
                    }
                    cancel();
                    return;
                }

                // momentum redirect — true soar in any direction we look
                player.setVelocity(player.getEyeLocation().getDirection().multiply(speed));
                player.setFallDistance(0f);

                // spatial rainbow ribbon: hue fixed by world position, never time
                Location tail = player.getLocation().clone().add(0, 0.3, 0);
                Color hue = spatialHue(tail);
                tail.getWorld().spawnParticle(Particle.DUST, tail, 2, 0.12, 0.12, 0.12, 0,
                        new Particle.DustOptions(hue, 1.2f));
                if (t % 2 == 0) {
                    Location wing = player.getLocation().clone().add(0, 0.6, 0);
                    wing.getWorld().spawnParticle(Particle.GLOW, wing, 1, 0.2, 0.2, 0.2, 0);
                }

                // L3+: the trail refracts into anything it touches
                if (fLevel >= 3) {
                    for (Entity e : player.getNearbyEntities(1.6, 1.6, 1.6)) {
                        if (!(e instanceof LivingEntity le) || e == player) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, le)) continue;
                        if (le.getNoDamageTicks() > 0) continue;
                        damage(le, 3, player);
                        Prism.paintNext(player, le);
                    }
                }

                // L5: party allies brushed by the trail briefly gain flight too
                if (fLevel >= 5) {
                    List<Player> party = Party.partyList(player);
                    if (party != null) {
                        for (Player ally : party) {
                            if (ally == player || !ally.isOnline()) continue;
                            if (ally.getLocation().distanceSquared(player.getLocation()) <= 4) {
                                ally.setAllowFlight(true);
                                ally.setFlying(true);
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
                            }
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** Hue chosen from a fixed function of world position (spatial, never time-cycled). */
    private static Color spatialHue(Location loc) {
        int idx = (int) (Math.abs(Math.floor(loc.getX()) + Math.floor(loc.getY()) + Math.floor(loc.getZ()))) % RAINBOW.length;
        return RAINBOW[idx];
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Soar where you look on a rainbow trail (3s).";
            case 2 -> ChatColor.GRAY + "Longer flight (4s).";
            case 3 -> ChatColor.GRAY + "The trail damages and repaints enemies it touches.";
            case 4 -> ChatColor.GRAY + "Faster flight.";
            case 5 -> ChatColor.GRAY + "Party allies brushed by the trail briefly fly too.";
            default -> ChatColor.GRAY + "Soar where you look on a rainbow trail.";
        };
    }

    @Override
    public String toString() {
        return "irisFlight";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.FEATHER),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Iris's Flight]",
                ChatColor.GRAY + "Take to the sky, soaring wherever you look on a",
                ChatColor.GRAY + "trailing " + ChatColor.LIGHT_PURPLE + "rainbow ribbon" + ChatColor.GRAY + " — weave above the fight",
                ChatColor.GRAY + "to manage your palette.");
    }
}
