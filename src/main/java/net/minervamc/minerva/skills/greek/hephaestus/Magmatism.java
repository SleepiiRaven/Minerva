package net.minervamc.minerva.skills.greek.hephaestus;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
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

        List<ArmorStand> magmaChunks = new ArrayList<>();
        int numberOfChunks = 16;

        for (int i = 0; i < numberOfChunks; i++) {
            double angle = i * (2 * Math.PI / numberOfChunks);
            Location spawnLoc = origin.clone().add(Math.cos(angle), 0, Math.sin(angle));
            ArmorStand displayEntity = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

            displayEntity.setInvisible(true);
            displayEntity.setInvulnerable(true);

            displayEntity.getEquipment().setHelmet(new ItemStack(Material.MAGMA_BLOCK));
            displayEntity.addScoreboardTag("magmaChunk");
            magmaChunks.add(displayEntity);
        }

        // Schedule the movement and interaction logic
        new BukkitRunnable() {
            int ticks = 0;
            List<ArmorStand> toRemove = new ArrayList<>();

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

                for (ArmorStand a : toRemove) {
                    World wrld = a.getWorld();
                    Location locA = a.getLocation().add(0, 2, 0);

                    wrld.spawnParticle(Particle.LARGE_SMOKE, locA, 5, 0, 0, 0, 0.2);
                    wrld.spawnParticle(Particle.LAVA, locA, 5, 0, 0, 0, 0.3);
                    wrld.spawnParticle(Particle.EXPLOSION, locA, 1, 0, 0, 0, 0.1);
                    wrld.spawnParticle(Particle.FIREWORK, locA, 5, 0, 0, 0, 0.2);
                    wrld.spawnParticle(Particle.FLAME, locA, 10, 0, 0, 0, 0.2);
                    magmaChunks.remove(a);
                    a.remove();
                }

                toRemove.clear();

                for (ArmorStand a : magmaChunks) {
                    if (a.isOnGround() && ticks >= 5) {
                        toRemove.add(a);
                    }
                }

                if (ticks++ <= 5) {
                    return;
                } else if (ticks == 6) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 2.0f, 0.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.7f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1f, 0.9f);
                }

                if (ticks++ > duration) {
                    magmaChunks.forEach((a) -> {
                        World wrld = a.getWorld();
                        Location locA = a.getLocation().add(0, 2, 0);
                        wrld.spawnParticle(Particle.LARGE_SMOKE, locA, 5, 0, 0, 0, 0.2);
                        wrld.spawnParticle(Particle.LAVA, locA, 5, 0, 0, 0, 0.3);
                        wrld.spawnParticle(Particle.EXPLOSION, locA, 1, 0, 0, 0, 0.1);
                        wrld.spawnParticle(Particle.FIREWORK, locA, 5, 0, 0, 0, 0.2);
                        wrld.spawnParticle(Particle.FLAME, locA, 10, 0, 0, 0, 0.2);
                        a.remove();
                    });
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
                            damage(target, damage, player);  // Deal 4 damage
                            Vector knockback = target.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(0.5);
                            target.setVelocity(knockback);

                            if (target instanceof IronGolem && target.getScoreboardTags().contains("livingForge") && PlayerStats.isSummoned(player, target)) {
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
        return ItemCreator.get(Material.MAGMA_BLOCK)
                .setName(TextContext.formatLegacy("&lMagmatism", false).color(NamedTextColor.RED))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Tear magma from beneath", false),
                        TextContext.formatLegacy("&7the ground around you,", false),
                        TextContext.formatLegacy("&7then shoot it outwards in", false),
                        TextContext.formatLegacy("&7a circle. Upon hitting", false),
                        TextContext.formatLegacy("&7enemies, the magma will", false),
                        TextContext.formatLegacy("&7push them back, damage them,", false),
                        TextContext.formatLegacy("&7and light them ablaze.", false)
                )).build();
    }
}
