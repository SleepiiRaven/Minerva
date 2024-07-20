package net.minervamc.minerva.skills.greek.ares;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Bloodlust extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int radius = 3;
        int duration = 60; //ticks
        int skeletonCount = 3;
        int angerRange = 20;
        Skeleton[] skeletons = new Skeleton[skeletonCount];
        Location playerLocation = player.getLocation();
        for (int i = 0; i < skeletonCount; i++) {
            Location skeletonLocation = playerLocation.clone().add(FastUtils.randomDoubleInRange(-radius, radius), 0, FastUtils.randomDoubleInRange(-radius, radius));
            Vector skeletonDirection = new Vector(FastUtils.randomDoubleInRange(-1, 1), 0, FastUtils.randomDoubleInRange(-1, 1));
            AtomicBoolean isSolid = new AtomicBoolean(false);
            player.getWorld().getChunkAtAsync(skeletonLocation, chunk -> {
                if (chunk.isLoaded()) {
                    if (skeletonLocation.getBlock().isSolid()) {
                        isSolid.set(true);
                    }
                }
            });
            if (isSolid.get()) {
                i -= 1;
                continue;
            }

            Bukkit.getConsoleSender().sendMessage("is not solid");

            Skeleton skeleton = (Skeleton) player.getWorld().spawnEntity(skeletonLocation.clone().setDirection(skeletonDirection), EntityType.SKELETON);
            skeleton.addScoreboardTag("aresSkeleton");
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            skeleton.getEquipment().setHelmet(new ItemStack(Material.TURTLE_HELMET));
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (duration*5), 4));
            skeleton.setInvulnerable(true);
            skeleton.setAI(false);
            skeletons[i] = skeleton;
        }

        new BukkitRunnable() {
            boolean allAboveGround = true;
            @Override
            public void run() {
                for (int i = 0; i < skeletonCount; i++) {
                    Skeleton skeleton = skeletons[i];
                    if (!allAboveGround) {
                        AtomicBoolean isSolid = new AtomicBoolean(false);
                        player.getWorld().getChunkAtAsync(skeleton.getLocation(), chunk -> {
                            if (chunk.isLoaded()) {
                                if (skeleton.getLocation().getBlock().isSolid()) {
                                    isSolid.set(true);
                                }
                            }
                        });

                        if (isSolid.get()) {
                            skeleton.teleport(skeleton.getLocation().add(0, 0.1, 0));
                            allAboveGround = false;
                            continue;
                        }

                        skeleton.setInvulnerable(false);
                        skeleton.setAI(true);
                    }
                    if (skeleton.getTarget() == player) skeleton.setTarget(null);
                    LivingEntity target = null;
                    for (Entity entity : player.getNearbyEntities(angerRange, angerRange, angerRange)) {
                        if (entity instanceof LivingEntity livingEntity && livingEntity != player && !entity.isInvulnerable() && !entity.isDead() && !livingEntity.getScoreboardTags().contains("artemisWolf") && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                            if (target == null) {
                                target = livingEntity;
                            } else if (player.getLocation().distanceSquared(livingEntity.getLocation()) < player.getLocation().distanceSquared(target.getLocation())) {
                                target = livingEntity;
                            }
                        }
                    }
                    if (skeleton.getTarget() == null && target != null) {
                        skeleton.setTarget(target);
                    }
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "bloodlust";
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Material.VINE);
    }
}
