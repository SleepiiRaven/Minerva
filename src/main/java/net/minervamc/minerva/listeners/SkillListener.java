package net.minervamc.minerva.listeners;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.greek.poseidon.AquaticLimbExtensions;
import net.minervamc.minerva.types.HeritageType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

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
            }
        }
    }
}
