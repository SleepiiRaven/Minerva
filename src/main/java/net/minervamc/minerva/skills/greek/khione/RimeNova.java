package net.minervamc.minerva.skills.greek.khione;

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
 * Khione RLR — point-blank ring that applies a chunk of Frost to everyone around, setting up
 * mass Encases. Brittle targets caught take a little bonus damage instead.
 */
public class RimeNova extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "rimeNova")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "rimeNova", cooldown);
        cooldownAlarm(player, cooldown, "Rime Nova");

        double radius = switch (level) {
            case 2 -> 4.0;
            case 3, 4 -> 4.5;
            case 5 -> 5.0;
            default -> 3.5;
        };
        double frost = switch (level) {
            case 2, 3 -> 35;
            case 4, 5 -> 40;
            default -> 30;
        };
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1f);

        applyRing(player, radius, frost, fLevel);
        if (fLevel >= 5) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyRing(player, radius + 1, frost * 0.6, fLevel);
                }
            }.runTaskLater(Minerva.getInstance(), 10L);
        }

        // expanding ring telegraph (single sweep, no strobe)
        new BukkitRunnable() {
            double r = 0.4;
            @Override
            public void run() {
                if (r > radius) {
                    cancel();
                    return;
                }
                Location c = player.getLocation();
                for (Vector v : ParticleUtils.getCirclePoints(r, Math.max(10, (int) (r * 12)))) {
                    c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(Frostbite.FROST[(int) (Math.random() * Frostbite.FROST.length)], 1.1f));
                }
                r += 0.5;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static void applyRing(Player player, double radius, double frost, int level) {
        for (Entity e : player.getNearbyEntities(radius, 3, radius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (level >= 3 && Frostbite.isBrittle(le)) {
                damage(le, 4, player);
            }
            Frostbite.addFrost(player, le, frost, level);
            le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().clone().add(0, 1, 0), 8, 0.2, 0.5, 0.2, 0.05);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Burst Frost onto all nearby enemies.";
            case 2 -> ChatColor.GRAY + "Larger radius and more Frost.";
            case 3 -> ChatColor.GRAY + "Brittle enemies caught take bonus damage.";
            case 4 -> ChatColor.GRAY + "Even larger radius.";
            case 5 -> ChatColor.GRAY + "A second wider pulse follows.";
            default -> ChatColor.GRAY + "Burst Frost onto nearby enemies.";
        };
    }

    @Override
    public String toString() {
        return "rimeNova";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PACKED_ICE),
                ChatColor.AQUA + "" + ChatColor.BOLD + "[Rime Nova]",
                ChatColor.GRAY + "Erupt a ring of cold, slamming " + ChatColor.AQUA + "Frost",
                ChatColor.GRAY + "onto every enemy around you — your mass setup.");
    }
}
