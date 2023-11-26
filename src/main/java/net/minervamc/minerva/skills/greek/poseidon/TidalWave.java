package net.minervamc.minerva.skills.greek.poseidon;

import net.minervamc.minerva.Minerva;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TidalWave extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int tickDuration;
        long cooldown;
        double damage;

        switch (level) {
            default -> {
                tickDuration = 20;
                cooldown = 10000 + (tickDuration*50);
                damage = 0.5;
            }
            case 2 -> {
                tickDuration = 30;
                cooldown = 9000 + (tickDuration*50);
                damage = 1;
            }
            case 3 -> {
                tickDuration = 40;
                cooldown = 8000 + (tickDuration*50);
                damage = 2;
            }
            case 4 -> {
                tickDuration = 50;
                cooldown = 7000 + (tickDuration*50);
                damage = 2.5;
            }
            case 5 -> {
                tickDuration = 60;
                cooldown = 8000 + (tickDuration*50);
                damage = 3;
            }
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tidalWave")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tidalWave", cooldown);
        cooldownAlarm(player, cooldown, "Tidal Wave");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);

        final Vector relativePlayerVector = new Vector(0, 1.5, 0);
        Horse horse = (Horse) player.getWorld().spawnEntity(player.getLocation().add(0, 100, 0), EntityType.HORSE);
        horse.setInvisible(true);
        horse.setInvulnerable(true);
        horse.setSilent(true);
        horse.setAI(false);
        horse.teleport(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                horse.setPassenger(player);
            }
        }.runTaskLater(Minerva.getInstance(), 5L);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (horse.getPassengers().size() == 0 || ticks >= (tickDuration/5)) {
                    horse.remove();
                    this.cancel();
                }
                Location location = horse.getLocation();
                Vector direction = player.getEyeLocation().getDirection().clone().setY(0);

                Vector lineDirection = direction.clone().rotateAroundY(90);

                waveParticleEffect(direction, lineDirection, relativePlayerVector, location, player);

                if (!horse.getLocation().clone().subtract(0, 1, 0).getBlock().isSolid() && !horse.getLocation().clone().subtract(0, 1, 0).getBlock().isLiquid()) {
                    horse.setVelocity(direction.clone().multiply(1).subtract(new Vector(0, 0.5, 0)));
                } else {
                    horse.setVelocity(direction.multiply(1));
                }

                for (Entity entity : horse.getLocation().getNearbyEntities(2, 2, 2)) {
                    if (entity instanceof LivingEntity livingEntity && livingEntity != horse && livingEntity != player) {
                        livingEntity.damage(damage, player);
                        livingEntity.setVelocity(direction.clone());
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 5L, 5L);
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

    private void waveParticleEffect(Vector direction, Vector lineDirection, Vector relativePlayerVector, Location location, Player player) {
        for (double i = -1; i <= 1; i += 0.2) {
            Vector localDirection = direction.clone();
            Vector progressVector = lineDirection.clone().multiply(i);
            Vector A = relativePlayerVector.clone().subtract(localDirection.clone().add(new Vector(0, 1.5, 0)));
            Vector B = relativePlayerVector.clone().add(new Vector(0, 0.5, 0));
            Vector C = relativePlayerVector.clone().add(localDirection.clone().multiply(0.8).subtract(new Vector(0, 0.5, 0)));
            for (Vector particleVector : ParticleUtils.getQuadraticBezierPoints(A, B, C, 10)) {
                Location particleLocation = location.clone().add(particleVector).clone().add(progressVector);
                player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0,157,196), 1));
                player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, new Particle.DustOptions(Color.BLUE, 1));
            }
            A = C.clone();
            B = relativePlayerVector.clone().subtract(new Vector(0, 0.5, 0));
            C = A.clone().subtract(new Vector(0, 1, 0));
            for (Vector particleVector : ParticleUtils.getQuadraticBezierPoints(A, B, C, 10)) {
                Location particleLocation = location.clone().add(particleVector).clone().add(progressVector);
                player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0,157,196), 1));
                player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, new Particle.DustOptions(Color.BLUE, 1));
            }
        }
    }

    @Override
    public String toString() {
        return "tidalWave";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PRISMARINE_SHARD), ChatColor.AQUA + "" + ChatColor.BOLD + "[Tidal Wave]", ChatColor.GRAY + "Ride forward on a speedy wave of water that does a medium amount of damage to enemies in its way.");
    }
}
