package net.minervamc.minerva.listeners;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.greek.apollo.PlagueVolley;
import net.minervamc.minerva.skills.greek.poseidon.AquaticLimbExtensions;
import net.minervamc.minerva.types.HeritageType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class SkillListener implements Listener {
    @EventHandler
    public void onPlayerKill(EntityDamageByEntityEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.getEntity() instanceof LivingEntity entity && event.getDamager() instanceof Player player && entity.getHealth() == 0) {
                    if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.LIFE_STEAL && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
                        // Hades'/Pluto's Life-steal ability
                        if (player.getHealth() > (player.getMaxHealth() - 1)) {
                            player.setHealth(player.getMaxHealth());
                        }
                        else {
                            double healing = 1;
                            player.setHealth(player.getHealth() + healing);
                        }
                    }
                }
            }
        }.runTaskLater(Minerva.getInstance(), 1L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlock().getType() == Material.WATER || (player.getLocation().getBlock().getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged())) {
            if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.OCEANS_EMBRACE && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0));
            }
        }
    }

    @EventHandler
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.PROTECTIVE_CLOUD && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onWaterFlow(BlockFromToEvent event) {
        for (List<Block> blocks : AquaticLimbExtensions.waterBlocks.values()) {
            if (blocks.contains(event.getBlock())) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPunch(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            if (AquaticLimbExtensions.waterBlocks.containsKey(player.getUniqueId())) {
                Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "aquaticPunching", AquaticLimbExtensions.punchDurationMillis);
            } else if (!Minerva.getInstance().getCdInstance().isCooldownDone(player.getUniqueId(), "burningLightNotPunching")) {
                Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "burningLightPunching", 250L);
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.ARROWS_OF_THE_SUN && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
            Random random = new Random();
            int randomInt = random.nextInt(0, 5);
            if (randomInt == 1) {
                Vector velocity = event.getProjectile().getVelocity();
                SpectralArrow arrow = (SpectralArrow) event.getProjectile().getWorld().spawnEntity(event.getProjectile().getLocation().setDirection(event.getProjectile().getLocation().getDirection()), EntityType.SPECTRAL_ARROW);
                event.getProjectile().remove();
                arrow.setVelocity(velocity);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (arrow.isDead()) {
                            World world = arrow.getWorld();
                            Location loc = arrow.getLocation();
                            world.spawnParticle(Particle.REDSTONE, loc, 10, 0, 0, 0, 0.5, new Particle.DustOptions(Color.fromRGB(250, 250, 210), 2));
                            world.spawnParticle(Particle.END_ROD, loc, 50, 0, 0, 0, 0.3);
                            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.5f, 1.2f);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 2f);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.2f, 1f);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2f, 2f);
                            for (Entity nearbyEntity : loc.getNearbyEntities(2, 2, 2)) {
                                if (!(nearbyEntity instanceof LivingEntity target) || nearbyEntity.equals(player)) continue;
                                target.damage(5, player);
                            }
                            this.cancel();
                            return;
                        }
                        if (arrow.isOnGround()) {
                            this.cancel();
                        }
                        if (player.getScoreboardTags().contains("homingApollo")) {
                            List<Entity> nearest = arrow.getNearbyEntities(20, 20, 20);
                            Entity target = null;
                            for (Entity near : nearest) {
                                if (near != player && near instanceof LivingEntity && !near.isInvulnerable() && !near.isDead() && player.hasLineOfSight(near)) {
                                    if (target == null) {
                                        target = near;
                                    } else if (arrow.getLocation().distanceSquared(near.getLocation()) < arrow.getLocation().distanceSquared(target.getLocation())) {
                                        target = near;
                                    }
                                }
                            }
                            if (target == null) return;
                            arrow.setVelocity(target.getLocation().toVector().subtract(arrow.getLocation().toVector()).normalize().multiply(2));
                        }
                    }
                }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
            }
        }
        if (player.getScoreboardTags().contains("homingApollo")) {
            Entity arrow = event.getProjectile();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isDead() || arrow.isOnGround()) {
                        this.cancel();
                    }

                    List<Entity> nearest = arrow.getNearbyEntities(20, 20, 20);
                    LivingEntity target = null;
                    for (Entity near : nearest) {
                        if (near != player && near instanceof LivingEntity livingEntityNear && !livingEntityNear.isDead() && player.hasLineOfSight(livingEntityNear)) {
                            if (target == null) {
                                target = livingEntityNear;
                            } else if (arrow.getLocation().distanceSquared(livingEntityNear.getEyeLocation()) < arrow.getLocation().distanceSquared(target.getEyeLocation())) {
                                target = livingEntityNear;
                            }
                        }
                    }
                    if (target == null) return;
                    arrow.setVelocity(target.getEyeLocation().toVector().subtract(arrow.getLocation().toVector()).normalize().multiply(2));
                }
            }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        }
    }
}
