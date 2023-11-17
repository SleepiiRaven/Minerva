package net.minervamc.minerva.skills.greek.hades;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class UmbrakinesisHades extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        double range; // How far away from particles can a particle hurt an enemy?
        double distance; // How long each beam is
        double damage; // Damage
        double kb; // KB
        long intervalBetweenTriggers; // Interval between each shoot
        double maxTriggers; // How many times the shadow arms trigger
        double velocityMultiplier; // How much velocity the player gains when grappling

        switch (level) {
            default -> {
                cooldown = 8000;
                range = 1.5;
                distance = 5;
                damage = 3;
                kb = ThreadLocalRandom.current().nextDouble(0.3, 0.5);
                intervalBetweenTriggers = 10L;
                maxTriggers = 4;
                velocityMultiplier = 1;
            }
            case 2 -> {
                cooldown = 7000;
                range = 1.5;
                distance = 7.5;
                damage = 3.5;
                kb = ThreadLocalRandom.current().nextDouble(0.3, 0.5);
                intervalBetweenTriggers = 10L;
                maxTriggers = 5;
                velocityMultiplier = 1.5;
            }
            case 3 -> {
                cooldown = 7000;
                range = 1.5;
                distance = 7.5;
                damage = 4;
                kb = ThreadLocalRandom.current().nextDouble(0.5, 1);
                intervalBetweenTriggers = 8L;
                maxTriggers = 5;
                velocityMultiplier = 2;
            }
            case 4 -> {
                cooldown = 6000;
                range = 1.5;
                distance = 10;
                damage = 5;
                kb = ThreadLocalRandom.current().nextDouble(0.5, 1);
                intervalBetweenTriggers = 8L;
                maxTriggers = 6;
                velocityMultiplier = 3;
            }
            case 5 -> {
                cooldown = 5000;
                range = 1.5;
                distance = 12;
                damage = 6;
                kb = ThreadLocalRandom.current().nextDouble(0.7, 1.2);
                intervalBetweenTriggers = 5L;
                maxTriggers = 10;
                velocityMultiplier = 3;
            }
        }

        long duration = (long) (intervalBetweenTriggers * 50 * maxTriggers);


        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "umbrakinesis")) {
            onCooldown(player);
            return;
        } else if (!cooldownManager.isCooldownDone(player.getUniqueId(), "channelingOfTartarusCasting")) {
            skillLocked(player, "you are currently casting Channeling of Tartarus");
            return;
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "umbrakinesisDuration")) {
            if (cooldownManager.getCooldownLeft(player.getUniqueId(), "umbrakinesisDuration") > (duration - ((maxTriggers-1)*intervalBetweenTriggers*50))) {
                hadesUmbraGrapple(player, cooldownManager, cooldown, velocityMultiplier);
                cooldownManager.setCooldownFromNow(player.getUniqueId(), "umbrakinesisDuration", (long) 0);
                return;
            }
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "umbrakinesisDuration", duration);
        new BukkitRunnable() {
            boolean leftArm = true;

            @Override
            public void run() {
                if (cooldownManager.isCooldownDone(player.getUniqueId(), "umbrakinesisDuration")) {
                    this.cancel();
                    cooldownManager.setCooldownFromNow(player.getUniqueId(), "umbrakinesis", cooldown);
                    cooldownAlarm(player, cooldown, "Umbrakinesis");
                    return;
                }

                Location viewPos = player.getEyeLocation();
                Vector viewDir = player.getEyeLocation().getDirection();

                if (leftArm) {
                    viewPos.add(ParticleUtils.rotateYAxis(viewDir.clone().multiply(2), -90));
                } else {
                    viewPos.add(ParticleUtils.rotateYAxis(viewDir.clone().multiply(2), 90));
                }

                player.getWorld().playSound(player.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1f, 1f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);

                for (double t = 0; t < distance; t += 0.5) {
                    double x = viewDir.getX() * t;
                    double y = viewDir.getY() * t;
                    double z = viewDir.getZ() * t;
                    viewPos.add(x, y, z);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, viewPos, 1, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.REDSTONE, viewPos, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(32, 32, 32), 2));
                    player.getWorld().spawnParticle(Particle.REDSTONE, viewPos, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(40, 40, 40), 2));
                    player.getWorld().spawnParticle(Particle.END_ROD, viewPos, 1, 0, 0, 0);
                    Collection<Entity> closebyMonsters = player.getWorld().getNearbyEntities(viewPos, range, range, range);
                    for (Entity closebyMonster : closebyMonsters) {
                        if (!(closebyMonster instanceof LivingEntity livingMonster) || (closebyMonster == player)) continue;
                        livingMonster.damage(damage, player);
                        Vector viewNormalized = (viewDir.normalize()).multiply(kb);
                        livingMonster.setVelocity(viewNormalized);
                    }
                    viewPos.subtract(x, y, z);
                }
                leftArm = !leftArm;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, intervalBetweenTriggers);

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

    private void hadesUmbraGrapple(Player player, CooldownManager cooldownManager, long cooldown, double velocityMultiplier) {
        Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(velocityMultiplier));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 5 && player.isOnGround()) {
                    this.cancel();
                    cooldownManager.setCooldownFromNow(player.getUniqueId(), "umbrakinesis", cooldown);
                    return;
                }
                Location pos = player.getLocation();
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, pos, 1, 0, 0, 0);
                player.getWorld().spawnParticle(Particle.REDSTONE, pos, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(32, 32, 32), 2));
                player.getWorld().spawnParticle(Particle.REDSTONE, pos, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(40, 40, 40), 2));
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String toString() {
        return "umbrakinesisHades";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BLACK_DYE), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "[Umbrakinesis]", ChatColor.GRAY + "Manipulate the shadows beside you and turn them into shadowy tendrils", ChatColor.GRAY + "that attack enemies in front of you. You can cancel this ability by recasting", ChatColor.GRAY + "it before the final hit (the 4th one) to push off the ground using the shadowy tendrils to", ChatColor.GRAY + "fly in the direction that you are facing.");
    }
}
