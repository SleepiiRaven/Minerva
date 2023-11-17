package net.minervamc.minerva.skills.greek.zeus;

import java.util.Random;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class StormsEmbrace extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int maxEntitiesDamaged = 5;
        double radius = Math.sqrt(3); // Change the middle.
        double damage = 6;
        long triggers = 20;
        long timeBetweenTriggers = 5;
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "stormsEmbrace")) {
            onCooldown(player);
        } else {
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "stormsEmbrace", cooldown);
            cooldownAlarm(player, cooldown, "Storm's Embrace");
            player.getLocation().getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
            player.getLocation().getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1f);
            new BukkitRunnable() {
                int triggersCompleted = 0;

                @Override
                public void run() {
                    Location effectLocation = player.getEyeLocation().add(new Vector(0, 3, 0));
                    if (triggersCompleted > triggers) {
                        this.cancel();
                        return;
                    }

                    Location finalParticle = null;

                    for (Vector relativeParticleLocation : ParticleUtils.getFilledCirclePoints(radius, 50)) {
                        Location particleLocation = effectLocation.add(relativeParticleLocation);
                        particleLocation.getWorld().spawnParticle(Particle.CLOUD, particleLocation, 0, 0, 0, 0, 0);
                        particleLocation.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GRAY, 2));
                        particleLocation.getWorld().spawnParticle(Particle.WATER_DROP, particleLocation, 0);
                        finalParticle = particleLocation;
                    }

                    if (finalParticle != null) {
                        branch(player, finalParticle, new Vector(0, -1, 0), 5, 3, damage);
                        finalParticle.getWorld().playSound(finalParticle, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1f);
                    }

                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 1f, 1f);

                    if (triggersCompleted % 5 == 0) {
                        triggersCompleted++;

                        int entitiesHarmed = 0;

                        for (Entity entity : player.getLocation().clone().getNearbyEntities(5, 5, 5)) {
                            if (entitiesHarmed > maxEntitiesDamaged) return;

                            if (entity instanceof LivingEntity livingEntity && livingEntity != player) {
                                livingEntity.damage(damage, player);
                                livingEntity.setVelocity(ParticleUtils.getDirection(player.getLocation(), livingEntity.getLocation()).multiply(0.1));
                                stun(livingEntity, 4);
                                entitiesHarmed++;
                            }
                        }
                    } else {
                        triggersCompleted++;
                    }
                }
            }.runTaskTimer(Minerva.getInstance(), 0L, timeBetweenTriggers);
        }

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

    private void branch(Player player, Location location, Vector direction, int maxDistance, int maxBranches, double damage) {
        int rotationMax = 20;
        Random random = new Random();

        for (double i = 0; i <= maxDistance; i += 0.2) {
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(direction.clone().multiply(i)), 0);
            player.getWorld().spawnParticle(Particle.REDSTONE, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.AQUA, 2));
            player.getWorld().spawnParticle(Particle.REDSTONE, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 2));
            player.getWorld().spawnParticle(Particle.REDSTONE, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GRAY, 2));

            if (random.nextInt(0, 10) == 2 && maxBranches > 0) {
                double xRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                double yRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                double zRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                branch(player, location.clone().add(direction.clone().multiply(i)), ParticleUtils.rotateYAxis(ParticleUtils.rotateZAxis(ParticleUtils.rotateXAxis(direction, xRotation), zRotation), yRotation), (int) (maxDistance - i), maxBranches - 1, damage);
                xRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                yRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                zRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                branch(player, location.clone().add(direction.clone().multiply(i)), ParticleUtils.rotateYAxis(ParticleUtils.rotateZAxis(ParticleUtils.rotateXAxis(direction, xRotation), zRotation), yRotation), (int) (maxDistance - i), maxBranches - 1, damage);
                return;
            }
        }
    }

    @Override
    public String toString() {
        return "stormsEmbrace";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.QUARTZ), ChatColor.GRAY + "" + ChatColor.BOLD + "[Storm's Embrace]", ChatColor.GRAY + "Summon a rain cloud that pours down a storm of lightning and rain upon your enemies in a large radius,", ChatColor.GRAY + "damaging them and stunning them.");
    }
}
