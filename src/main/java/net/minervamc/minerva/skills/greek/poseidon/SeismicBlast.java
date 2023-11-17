package net.minervamc.minerva.skills.greek.poseidon;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class SeismicBlast extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 5000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "seismicBlast")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "seismicBlast", cooldown);
        cooldownAlarm(player, cooldown, "Seismic Blast");

        double radius = 5;
        long delay = 15;
        Vector knockUp = new Vector(0, 1.5, 0);
        Vector knockDown = new Vector(0, -1.5, 0);

        player.setVelocity(new Vector(0, 0.25, 0));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.5f);

        for (Vector particleVector : ParticleUtils.getFilledCirclePoints(radius, radius * 10)) {
            Location particleLocation = player.getLocation().clone().add(particleVector);
            Block block = player.getLocation().subtract(0, 1, 0).getBlock();
            BlockData blockData;
            if (!block.getType().isSolid()) blockData = Bukkit.createBlockData(Material.DIRT);
            blockData = block.getBlockData();
            player.getWorld().spawnParticle(Particle.BLOCK_DUST, particleLocation, 3, blockData);
        }

        List<LivingEntity> caughtLivingEntities = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(radius, 2, radius)) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (livingEntity == player) continue;
            livingEntity.damage(1, player);
            livingEntity.setVelocity(knockUp);
            caughtLivingEntities.add(livingEntity);
        }

        if (caughtLivingEntities.size() < 1) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean playedSound = false;
                for (LivingEntity livingEntity : caughtLivingEntities) {
                    if (!livingEntity.isOnGround()) {
                        if (playedSound == false) {
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.7f);
                            playedSound = true;
                        }
                        livingEntity.setVelocity(knockDown);
                    }
                }
            }
        }.runTaskLater(Minerva.getInstance(), delay);
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
        return "seismicBlast";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.DIRT), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Earthquake]", ChatColor.GRAY + "Shake the earth below you, throwing enemies in a small radius up, then back down,", ChatColor.GRAY + "causing massive fall damage to enemies that can take fall damage.");
    }
}
