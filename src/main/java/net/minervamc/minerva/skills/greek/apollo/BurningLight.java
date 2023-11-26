package net.minervamc.minerva.skills.greek.apollo;

import net.kyori.adventure.sound.SoundStop;
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
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BurningLight extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int radius = 1;
        long durationMillis = 5000;
        long cooldown = durationMillis + 9000;
        int maxTicks = 20; // MUST BE DURATION MILLIS IN TICKS, DIVIDED BY THE TIME BETWEEN TRIGGERS!
        double distanceThrown = 10;
        double punchingSpeedTicks = 5;
        int fireTicks = 40;
        int damage = 5;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "burningLight")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "burningLightNotPunching", durationMillis);
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "burningLight", cooldown);
        cooldownAlarm(player, cooldown, "Burning Light");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1f);

        new BukkitRunnable() {
            boolean punching = false;
            int punchingTicks = 0;
            int ticks = 0;
            Vector savedDirection = player.getEyeLocation().getDirection();
            Location savedLocation = player.getEyeLocation();
            @Override
            public void run() {
                if (ticks > maxTicks || !player.isOnline()) {
                    this.cancel();
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.7f);
                }

                if (!cooldownManager.isCooldownDone(player.getUniqueId(),"burningLightPunching")) {
                    if (ticks == 0) {
                        cooldownManager.setCooldownFromNow(player.getUniqueId(), "burningLightPunching", 0L);
                    } else {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                        cooldownManager.setCooldownFromNow(player.getUniqueId(), "burningLightPunching", 0L);
                        cooldownManager.setCooldownFromNow(player.getUniqueId(), "burningLightNotPunching", 0L);
                        punching = true;
                        savedLocation = player.getEyeLocation();
                        savedDirection = player.getEyeLocation().getDirection();
                    }
                }
                ticks++;

                Location sphereLoc = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().clone().multiply(2));

                if (punching) {
                    double distancePerTick = distanceThrown/punchingSpeedTicks;
                    if (punchingTicks > punchingSpeedTicks) {
                        this.cancel();
                        return;
                    }
                    sphereLoc = savedLocation.clone().add(savedDirection.clone().multiply(2 + (distancePerTick * punchingTicks)));
                    punchingTicks++;
                }

                for (Entity entity : sphereLoc.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.damage(damage);
                        livingEntity.setFireTicks(Math.min(livingEntity.getFireTicks() + fireTicks, livingEntity.getMaxFireTicks()));
                    }
                }

                if (punching) sphereLoc.getWorld().spawnParticle(Particle.END_ROD, sphereLoc, 20);

                for (Vector point : ParticleUtils.getSpherePoints(radius)) {
                    Location particleLocation = sphereLoc.clone().add(point);
                    particleLocation.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(184,134,11), 2));
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 5L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "?";
    }

    @Override
    public String toString() {
        return "burningLight";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ALLIUM), ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Burning Light", ChatColor.GRAY + "Summon a ball of light, and left click to throw it.");
    }
}
