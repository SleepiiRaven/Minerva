package net.minervamc.minerva.skills.greek.artemis;

import javax.sql.PooledConnection;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SuperCharged extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long chargeTime = 40;
        double maxRadius = 7;
        double range = 30;
        double normalDamage = 200;
        double headshotDamage = 400;
        long cooldown = 6000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "superCharged")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "superCharged", cooldown);
        cooldownAlarm(player, cooldown, "Super Charged");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_QUICK_CHARGE_3, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);

        new BukkitRunnable() {
            double radius = maxRadius;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (radius < 2) {
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    Location loc = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().clone().multiply(3));
                    for (int i = 0; i < range; i++) {
                        Location particleLoc = loc.clone().add(loc.getDirection().clone().multiply(i));
                        if (particleLoc.getBlock().isSolid()) {
                            break;
                        }
                        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLoc, 1, 0, 0, 0, 0);
                        player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0 ,0);
                        player.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 3, 0, 0, 0, 0, new Particle.DustOptions(Color.SILVER, 2));
                        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, particleLoc, 1, 0, 0, 0, 0);
                        for (Entity entity : particleLoc.getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity livingEntity && livingEntity != player && !livingEntity.getScoreboardTags().contains("artemisWolf") && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                                if (particleLoc.distance(livingEntity.getEyeLocation()) <= 0.3) SkillUtils.damage(livingEntity, headshotDamage, player);
                                else SkillUtils.damage(livingEntity, normalDamage, player);
                            }
                        }
                    }
                    this.cancel();
                    return;
                }

                Location effectLocation = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().clone().multiply(3));

                for (Vector vector : ParticleUtils.getVerticalCirclePoints(radius, player.getEyeLocation().getPitch(), player.getEyeLocation().getYaw(), 20)) {
                    Location particleLocation = effectLocation.clone().add(vector);
                    player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.SILVER, 2f));
                }
                radius -= maxRadius/chargeTime;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "superCharged";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.TRIDENT), ChatColor.BOLD + "" + ChatColor.YELLOW + "[Super Charged]", ChatColor.GRAY + "With the careful precision and patience of a Huntress,", ChatColor.GRAY + "spend a moment to line up your bow shot with a silver bolt from", ChatColor.GRAY + "your quiver to deliver a devastating shot that deals extra damage on a headshot.");
    }
}
