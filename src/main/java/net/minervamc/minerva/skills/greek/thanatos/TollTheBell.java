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
 * Thanatos RRL — the accelerator. A resonant gong HALVES the remaining Death Timers of every
 * nearby marked enemy, rushing the whole board toward the reap at once. Each hourglass visibly
 * drops its sand by half. No marks are applied here — the passive + Knell handle that.
 */
public class TollTheBell extends Skill {
    private static final String[] THANATOS_KEYS = {"shroudOfLetus", "knell", "scytheOfLetus"};

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 13500;
            case 4, 5 -> 13000;
            default -> 14000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tollTheBell")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tollTheBell", cooldown);
        cooldownAlarm(player, cooldown, "Toll the Bell");

        double radius = switch (level) {
            case 2, 3 -> 12;
            case 4, 5 -> 13;
            default -> 10;
        };
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.8f, 0.6f);

        // halve every nearby marked timer (the core accelerator).
        TheInevitable.halveTimers(player, radius);

        // L4: do a second halving pass for timers already under half — the inevitable, doubly so.
        if (fLevel >= 4) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    TheInevitable.halveTimers(player, radius);
                }
            }.runTaskLater(Minerva.getInstance(), 4L);
        }

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (!TheInevitable.hasTimer(le)) continue;

            // L3: briefly reduce incoming healing (mining fatigue is harmless flavor; the real
            // anti-heal is a steady WITHER tick that offsets regen while it's up).
            if (fLevel >= 3) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
            }
            // L5: each marked enemy tolled refunds a sliver of your cooldowns.
            if (fLevel >= 5) {
                refundSliver(player, cooldownManager);
            }
        }

        // one resonant ground ring (single sweep, no strobe).
        new BukkitRunnable() {
            double r = 0.6;
            @Override
            public void run() {
                if (r > radius * 0.6) {
                    cancel();
                    return;
                }
                Location c = player.getLocation();
                for (Vector v : ParticleUtils.getCirclePoints(r, Math.max(14, (int) (r * 8)))) {
                    c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v).add(0, 0.15, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TheInevitable.BONE[1], 1.0f));
                }
                c.getWorld().spawnParticle(Particle.SOUL, c.clone().add(0, 0.3, 0), 2, r * 0.4, 0.05, r * 0.4, 0.005);
                r += 0.8;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static void refundSliver(Player player, CooldownManager cd) {
        long sliver = 700;
        for (String key : THANATOS_KEYS) {
            long rem = cd.getCooldownLeft(player.getUniqueId(), key);
            if (rem > 0) {
                cd.setCooldownFromNow(player.getUniqueId(), key, Math.max(0L, rem - sliver));
            }
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Halve the remaining Death Timers of nearby marked enemies.";
            case 2 -> ChatColor.GRAY + "Larger radius.";
            case 3 -> ChatColor.GRAY + "Tolled enemies briefly take wither, offsetting their healing.";
            case 4 -> ChatColor.GRAY + "Timers already under half are halved again.";
            case 5 -> ChatColor.GRAY + "Each enemy tolled refunds a sliver of your cooldowns.";
            default -> ChatColor.GRAY + "Halve the timers of nearby marked enemies.";
        };
    }

    @Override
    public String toString() {
        return "tollTheBell";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BELL),
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Toll the Bell]",
                ChatColor.GRAY + "Ring a resonant gong that " + ChatColor.DARK_GREEN + "halves" + ChatColor.GRAY + " the remaining",
                ChatColor.GRAY + "Death Timers of every nearby marked enemy — rushing",
                ChatColor.GRAY + "the whole board toward the reap at once.");
    }
}
