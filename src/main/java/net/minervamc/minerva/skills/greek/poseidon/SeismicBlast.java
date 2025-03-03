package net.minervamc.minerva.skills.greek.poseidon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SeismicBlast extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        double radius;
        long delay;
        double knock;
        double damage;

        switch (level) {
            case 2 -> {
                cooldown = 9000;
                radius = 4;
                delay = 15;
                knock = 1.2;
                damage = 1;
            }
            case 3 -> {
                cooldown = 8500;
                radius = 5;
                delay = 15;
                knock = 1.5;
                damage = 1.5;
            }
            case 4 -> {
                cooldown = 8000;
                radius = 7.5;
                delay = 20;
                knock = 2;
                damage = 2;
            }
            case 5 -> {
                cooldown = 7500;
                radius = 10;
                delay = 20;
                knock = 2;
                damage = 3;
            }
            default -> {
                cooldown = 9000;
                radius = 4;
                delay = 10;
                knock = 1;
                damage = 6;
            }
        }


        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "seismicBlast")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "seismicBlast", cooldown);
        cooldownAlarm(player, cooldown, "Seismic Blast");

        Color[] colors = {
                Color.fromRGB(103, 68, 34),
                Color.fromRGB(131, 100, 62),
                Color.fromRGB(194, 155, 108),
                Color.fromRGB(148, 115, 82),
                Color.fromRGB(112, 79, 56),
                Color.fromRGB(82, 57, 47)
        };

        knockback(player, new Vector(0, 0.25, 0));
        Vector knockUp = new Vector(0, knock, 0);
        Vector knockDown = new Vector(0, -knock, 0);

        Random random = new Random();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.5f);

        for (Vector particleVector : ParticleUtils.getFilledCirclePoints(radius, radius * 10)) {
            Location particleLocation = player.getLocation().clone().add(particleVector);
            Block block = player.getLocation().subtract(0, 1, 0).getBlock();
            BlockData blockData;
            if (!block.getType().isSolid()) blockData = Bukkit.createBlockData(Material.DIRT);
            else blockData = block.getBlockData();
            player.getWorld().spawnParticle(Particle.BLOCK, particleLocation, 3, blockData);
            player.getWorld().spawnParticle(Particle.DUST, particleLocation, 0, 0, 0, 0, 0, new Particle.DustOptions(colors[random.nextInt(0, 5)], 2));
        }

        List<LivingEntity> caughtLivingEntities = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(radius, 2, radius)) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (livingEntity == player) continue;
            if (!(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                damage(livingEntity, damage, player);
                knockback(livingEntity, knockUp);
                caughtLivingEntities.add(livingEntity);
            }
        }

        if (caughtLivingEntities.size() < 1) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean playedSound = false;
                for (LivingEntity livingEntity : caughtLivingEntities) {
                    if (!livingEntity.isOnGround()) {
                        if (!playedSound) {
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.7f);
                            playedSound = true;
                        }
                        knockback(livingEntity, knockDown);
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
        return ItemUtils.getItem(new ItemStack(Material.DIRT), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Earthquake]", ChatColor.GRAY + "Shake the earth below you, throwing", ChatColor.GRAY + "enemies in a small radius up, then back down,", ChatColor.GRAY + "causing massive fall damage to enemies", ChatColor.GRAY + "that can take fall damage.");
    }
}
