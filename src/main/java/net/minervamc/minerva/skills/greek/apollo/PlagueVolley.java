package net.minervamc.minerva.skills.greek.apollo;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Sound;

public class PlagueVolley extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 5000;
        double damage = 5;

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
            default -> {}
            case 3,4 -> {
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, -30));
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, 30));
            }
            case 5 -> {
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, -45));
                arrowDirections.add(ParticleUtils.rotateYAxis(direction, 45));
            }
        }

        List<Arrow> arrows = new ArrayList<>();
        for (Vector arrowDirection : arrowDirections) {
            Arrow arrow = player.getWorld().spawnArrow(location.clone().add(arrowDirection).setDirection(arrowDirection), arrowDirection.clone().multiply(0.8), 1f, 0f);
            arrow.setShooter(player);
            arrow.setVelocity(arrowDirection.clone().multiply(3));
            arrow.setBasePotionType(PotionType.POISON);
            arrow.setDamage(damage);
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
        return ItemUtils.getItem(new ItemStack(Material.WITHER_ROSE), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Plague Volley]", ChatColor.GRAY + "Shoot a volley of arrows imbued with the plague.");
    }
}
