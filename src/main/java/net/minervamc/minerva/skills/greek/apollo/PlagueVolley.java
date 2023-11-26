package net.minervamc.minerva.skills.greek.apollo;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlagueVolley extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 5000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "plagueVolley")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "plagueVolley", cooldown);
        cooldownAlarm(player, cooldown, "Plague Volley");

        Location location = player.getLocation();
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
            Arrow arrow = (Arrow) player.getWorld().spawnEntity(location.add(arrowDirection).setDirection(arrowDirection), EntityType.ARROW);
            arrow.setVelocity(arrowDirection.clone().multiply(3));
            arrow.setBasePotionData(new PotionData(PotionType.POISON));
            arrows.add(arrow);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean allDead = true;
                for (Arrow arrow : arrows) {
                    if (arrow.isDead() || arrow.isOnGround()) {
                        arrow.remove();
                    } else {
                        allDead = false;
                    }
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
