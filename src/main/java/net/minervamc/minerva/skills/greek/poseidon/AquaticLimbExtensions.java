package net.minervamc.minerva.skills.greek.poseidon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AquaticLimbExtensions extends Skill {
    public static long punchDurationMillis = 51;
    public static HashMap<UUID, List<Block>> waterBlocks = new HashMap<>();
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        if (!waterBlocks.containsKey(player.getUniqueId())) {
            waterBlocks.put(player.getUniqueId(), new ArrayList<>());
        }
        double distanceNormal = 5;
        double distanceExtended = 15;
        int maxPunchingTicks = 5; // Takes twice as long to do a full punch
        long punchCooldown = (maxPunchingTicks * 2) * 50;
        long durationTicks = maxPunchingTicks * 20;
        long cooldown = durationTicks*50 + 9000;
        double damage = 5;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "aquaticLimbExtensions")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "aquaticLimbExtensions", cooldown);
        cooldownAlarm(player, cooldown, "Aquatic Limb Extensions");

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;
            boolean punching = false;
            boolean recoiling = false;
            boolean leftArm = false;
            int punchingTicks = 0;
            Location savedLocation = player.getEyeLocation();
            Vector savedDirection = savedLocation.getDirection();

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                    reset(player);
                    this.cancel();
                    return;
                }

                Location viewPos = player.getEyeLocation();
                Vector viewDir = viewPos.getDirection();
                Location startLocLeft = viewPos.clone().add(ParticleUtils.rotateYAxis(viewDir.clone().multiply(2), -90));
                Location startLocRight = viewPos.clone().add(ParticleUtils.rotateYAxis(viewDir.clone().multiply(2), 90));
                Vector directionLeft = ParticleUtils.getDirection(startLocLeft, viewPos.clone().add(viewDir.clone().multiply(distanceExtended + 5)));
                Vector directionRight = ParticleUtils.getDirection(startLocRight, viewPos.clone().add(viewDir.clone().multiply(distanceExtended + 5)));

                if (!cooldownManager.isCooldownDone(player.getUniqueId(), "aquaticPunching") && cooldownManager.isCooldownDone(player.getUniqueId(), "aquaticLimbExtensionPunch")) {
                    leftArm = !leftArm;
                    punching = true;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.2f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7f, 1f);
                    cooldownManager.setCooldownFromNow(player.getUniqueId(), "aquaticLimbExtensionPunch", punchCooldown);
                }

                if (viewDir != savedDirection || viewPos != savedLocation) {
                    reset(player);
                    savedDirection = viewDir;
                    savedLocation = viewPos;
                } else return;


                double addDistance = distanceExtended / maxPunchingTicks;

                if (punchingTicks >= 10 && punching) {
                    punching = false;
                    recoiling = true;
                } else if (punchingTicks <= 0 && recoiling) {
                    punching = false;
                    recoiling = false;
                    punchingTicks = 0;
                }

                if (punching) {
                    punchingTicks++;
                } else if (recoiling) {
                    reset(player);
                    punchingTicks--;
                }

                if (leftArm) {
                    if (punching) {
                        waterArmEffect(player, startLocLeft, directionLeft, distanceNormal + (punchingTicks * addDistance), damage);
                    } else {
                        waterArmEffect(player, startLocLeft, directionLeft, distanceNormal + (punchingTicks * addDistance), 0);
                    }
                    waterArmEffect(player, startLocRight, directionRight, distanceNormal, 0);
                } else {
                    waterArmEffect(player, startLocLeft, directionLeft, distanceNormal, 0);
                    if (punching) {
                        waterArmEffect(player, startLocRight, directionRight, distanceNormal + (punchingTicks * addDistance), damage);
                    } else {
                        waterArmEffect(player, startLocRight, directionRight, distanceNormal + (punchingTicks * addDistance), 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
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

    private void waterArmEffect(Player player, Location start, Vector direction, double length, double damage) {
        for (double t = 0; t < length; t += 1) {
            Vector effect = direction.clone().multiply(t);
            Location effectLocation = start.clone().add(effect);
            if (effectLocation.getBlock().getType().isAir()) {
                Block block = effectLocation.getBlock();
                block.setType(Material.WATER);
                waterBlocks.get(player.getUniqueId()).add(block);

                for (Entity entity : effectLocation.getNearbyEntities(1, 2, 1)) {
                    if (damage != 0 && entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Horse)) {
                        livingEntity.damage(damage, player);
                        livingEntity.setVelocity(direction.clone().multiply(0.5));
                    }
                }
            }
        }
    }

    private void reset(Player player) {
        for (Block block : waterBlocks.get(player.getUniqueId())) {
            if (block != null) {
                block.setType(Material.AIR);
            }
        }
        waterBlocks.get(player.getUniqueId()).clear();
    }

    @Override
    public String toString() {
        return "aquaticLimbExtensions";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.TRIDENT), ChatColor.BLUE + "" + ChatColor.BOLD + "[Aquatic Limb Extensions]", ChatColor.GRAY + "Extend your limbs with water for a short period of time.", ChatColor.GRAY + "While your limbs are extended, when you left click, the Aquatic Limb Extensions punch with you.");
    }
}
