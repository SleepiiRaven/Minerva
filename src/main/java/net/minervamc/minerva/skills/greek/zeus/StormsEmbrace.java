package net.minervamc.minerva.skills.greek.zeus;

import java.util.Random;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
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
        int maxEntitiesDamaged;
        double radius; // Change the middle.
        double damage;
        long triggers;
        long timeBetweenTriggers;
        long cooldown;

        switch (level) {
            case 2 -> {
                maxEntitiesDamaged = 7;
                radius = Math.sqrt(3); // Change the middle.
                damage = 5;
                triggers = 30;
                timeBetweenTriggers = 5;
                cooldown = 9500;
            }
            case 3 -> {
                maxEntitiesDamaged = 10;
                radius = Math.sqrt(5); // Change the middle.
                damage = 12;
                triggers = 30;
                timeBetweenTriggers = 5;
                cooldown = 9000;
            }
            case 4 -> {
                maxEntitiesDamaged = 15;
                radius = Math.sqrt(9); // Change the middle.
                damage = 15;
                triggers = 30;
                timeBetweenTriggers = 5;
                cooldown = 8500;
            }
            case 5 -> {
                maxEntitiesDamaged = 20;
                radius = Math.sqrt(16); // Change the middle.
                damage = 20;
                triggers = 30;
                timeBetweenTriggers = 5;
                cooldown = 8000;
            }
            default -> {
                maxEntitiesDamaged = 5;
                radius = Math.sqrt(3); // Change the middle.
                damage = 3;
                triggers = 20;
                timeBetweenTriggers = 5;
                cooldown = 20000;
            }
        }

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
                    if (player.isDead() || !player.isOnline() || triggersCompleted > triggers) {
                        this.cancel();
                        return;
                    }

                    Location finalParticle = null;

                    for (Vector relativeParticleLocation : ParticleUtils.getFilledCirclePoints(radius, 50)) {
                        Location particleLocation = effectLocation.add(relativeParticleLocation);
                        particleLocation.getWorld().spawnParticle(Particle.CLOUD, particleLocation, 0, 0, 0, 0, 0);
                        particleLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GRAY, 2));
                        particleLocation.getWorld().spawnParticle(Particle.FALLING_WATER, particleLocation, 0);
                        finalParticle = particleLocation;
                    }

                    if (finalParticle != null) {
                        branch(player, finalParticle, new Vector(0, -1, 0), 5, 3, damage);
                        finalParticle.getWorld().playSound(finalParticle, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.1f, 1f);
                    }

                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.2f, 1f);

                    if (triggersCompleted % 5 == 0) {
                        triggersCompleted++;

                        int entitiesHarmed = 0;

                        for (Entity entity : player.getLocation().clone().getNearbyEntities(5, 5, 5)) {
                            if (entitiesHarmed > maxEntitiesDamaged) return;

                            if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                                damage(livingEntity, damage, player);
                                knockback(livingEntity, ParticleUtils.getDirection(player.getLocation(), livingEntity.getLocation()).multiply(0.1));
                                stun(player, livingEntity, 4);
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
            player.getWorld().spawnParticle(Particle.DUST, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.AQUA, 2));
            player.getWorld().spawnParticle(Particle.DUST, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 2));
            player.getWorld().spawnParticle(Particle.DUST, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GRAY, 2));

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
        return ItemUtils.getItem(new ItemStack(Material.QUARTZ), ChatColor.GRAY + "" + ChatColor.BOLD + "[Storm's Embrace]", ChatColor.GRAY + "Summon a rain cloud that pours", ChatColor.GRAY + "down a storm of lightning and", ChatColor.GRAY + "rain upon your enemies in a", ChatColor.GRAY + "large radius, damaging them", ChatColor.GRAY + "and stunning them.");
    }
}
