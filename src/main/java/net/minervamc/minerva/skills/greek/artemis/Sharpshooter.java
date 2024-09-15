package net.minervamc.minerva.skills.greek.artemis;

import net.kyori.adventure.sound.SoundStop;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Sharpshooter extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int effectDuration = 150;
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "sharpshooter")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "sharpshooter", cooldown);
        cooldownAlarm(player, cooldown, "Sharpshooter");

        player.addScoreboardTag("homingArtemis");
        new BukkitRunnable() {
            int ticks = 0;
            double yOffset = 0;
            double radius = 3;

            @Override
            public void run() {
                Location location = player.getLocation();
                if (player.isDead() || !player.isOnline() || ticks++ >= effectDuration || location.getWorld() == null) {
                    player.removeScoreboardTag("homingArtemis");
                    this.cancel();
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 2f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 1.3f);
                    player.getWorld().stopSound(SoundStop.named(Sound.BLOCK_CONDUIT_AMBIENT));
                    player.getWorld().stopSound(SoundStop.named(Sound.BLOCK_AMETHYST_BLOCK_CHIME));
                    return;
                }

                if (radius <= 0) {
                    radius = 3;
                    yOffset = 0;
                }

                Location particleLoc = location.clone().add(0, yOffset, 0);
                particleLoc.setX(location.getX() + Math.cos(ticks) * radius);
                particleLoc.setZ(location.getZ() + Math.sin(ticks) * radius);
                location.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.fromRGB(153, 153, 255), 2));

                particleLoc.setX(location.getX() + Math.sin(ticks) * radius);
                particleLoc.setZ(location.getZ() + Math.cos(ticks) * radius);
                location.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.fromRGB(153, 153, 255), 2));


                radius -= 0.075;
                yOffset += 0.2;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2f, 2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 2f, 1.9f);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "sharpshooter";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BOW), ChatColor.BOLD + "" + ChatColor.AQUA + "[Sharpshooter]", ChatColor.GRAY + "For a short time, your arrows fire in an arc that, with your incredible skills", ChatColor.GRAY + "in archery and sharpshooting, almost never miss their targets.");
    }
}
