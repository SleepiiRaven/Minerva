package net.minervamc.minerva.listeners;

import java.util.List;
import java.util.Random;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.greek.poseidon.AquaticLimbExtensions;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkillListener implements Listener {
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            event.setCancelled(true);
            return;
        }

        if (event.getDrops() != null && event.getEntity().getScoreboardTags().contains("aresSummoned"))
            event.getDrops().clear();
    }

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
                        } else {
                            double healing = 1;
                            player.setHealth(player.getHealth() + healing);
                        }
                    } else if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.ARES_BLESSING && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
                        int amp = 0;
                        if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
                            amp = player.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1;
                        }
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, amp));
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
        if (player.getInventory().getItemInMainHand().getType() == Material.BOW) {
            if (PlayerStats.getStats(event.getPlayer().getUniqueId()).getPassive() == Skills.HUNTRESS_AGILITY && PlayerStats.getStats(event.getPlayer().getUniqueId()).getPassiveActive()) {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5, 0));
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
            } else if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
                if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.DRUNKEN_REVELRY && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0));
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
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arrow originalArrow = (Arrow) event.getProjectile();
        if (PlayerStats.getStats(player.getUniqueId()).getPassive() == Skills.ARROWS_OF_THE_SUN && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
            Random random = new Random();
            int randomInt = random.nextInt(0, 5);
            if (randomInt == 1) {
                Vector velocity = event.getProjectile().getVelocity();
                SpectralArrow arrow = originalArrow.getWorld().spawnArrow(originalArrow.getLocation(), event.getProjectile().getVelocity(), 1f, 1f, SpectralArrow.class);
                event.getProjectile().remove();
                arrow.setDamage(originalArrow.getDamage());
                arrow.setShooter(player);
                arrow.setVelocity(velocity);
                if (player.getScoreboardTags().contains("homingApollo") || player.getScoreboardTags().contains("homingArtemis")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isDead() || !player.isOnline() || arrow.isDead() || arrow.isOnGround()) {
                                this.cancel();
                            }
                            List<Entity> nearest;
                            nearest = arrow.getNearbyEntities(5, 5, 5);
                            LivingEntity target = null;
                            for (Entity near : nearest) {
                                if (near != player && near instanceof LivingEntity livingEntityNear && !livingEntityNear.isInvulnerable() && !livingEntityNear.isDead() && player.hasLineOfSight(livingEntityNear) && !(livingEntityNear instanceof Player livingPlayer && (livingPlayer.getGameMode() == GameMode.CREATIVE || livingPlayer.getGameMode() == GameMode.SPECTATOR || Party.isPlayerInPlayerParty(player, livingPlayer)))) {
                                    if (player.getScoreboardTags().contains("homingApollo")) {
                                        if (target == null) {
                                            target = livingEntityNear;
                                        } else if (arrow.getLocation().distanceSquared(livingEntityNear.getEyeLocation()) < arrow.getLocation().distanceSquared(target.getEyeLocation())) {
                                            target = livingEntityNear;
                                        }
                                    } else {
                                        if (!near.getScoreboardTags().contains("artemisWolf")) {
                                            if (target == null) {
                                                target = livingEntityNear;
                                            } else if (arrow.getLocation().distanceSquared(livingEntityNear.getEyeLocation()) < arrow.getLocation().distanceSquared(target.getEyeLocation())) {
                                                target = livingEntityNear;
                                            }
                                        }
                                    }
                                }
                            }
                            if (target == null) return;
                            arrow.setVelocity(target.getEyeLocation().toVector().subtract(arrow.getLocation().toVector()).normalize().multiply(2));
                        }
                    }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isDead() || !player.isOnline() || arrow.isDead()) {
                            World world = arrow.getWorld();
                            Location loc = arrow.getLocation();
                            world.spawnParticle(Particle.DUST, loc, 10, 0, 0, 0, 0.5, new Particle.DustOptions(Color.fromRGB(250, 250, 210), 2));
                            world.spawnParticle(Particle.END_ROD, loc, 50, 0, 0, 0, 0.3);
                            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.5f, 1.2f);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 2f);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.2f, 1f);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2f, 2f);
                            for (Entity nearbyEntity : loc.getNearbyEntities(4, 4, 4)) {
                                if (nearbyEntity instanceof LivingEntity target && target != player && !(target instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                                    Skill.damage(target, arrow.getDamage() * 0.8, player);
                                }
                            }
                            this.cancel();
                            return;
                        }
                        if (arrow.isOnGround()) {
                            arrow.remove();
                            this.cancel();
                        }
                    }
                }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
            }
        }
        if (player.getScoreboardTags().contains("homingApollo") || player.getScoreboardTags().contains("homingArtemis")) {
            Arrow finalOriginalArrow = originalArrow;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isDead() || !player.isOnline() || finalOriginalArrow.isDead() || finalOriginalArrow.isOnGround()) {
                        this.cancel();
                    }
                    List<Entity> nearest;
                    nearest = finalOriginalArrow.getNearbyEntities(5, 5, 5);
                    LivingEntity target = null;
                    for (Entity near : nearest) {
                        if (near != player && near instanceof LivingEntity livingEntityNear && !livingEntityNear.isInvulnerable() && !livingEntityNear.isDead() && player.hasLineOfSight(livingEntityNear) && !(livingEntityNear instanceof Player livingPlayer && (livingPlayer.getGameMode() == GameMode.CREATIVE || livingPlayer.getGameMode() == GameMode.SPECTATOR || Party.isPlayerInPlayerParty(player, livingPlayer)))) {
                            if (player.getScoreboardTags().contains("homingApollo")) {
                                if (target == null) {
                                    target = livingEntityNear;
                                } else if (finalOriginalArrow.getLocation().distanceSquared(livingEntityNear.getEyeLocation()) < finalOriginalArrow.getLocation().distanceSquared(target.getEyeLocation())) {
                                    target = livingEntityNear;
                                }
                            } else {
                                if (!near.getScoreboardTags().contains("artemisWolf")) {
                                    if (target == null) {
                                        target = livingEntityNear;
                                    } else if (finalOriginalArrow.getLocation().distanceSquared(livingEntityNear.getEyeLocation()) < finalOriginalArrow.getLocation().distanceSquared(target.getEyeLocation())) {
                                        target = livingEntityNear;
                                    }
                                }
                            }
                        }
                    }
                    if (target == null) return;
                    finalOriginalArrow.setVelocity(target.getEyeLocation().toVector().subtract(finalOriginalArrow.getLocation().toVector()).normalize().multiply(2));
                }
            }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        }
    }

    @EventHandler
    public void onTargetEntity(EntityTargetLivingEntityEvent event) {
        if (event.getEntity().getScoreboardTags().contains("aresSummoned")) {
            if (!(event.getTarget() instanceof LivingEntity potentialTarget) || (potentialTarget instanceof Tameable && ((Tameable) potentialTarget).getOwner() != null) || potentialTarget.getScoreboardTags().contains("aresSummoned")) {
                event.setCancelled(true);
            } else if (event.getTarget() instanceof Player player) {
                if (event.getEntity().getScoreboardTags().contains(player.getUniqueId().toString())) {
                    event.setCancelled(true);
                } else if (Party.isInParty(player)) {
                    for (Player partyMember : Party.partyList(player)) {
                        if (event.getEntity().getScoreboardTags().contains(partyMember.getUniqueId().toString()))
                            event.setCancelled(true);
                    }
                }
            }
        }
    }
}
