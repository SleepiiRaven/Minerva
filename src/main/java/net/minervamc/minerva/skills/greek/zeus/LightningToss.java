package net.minervamc.minerva.skills.greek.zeus;

import java.util.Random;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class LightningToss extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int maxDistance;
        int maxBranches;
        double damage;
        long cooldown;
        switch (level) {
            case 2 -> {
                maxDistance = 12;
                maxBranches = 5;
                damage = 16;
                cooldown = 2750;
            }
            case 3 -> {
                maxDistance = 15;
                maxBranches = 6;
                damage = 18;
                cooldown = 2000;
            }
            case 4 -> {
                maxDistance = 17;
                maxBranches = 10;
                damage = 20;
                cooldown = 2000;
            }
            case 5 -> {
                maxDistance = 20;
                maxBranches = 12;
                damage = 25;
                cooldown = 1500;
            }
            default -> {
                maxDistance = 10;
                maxBranches = 5;
                damage = 12;
                cooldown = 4000;
            }
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "lightningToss")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "lightningToss", cooldown);
        cooldownAlarm(player, cooldown, "Lightning Toss");

        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);
        branch(player, player.getEyeLocation(), player.getLocation().getDirection(), maxDistance, maxBranches, damage);
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

    private void branch(Player player, Location location, Vector direction, int maxDistance, int maxBranches, double damage) {
        int rotationMax = 20;
        Random random = new Random();

        for (double i = 0; i <= maxDistance; i += 0.2) {
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(direction.clone().multiply(i)), 0);
            player.getWorld().spawnParticle(Particle.DUST, location.clone().add(direction.clone().multiply(i)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(254, 191, 16), 2));
            for (Entity entity : location.clone().add(direction.clone().multiply(i)).getNearbyEntities(1, 1, 1)) {
                if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                    SkillUtils.damage(livingEntity, damage, player);
                    livingEntity.setVelocity(direction.multiply(0.3));
                    return;
                }
            }

            if (random.nextInt(0, 20) == 2 && maxBranches > 0) {
                double xRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                double yRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                double zRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                branch(player, location.clone().add(direction.clone().multiply(i)), ParticleUtils.rotateYAxis(ParticleUtils.rotateZAxis(ParticleUtils.rotateXAxis(direction, xRotation), zRotation), yRotation), (int) (maxDistance - i), maxBranches - 1, damage);
                xRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                yRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                zRotation = FastUtils.randomDoubleInRange(-rotationMax, rotationMax);
                branch(player, location.clone().add(direction.clone().multiply(i)), ParticleUtils.rotateYAxis(ParticleUtils.rotateZAxis(ParticleUtils.rotateXAxis(direction, xRotation), zRotation), yRotation), (int) (maxDistance - i), maxBranches - 1, damage);
                return;
            }
        }
    }

    @Override
    public String toString() {
        return "lightningToss";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BLAZE_ROD), ChatColor.YELLOW + "" + ChatColor.BOLD + "[Lightning Toss]", ChatColor.GRAY + "Quickly shoot a massive branching beam of lightning in the direction you are looking.");
    }
}
