package net.minervamc.minerva.skills.greek.hephaestus;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Magmatism extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        Location origin = player.getLocation();
        World world = player.getWorld();
        int duration = 100; // duration in ticks
        double deceleration = 0.95;
        double damage = 2;
        int fireTicks = 40;
        long cooldown = 13000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "magmatism")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "magmatism", cooldown);
        cooldownAlarm(player, cooldown, "Magmatism");

        // Initialize display entities in a circular pattern
        List<ArmorStand> magmaChunks = new ArrayList<>();
        int numberOfChunks = 16;  // More chunks for a more complete circle

        for (int i = 0; i < numberOfChunks; i++) {
            double angle = i * (2 * Math.PI / numberOfChunks);
            Location spawnLoc = origin.clone().add(Math.cos(angle), 0, Math.sin(angle));
            ArmorStand displayEntity = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

            // Armor stand customization for magma visual
            displayEntity.setInvisible(true);
            displayEntity.setInvulnerable(true);

            // Use magma block as display item
            displayEntity.getEquipment().setHelmet(new ItemStack(Material.MAGMA_BLOCK));
            displayEntity.addScoreboardTag("magmaChunk");
            magmaChunks.add(displayEntity);
        }

        // Schedule the movement and interaction logic
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_WOLF_ARMOR_BREAK, 1.2f, 0.4f);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1f, 1f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_COMPOSTER_FILL, 1f, 0.9f);
                    magmaChunks.forEach(a -> {
                        a.setVelocity(new Vector(0, 0.25, 0));
                    });
                    ticks++;
                    return;
                }

                if (ticks++ <= 5) {
                    return;
                } else if (ticks == 6) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 2.0f, 0.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.7f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1f, 0.9f);
                }

                if (ticks++ > duration) {
                    magmaChunks.forEach(Entity::remove);
                    cancel();
                    return;
                }

                for (ArmorStand magmaChunk : magmaChunks) {
                    if (!magmaChunk.isValid()) continue;

                    if (ticks % 5 == 0)
                        magmaChunk.getWorld().spawnParticle(Particle.LAVA, magmaChunk.getLocation(), 0, 0, 0, 0, 0);

                    // Slow down movement gradually
                    Vector velocity = magmaChunk.getVelocity();
                    velocity.multiply(deceleration);  // Deceleration
                    magmaChunk.setVelocity(velocity);

                    // Check for nearby entities to apply effects
                    magmaChunk.getNearbyEntities(1, 1, 1).forEach(entity -> {
                        if (entity.getScoreboardTags().contains("magmaChunk")) {
                            Vector knockback = entity.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(0.5);
                            entity.setVelocity(knockback);
                        } else if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            target.setFireTicks(fireTicks);  // Set target on fire
                            target.damage(damage, player);  // Deal 4 damage
                            Vector knockback = target.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(0.5);
                            target.setVelocity(knockback);

                            if (target instanceof IronGolem && target.getScoreboardTags().contains(player.getUniqueId().toString())) {
                                LivingForge.overheat(target);
                            }
                        }
                    });
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0, 1);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "magmatism";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.create(Material.MAGMA_BLOCK);
    }
}
