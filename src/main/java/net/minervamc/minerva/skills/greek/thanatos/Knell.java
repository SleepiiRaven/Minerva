package net.minervamc.minerva.skills.greek.thanatos;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Thanatos RLR — the ranged mark applier. Hurl a tolling bell on an arc; where it lands, every
 * enemy within the impact radius gains a Death Timer (and Knell sets the execution threshold).
 * Since Thanatos can't dash in, this is how the clocks start at distance.
 */
public class Knell extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 10500;
            case 4, 5 -> 10000;
            default -> 11000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "knell")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "knell", cooldown);
        cooldownAlarm(player, cooldown, "Knell");

        double radius = switch (level) {
            case 2, 3 -> 5.0;
            case 4, 5 -> 5.5;
            default -> 4.0;
        };
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.6f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 0.6f);

        // launch the bell along an arc from the eye toward where the player looks.
        final Location start = player.getEyeLocation().clone();
        final Vector dir = start.getDirection().clone();
        new BukkitRunnable() {
            int t = 0;
            Location pos = start.clone();
            @Override
            public void run() {
                if (t >= 40 || player.isDead()) {
                    impact(player, pos, radius, fLevel);
                    cancel();
                    return;
                }
                Vector step = dir.clone().multiply(0.85);
                step.setY(step.getY() - t * 0.018); // gentle gravity = arc
                pos.add(step);

                // hourglass-glyph trail on the bell — steady bone dust, single soul mote.
                pos.getWorld().spawnParticle(Particle.DUST, pos, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(TheInevitable.BONE[0], 1.1f));
                if (t % 3 == 0) pos.getWorld().spawnParticle(Particle.SOUL, pos, 1, 0.02, 0.02, 0.02, 0.0);

                // stop early if we hit the ground or a non-ally entity.
                if (!pos.getBlock().isPassable()) {
                    impact(player, pos, radius, fLevel);
                    cancel();
                    return;
                }
                for (Entity e : pos.getWorld().getNearbyEntities(pos, 1.0, 1.0, 1.0)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    impact(player, pos, radius, fLevel);
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static void impact(Player player, Location at, double radius, int level) {
        at.getWorld().playSound(at, Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        at.getWorld().playSound(at, Sound.BLOCK_BELL_USE, 0.7f, 0.5f);

        // a single low bell-toll ring sweeps out once (no strobe).
        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > radius) {
                    cancel();
                    return;
                }
                for (Vector v : ParticleUtils.getCirclePoints(r, Math.max(12, (int) (r * 10)))) {
                    at.getWorld().spawnParticle(Particle.DUST, at.clone().add(v).add(0, 0.15, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TheInevitable.BONE[1], 1.0f));
                }
                at.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 0.3, 0), 2, r * 0.3, 0.1, r * 0.3, 0.005);
                r += 0.6;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // hourglass glyph above the impact
        for (Vector v : ParticleUtils.getVerticalCirclePoints(0.6, 0, 0, 8)) {
            at.getWorld().spawnParticle(Particle.DUST, at.clone().add(0, 1.2, 0).add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(TheInevitable.BONE[0], 0.9f));
        }

        for (Entity e : at.getWorld().getNearbyEntities(at, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (le.hasMetadata("NPC")) continue;

            boolean fresh = !TheInevitable.hasTimer(le);
            TheInevitable.applyTimer(player, le, level);

            // L3: also slow the marked enemies briefly.
            if (level >= 3) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            }
            // L5: mark refreshes whenever the target later takes damage.
            if (level >= 5) {
                le.addScoreboardTag("thanatosKnellRefresh");
            }
            le.getWorld().spawnParticle(Particle.SOUL, le.getLocation().clone().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.01);
            if (fresh) le.getWorld().playSound(le.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.5f, 0.8f);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Hurl a bell; enemies near the impact gain Death Timers.";
            case 2 -> ChatColor.GRAY + "Larger impact radius.";
            case 3 -> ChatColor.GRAY + "Marked enemies are also briefly slowed.";
            case 4 -> ChatColor.GRAY + "Sets a higher execution threshold — easier reaps.";
            case 5 -> ChatColor.GRAY + "Marks refresh whenever the target takes damage.";
            default -> ChatColor.GRAY + "Hurl a bell to mark enemies at range.";
        };
    }

    @Override
    public String toString() {
        return "knell";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BELL),
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Knell]",
                ChatColor.GRAY + "Hurl a tolling bell to a spot. Every enemy caught",
                ChatColor.GRAY + "in the impact gains a " + ChatColor.DARK_GREEN + "Death Timer" + ChatColor.GRAY + " — your",
                ChatColor.GRAY + "reach, since the clock comes to them.");
    }
}
