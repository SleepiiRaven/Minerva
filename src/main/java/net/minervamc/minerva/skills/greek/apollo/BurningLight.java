package net.minervamc.minerva.skills.greek.apollo;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
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
        int maxTicks = 20; // MUST BE DURATION MILLIS IN TICKS, DIVIDED BY THE TIME BETWEEN TRIGGERS!
        long durationMillis = 50 * maxTicks;
        long cooldown = durationMillis + 9000;
        double distanceThrown = 15;
        int damage = 4;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "burningLight")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "burningLight", cooldown);
        cooldownAlarm(player, cooldown, "Burning Light");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1f);

        new BukkitRunnable() {
            final Vector savedDirection = player.getEyeLocation().getDirection();
            final Location savedLocation = player.getEyeLocation();
            int ticks = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks > maxTicks || !player.isOnline()) {
                    this.cancel();
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.7f);
                }

                ticks++;

                Location sphereLoc = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().clone().multiply(4));

                for (Entity entity : sphereLoc.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity livingEntity && entity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                        damage(livingEntity, damage, player);
                    }
                }

                int i = 0;
                for (Vector point : ParticleUtils.getSpherePoints(radius, 3)) {
                    i++;
                    Location particleLocation = sphereLoc.clone().add(point);
                    particleLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(184, 134, 11), 2));
                    if (i % 3 == 0)
                        particleLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 2));
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
        return ItemUtils.getItem(new ItemStack(Material.RAW_GOLD), ChatColor.GOLD + "" + ChatColor.BOLD + "[Burning Light]", ChatColor.GRAY + "Summon a ball of light in front", ChatColor.GRAY + "of you that follows your movements", ChatColor.GRAY + "and works like a makeshift", ChatColor.GRAY + "shield, dealing massive", ChatColor.GRAY + "damage to those who step in it.");
    }
}
