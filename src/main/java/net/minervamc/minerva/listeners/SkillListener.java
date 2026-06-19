package net.minervamc.minerva.listeners;

import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.SkillTriggers;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import net.minervamc.minerva.skills.greek.athena.TacticalAgility;
import net.minervamc.minerva.skills.greek.demeter.VineGrapple;
import net.minervamc.minerva.skills.greek.hermes.FleetFootwork;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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

        Set<String> tags = event.getEntity().getScoreboardTags();

        if (event.getDrops() != null &&
                (tags.contains("aresSummoned") ||
                tags.contains("livingForge")))
            event.getDrops().clear();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // A player mid spell-cast (Q combo): left-click swings are UI input, not melee attacks.
        if (event.getDamager() instanceof Player castingDamager && SkillTriggers.isCasting(castingDamager)) {
            event.setCancelled(true);
            return;
        }
        // Soft-CC: asleep or feared entities cannot attack.
        if (Skill.isAsleep(event.getDamager()) || Skill.isFeared(event.getDamager())) {
            event.setCancelled(true);
            return;
        }
        // Nemesis: damage dealt by a marked enemy feeds its marker's Ledger.
        net.minervamc.minerva.skills.greek.nemesis.MarkOfHubris.feedIfMarked(event.getDamager(), event.getFinalDamage());
        if (event.getDamager().getScoreboardTags().contains("charmed") && event.getDamager().getScoreboardTags().contains(event.getEntity().getUniqueId().toString())) {
            event.setCancelled(true);
        }

        if (event.getEntity().hasMetadata("NPC") && event.getEntity().getScoreboardTags().contains("mirrorImage")) {
            Entity entity = event.getEntity();
            if (event.getDamager() instanceof Player player && PlayerStats.isSummoned(player, entity)) {
                event.setCancelled(true);
                return;
            } else {
                entity.remove();
            }
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        TacticalAgility.onWeaponHit(player, player.getInventory().getItemInMainHand());
        FleetFootwork.onWeaponHit(player, event);

        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        Skill passive = stats.getPassive();
        boolean passiveActive = stats.getPassiveActive();
        if (passive == Skills.SMOLDER && passiveActive) {
            int stacks = Skill.getStacks(player, "smolder");
            if (stacks > 0) {
                event.setDamage(event.getDamage() + event.getDamage() * 0.06 * Math.pow(stacks, 2));
                Skill.stack(player, "smolder", -5, "Smolder", 0);
            }
        }

        // New-heritage passives that proc on dealing damage
        if (passiveActive && event.getEntity() instanceof LivingEntity victim) {
            int pLevel = stats.getPassiveLevel();
            if (passive == Skills.FROSTBITE) {
                net.minervamc.minerva.skills.greek.khione.Frostbite.onWeaponHit(player, victim, pLevel);
            } else if (passive == Skills.SANDMAN) {
                net.minervamc.minerva.skills.greek.hypnos.Sandman.onWeaponHit(player, victim, pLevel);
            } else if (passive == Skills.PRISM) {
                net.minervamc.minerva.skills.greek.iris.Prism.onWeaponHit(player, victim, pLevel);
            } else if (passive == Skills.THE_INEVITABLE) {
                net.minervamc.minerva.skills.greek.thanatos.TheInevitable.onWeaponHit(player, victim, pLevel);
            } else if (passive == Skills.SHARED_FATE) {
                net.minervamc.minerva.skills.greek.arke.SharedFate.onOwnerDealDamage(player, victim, event.getFinalDamage(), pLevel);
            } else if (passive == Skills.WAR_FOOTING) {
                net.minervamc.minerva.skills.greek.bellona.WarFooting.onOwnerDealDamage(player, victim, event, pLevel);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // if it's a kill
                if (event.getEntity() instanceof LivingEntity entity && entity.getHealth() == 0) {
                    if (passive == Skills.LIFE_STEAL && PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) {
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
        if (player.hasMetadata("NPC")) return;
        FleetFootwork.onMove(event);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        // Sleep breaks instantly on any real damage (applies to all entities, not just players).
        if (event.getFinalDamage() > 0.1) {
            Skill.wake(event.getEntity());
        }
        if (event.getEntity() instanceof Player player) {
            if (player.hasMetadata("NPC")) {
                if (player.getScoreboardTags().contains("mirrorImage")) {
                    if (event.getDamageSource().getCausingEntity() == null) {
                        event.setCancelled(true);
                    }
                }
                return;
            }
            PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
            Skill passive = stats.getPassive();
            boolean passiveActive = stats.getPassiveActive();

            // New-heritage passives that react to taking damage
            if (passiveActive) {
                int pLevel = stats.getPassiveLevel();
                if (passive == Skills.LEDGER_OF_WRONGS) {
                    net.minervamc.minerva.skills.greek.nemesis.LedgerOfWrongs.onTakeDamage(player, event, pLevel);
                } else if (passive == Skills.BANKED_EMBERS) {
                    net.minervamc.minerva.skills.greek.hestia.BankedEmbers.onTakeDamage(player);
                } else if (passive == Skills.IRIDESCENT_SOUL) {
                    net.minervamc.minerva.skills.greek.psyche.IridescentSoul.onTakeDamage(player, event, pLevel);
                }
            }

            if (event.getFinalDamage() > 0.1 && passiveActive && event.getDamageSource().getCausingEntity() != null && event.getDamageSource().getCausingEntity() != event.getEntity()) {
                if (passive == Skills.SMOLDER) {
                    Skill.stack(player, "smolder", 1, "Smolder", 5000);
                } else if (player.getScoreboardTags().contains("athenaParry")) {
                    player.addScoreboardTag("athenaParrySuccess");
                    event.setCancelled(true);
                }
            } else if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (FleetFootwork.shouldCancelFall(player)) {
                    event.setCancelled(true);
                } else if (passive == Skills.PROTECTIVE_CLOUD && passiveActive) {
                    event.setCancelled(true);
                } else if (player.getScoreboardTags().contains("vineGrappleActive")) {
                    event.setCancelled(true);
                }
            } else if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
                if (passive == Skills.DRUNKEN_REVELRY && passiveActive) {
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
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player sneaker = event.getPlayer();
        if (VineGrapple.isLatched(sneaker)) {
            VineGrapple.releaseGrapple(sneaker); // release latch, stay in vine state to re-fire
        } else {
            VineGrapple.exitState(sneaker); // cancel state entirely when not latched
        }
    }

    @EventHandler
    public void onPlayerPunch(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            if (player.getScoreboardTags().contains("vineGrappleActive")) {
                VineGrapple.onLeftClick(player);
            }
            if (AquaticLimbExtensions.waterBlocks.containsKey(player.getUniqueId())) {
                Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "aquaticPunching", AquaticLimbExtensions.punchDurationMillis);
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.hasMetadata("NPC")) return;
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
                                        if (PlayerStats.isSummoned(player, near)) {
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
        if (event.getTarget() instanceof Player player && PlayerStats.isSummoned(player, event.getEntity()))
            event.setCancelled(true);
        if (PlayerStats.whoSummonedMe(event.getEntity()) != null) {
            if (!(event.getTarget() instanceof LivingEntity potentialTarget) || (potentialTarget instanceof Tameable && ((Tameable) potentialTarget).getOwner() != null)) {
                event.setCancelled(true);
            } else if (event.getTarget() instanceof Player player) {
                if (PlayerStats.isSummoned(player, event.getEntity())) {
                    event.setCancelled(true);
                } else if (Party.isInParty(player)) {
                    for (Player partyMember : Party.partyList(player)) {
                        if (PlayerStats.isSummoned(partyMember, event.getEntity()))
                            event.setCancelled(true);
                    }
                }
            }
        }
    }
}
