package net.minervamc.minerva.skills.greek.apollo;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlagueVolley extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 5000;
        double damage = 2.5;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "plagueVolley")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "plagueVolley", cooldown);
        cooldownAlarm(player, cooldown, "Plague Volley");

        Location location = player.getEyeLocation();
        Vector direction = location.getDirection();

        location.getWorld().playSound(location, Sound.ITEM_CROSSBOW_SHOOT, 1.6f, 1f);
        location.getWorld().playSound(location, Sound.ENTITY_ARROW_SHOOT, 1.2f, 1f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_2, 1f, 1f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RETURN, 1.2f, 1f);

        List<Vector> arrowDirections = new ArrayList<>();
        arrowDirections.add(direction);
        arrowDirections.add(ParticleUtils.rotateYAxis(direction, -15));
        arrowDirections.add(ParticleUtils.rotateYAxis(direction, 15));
        switch (level) {
            case 3, 4 -> {
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, -30));
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, 30));
            }
            case 5 -> {
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, -45));
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, 45));
            }
            default -> {
            }
        }

        List<Arrow> arrows = new ArrayList<>();
        for (Vector arrowDirection : arrowDirections) {
            Arrow arrow = player.getWorld().spawnArrow(location.clone().add(arrowDirection).setDirection(arrowDirection), arrowDirection.clone().multiply(0.8), 1f, 0f);
            arrow.setShooter(player);
            arrow.setVelocity(arrowDirection.clone().multiply(3));
            arrow.setBasePotionType(PotionType.POISON);
            arrow.setDamage(damage);
            if (player.getScoreboardTags().contains("homingApollo")) {
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
            arrows.add(arrow);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                boolean allDead = true;
                for (Arrow arrow : arrows) {
                    if (arrow.isDead() || arrow.isOnGround()) {
                        arrow.remove();
                        continue;
                    } else {
                        allDead = false;
                    }

                    List<Vector> spiralPoints = ParticleUtils.getVerticalCirclePoints(1, arrow.getLocation().getPitch(), arrow.getLocation().getYaw(), 10);
                    if (ticks >= 10) ticks = 0;
                    Location particleLoc = arrow.getLocation().add(spiralPoints.get(ticks));
                    particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GREEN, 1f));
                    ticks++;
                }
                if (allDead) this.cancel();
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 5L);
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

    @Override
    public String toString() {
        return "plagueVolley";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.WITHER_ROSE), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Plague Volley]", ChatColor.GRAY + "Shoot a volley of arrows", ChatColor.GRAY + "imbued with the plague.");
    }
}
