package net.minervamc.minerva.skills.greek.zeus;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Soar extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        int duration;
        switch (level) {
            default -> {
                cooldown = 12000;
                duration = 20;
            }
            case 2 -> {
                cooldown = 11000;
                duration = 25;
            }
            case 3 -> {
                cooldown = 10000;
                duration = 30;
            }
            case 4 -> {
                cooldown = 9500;
                duration = 35;
            }
            case 5 -> {
                cooldown = 8000;
                duration = 40;
            }
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "soar")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "soar", cooldown);
        cooldownAlarm(player, cooldown, "Soar");

        player.setVelocity(new Vector(0, 1, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= duration) {
                    if (!(player.getGameMode() == GameMode.CREATIVE)) {
                        player.setAllowFlight(false);
                    }
                    player.setFlying(false);
                    this.cancel();
                }

                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 0, 0, 0, 0, 0);

                ticks++;
            }

        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> "?";
            case 2 -> "??";
            case 3 -> "???";
            case 4 -> "????";
            case 5 -> "?????";
            default -> "-";
        };
    }

    @Override
    public String toString() {
        return "soar";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.FEATHER), ChatColor.WHITE + "" + ChatColor.BOLD + "[Soar]", ChatColor.GRAY + "Use the winds to lift your weight, allowing you to fly for a short period of time.");
    }
}
