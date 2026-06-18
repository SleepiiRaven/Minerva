package net.minervamc.minerva.skills.greek.athena;

import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class SpearOfAthena extends Skill {

    private static final Color[] GOLD_GRADIENT = {
        Color.fromRGB(255, 215, 0),
        Color.fromRGB(255, 200, 30),
        Color.fromRGB(255, 230, 80),
        Color.fromRGB(240, 190, 0),
        Color.fromRGB(255, 255, 180),
    };

    private static final Color[] DIVINE_GRADIENT = {
        Color.fromRGB(180, 230, 255),
        Color.fromRGB(200, 240, 255),
        Color.fromRGB(255, 255, 255),
        Color.fromRGB(150, 210, 255),
        Color.fromRGB(220, 250, 255),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 8000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "spearOfAthena")) {
            onCooldown(player);
            return;
        }

        boolean empowered = player.getScoreboardTags().contains("athenaParry");
        if (empowered) {
            player.removeScoreboardTag("athenaParry");
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "spearOfAthena", cooldown);
        cooldownAlarm(player, cooldown, "Spear of Athena");

        double momentumMultiplier = TacticalAgility.detonateIfCharged(player);
        double speed = 1.2;
        double maxDistance = 30;
        double baseDamage = empowered ? 18 : 10;
        int aoeRadius = 4;

        Vector direction = player.getEyeLocation().getDirection().normalize();

        ItemDisplay display = player.getWorld().spawn(
            player.getEyeLocation().add(direction.clone().multiply(1.5)),
            ItemDisplay.class,
            entity -> {
                entity.setItemStack(new ItemStack(Material.TRIDENT));
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
            }
        );

        // Cast sounds
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
        if (empowered) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
            // Divine burst around the player on cast
            Location castLoc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, castLoc, 40, 0.5, 0.5, 0.5, 0.25);
            player.getWorld().spawnParticle(Particle.FLASH, castLoc, 1, 0, 0, 0, 0);
            for (Color c : DIVINE_GRADIENT) {
                player.getWorld().spawnParticle(Particle.DUST, castLoc, 12, 0.6, 0.6, 0.6, 0, new Particle.DustOptions(c, 2f));
            }
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.5f, 0.9f);
        }

        Color[] trailColors = empowered ? DIVINE_GRADIENT : GOLD_GRADIENT;

        new BukkitRunnable() {
            int ticks = 0;
            float angle = 0;
            double distanceTraveled = 0;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }

                display.teleport(display.getLocation().add(direction.clone().multiply(speed)));
                distanceTraveled += speed;

                // Spin the spear along its flight axis
                angle += 30;
                Matrix4f matrix = new Matrix4f().identity().rotateX((float) Math.toRadians(angle));
                display.setTransformationMatrix(matrix);
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(1);

                // Particle trail
                Location particleLoc = display.getLocation();
                particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 4, 0.1, 0.1, 0.1, 0,
                    ParticleUtils.getDustOptionsFromGradient(trailColors, 1.5f));
                if (ticks % 2 == 0) {
                    particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                if (empowered && ticks % 2 == 0) {
                    particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 6, 0.3, 0.3, 0.3, 0.6);
                }

                // Entity collision
                for (Entity entity : display.getWorld().getNearbyEntities(display.getLocation(), 0.8, 0.8, 0.8)) {
                    if (!(entity instanceof LivingEntity living) || entity == player || entity == display) continue;
                    if (entity instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, entity)) continue;

                    damage(living, baseDamage * momentumMultiplier, player, false, true);
                    stun(player, entity, 25);
                    onImpact(display.getLocation(), player, empowered, baseDamage, aoeRadius);
                    display.remove();
                    cancel();
                    return;
                }

                // Block collision
                if (display.getLocation().getBlock().isSolid()) {
                    onImpact(display.getLocation(), player, empowered, baseDamage, aoeRadius);
                    display.remove();
                    cancel();
                    return;
                }

                // Max range
                if (distanceTraveled >= maxDistance) {
                    onImpact(display.getLocation(), player, empowered, baseDamage, aoeRadius);
                    display.remove();
                    cancel();
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    private void onImpact(Location loc, Player player, boolean empowered, double baseDamage, int radius) {
        if (empowered) {
            // AoE divine explosion
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living) || entity == player) continue;
                if (entity instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                if (PlayerStats.isSummoned(player, entity)) continue;
                damage(living, baseDamage * 0.6, player, false, true);
                Vector kb = ParticleUtils.getDirection(loc, entity.getLocation()).multiply(1.8);
                knockback(entity, entity.getVelocity().add(kb));
            }

            // Divine impact visuals
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 100, 0.8, 0.8, 0.8, 0.35);
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 250, 1.2, 1.2, 1.2, 2.5);
            for (Color c : DIVINE_GRADIENT) {
                loc.getWorld().spawnParticle(Particle.DUST, loc, 35, 0.9, 0.9, 0.9, 0, new Particle.DustOptions(c, 2.5f));
            }
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 4, 0.4, 0.4, 0.4, 0);

            loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1.5f);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.8f);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
            loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.2f);
        } else {
            // Normal impact visuals
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 25, 0.3, 0.3, 0.3, 0.2);
            for (Color c : GOLD_GRADIENT) {
                loc.getWorld().spawnParticle(Particle.DUST, loc, 18, 0.4, 0.4, 0.4, 0, new Particle.DustOptions(c, 1.5f));
            }
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 25, 0.3, 0.3, 0.3, 0.5);

            loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 0.9f);
            loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 1.2f);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "spearOfAthena";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.TRIDENT)
            .setName(TextContext.formatLegacy("&lSpear of Athena", false).color(TextColor.color(255, 215, 0)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Hurl Athena's divine spear at your foes,", false),
                TextContext.formatLegacy("&7stunning those it strikes.", false),
                TextContext.formatLegacy("&eCast while in &6Parry stance &efor a", false),
                TextContext.formatLegacy("&epowerful divine explosion on impact!", false)
            )).build();
    }
}
