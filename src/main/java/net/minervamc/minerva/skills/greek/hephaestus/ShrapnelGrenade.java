package net.minervamc.minerva.skills.greek.hephaestus;

import java.util.List;
import java.util.Random;
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

public class ShrapnelGrenade extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double damage = 4;
        double speed = 30;
        int rotationSpeed = 2; // ticks per rotation
        double baseKnockback = 1.5;
        double baseKnockbackForge = 3.0;
        double airKnockbackMultiplier = 1.8;
        double knockbackFalloff = 0.5;

        long cooldown = 9000;
        double distance = 10;
        int radius = 3;
        double damageForge = 14;
        Color[] colors = {
              Color.fromRGB(223,184,119),
              Color.fromRGB(231,165,58),
              Color.fromRGB(207,128,0),
              Color.fromRGB(164,107,15),
              Color.fromRGB(122,82,19)
        };

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "shrapnelGrenade")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "shrapnelGrenade", cooldown);
        cooldownAlarm(player, cooldown, "Shrapnel Grenade");

        ItemDisplay display = player.getWorld().spawn(player.getEyeLocation().subtract(0, 0.5, 0), ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(Material.FIREWORK_STAR));
        });

        final Vector direction = player.getEyeLocation().getDirection();
        List<Vector> curve = ParticleUtils.getQuadraticBezierPoints(new Vector(0, 0, 0), direction.clone().multiply(distance).add(new Vector(0, 2, 0)), direction.clone().multiply(distance).add(new Vector(0, -3, 0)), speed);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
        new BukkitRunnable() {
            int ticks = 0;
            float angle = 0; // Start angle for rotation
            final Location loc = display.getLocation();

            @Override
            public void run() {
                if (display.getLocation().getBlock().isSolid() || !display.isValid()) {
                    for (Entity damageEntity : display.getWorld().getNearbyEntities(display.getLocation(), radius, radius, radius)) {
                        if (!(damageEntity instanceof LivingEntity livingMonster) || (damageEntity == display) || (damageEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;

                        double dmgCurr = (livingMonster == player) ? damage/4 : damage;
                        damage(livingMonster, dmgCurr, player);
                        double distance = display.getLocation().distance(livingMonster.getLocation());

                        Vector knockbackForce = ParticleUtils.getDirection(display.getLocation(), livingMonster.getLocation())
                                .normalize()
                                .multiply(baseKnockback)
                                .multiply(livingMonster.isOnGround() ? 1 : airKnockbackMultiplier)
                                .divide(new Vector(Math.pow(distance + knockbackFalloff, 2), Math.pow(distance + knockbackFalloff, 2), Math.pow(distance + knockbackFalloff, 2)));


                        knockback(livingMonster, livingMonster.getVelocity().add(knockbackForce));

                        if (PlayerStats.isSummoned(player, damageEntity) && damageEntity.getScoreboardTags().contains("livingForge")) {
                            livingForgeExplosion(damageForge, baseKnockbackForge, airKnockbackMultiplier, knockbackFalloff, player, damageEntity, radius * 2);
                            damageEntity.remove();
                        }
                    }

                    display.getWorld().spawnParticle(Particle.LARGE_SMOKE, display.getLocation(), 50, 0, 0, 0, 0.2);
                    display.getWorld().spawnParticle(Particle.LAVA, display.getLocation(), 50, 0, 0, 0, 0.3);
                    display.getWorld().spawnParticle(Particle.EXPLOSION, display.getLocation(), 1, 0, 0, 0, 0.1);
                    display.getWorld().spawnParticle(Particle.FIREWORK, display.getLocation(), 50, 0, 0, 0, 0.2);
                    display.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, display.getLocation(), 100, 0, 0, 0, 0.2);
                    display.getWorld().playSound(display.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.5f);
                    display.getWorld().playSound(display.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
                    display.getWorld().playSound(display.getLocation(), Sound.ENTITY_PUFFER_FISH_STING, 1f, 1f);
                    display.remove();
                    this.cancel();
                }

                if (ticks % rotationSpeed == 0) {
                    Matrix4f matrix = new Matrix4f().identity();

                    angle += 45;
                    display.setTransformationMatrix(matrix.rotateX(((float) Math.toRadians(angle)) + 0.1F));
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(rotationSpeed);
                }

                // Movement
                if (ticks >= curve.size()) {
                    display.teleport(display.getLocation().add(new Vector(0, -1, 0).add(loc.getDirection().multiply(0.2)).multiply(distance/speed)));
                } else {

                    display.teleport(loc.clone().add(curve.get(ticks)));
                }


                for (Entity entity : display.getWorld().getNearbyEntities(display.getLocation(), 0.1, 0.1, 0.1)) {
                    if (!(entity instanceof LivingEntity) || entity == player || (entity == display) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                        continue;

                    for (Entity damageEntity : display.getWorld().getNearbyEntities(display.getLocation(), radius, radius, radius)) {
                        if (!(damageEntity instanceof LivingEntity livingMonster) || (damageEntity == display) || (damageEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;

                        double dmgCurr = (livingMonster == player) ? damage/4 : damage;
                        damage(livingMonster, dmgCurr, player);
                        double distance = display.getLocation().distance(livingMonster.getLocation());

                        Vector knockbackForce = ParticleUtils.getDirection(display.getLocation(), livingMonster.getLocation())
                                .normalize()
                                .multiply(baseKnockback)
                                .multiply(livingMonster.isOnGround() ? 1 : airKnockbackMultiplier)
                                .divide(new Vector(Math.pow(distance + knockbackFalloff, 2), Math.pow(distance + knockbackFalloff, 2), Math.pow(distance + knockbackFalloff, 2)));


                        knockback(livingMonster, livingMonster.getVelocity().add(knockbackForce));
                        //knockback(livingMonster, ParticleUtils.getDirection(display.getLocation(), livingMonster.getLocation()).normalize().divide(new Vector(distance, distance, distance)).multiply(kb));

                        if (PlayerStats.isSummoned(player, damageEntity) && damageEntity.getScoreboardTags().contains("livingForge")) {
                            livingForgeExplosion(damageForge, baseKnockbackForge, airKnockbackMultiplier, knockbackFalloff, player, damageEntity, radius * 2);
                            damageEntity.remove();
                        }
                    }

                    display.getWorld().spawnParticle(Particle.LARGE_SMOKE, display.getLocation(), 50, 0, 0, 0, 0.2);
                    display.getWorld().spawnParticle(Particle.LAVA, display.getLocation(), 50, 0, 0, 0, 0.3);
                    display.getWorld().spawnParticle(Particle.EXPLOSION, display.getLocation(), 1, 0, 0, 0, 0.1);
                    display.getWorld().spawnParticle(Particle.FIREWORK, display.getLocation(), 50, 0, 0, 0, 0.2);
                    display.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, display.getLocation(), 100, 0, 0, 0, 0.2);
                    display.getWorld().playSound(display.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.5f);
                    display.getWorld().playSound(display.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
                    display.getWorld().playSound(display.getLocation(), Sound.ENTITY_PUFFER_FISH_STING, 1f, 1f);
                    display.remove();
                    this.cancel();
                }
                int randInt = new Random().nextInt(0, 4);
                display.getWorld().spawnParticle(Particle.DUST, display.getLocation(), 0, 0, 0, 0, 0, new Particle.DustOptions(colors[randInt], 1f));
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 2L, 1L);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.7f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_BREAK, 1f, 0.9f);
    }

    private void livingForgeExplosion(double damage, double baseKnockback, double airKnockbackMultiplier, double knockbackFalloff, Player player, Entity entity, int radius) {
        for (Entity damageEntity : entity.getWorld().getNearbyEntities(entity.getLocation(), radius, radius, radius)) {
            if (!(damageEntity instanceof LivingEntity livingMonster) || (damageEntity == entity) || (damageEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                continue;

            double dmgCurr = (livingMonster == player) ? damage/4 : damage;
            damage(livingMonster, dmgCurr, player);
            double distance = entity.getLocation().distance(livingMonster.getLocation());
            Vector knockbackForce = ParticleUtils.getDirection(entity.getLocation(), livingMonster.getLocation())
                    .normalize()
                    .multiply(baseKnockback)
                    .multiply(livingMonster.isOnGround() ? 1 : airKnockbackMultiplier)
                    .divide(new Vector(Math.pow(distance + knockbackFalloff, 2), Math.pow(distance + knockbackFalloff, 2), Math.pow(distance + knockbackFalloff, 2)));
            knockback(livingMonster, livingMonster.getVelocity().add(knockbackForce));
        }

        entity.getWorld().spawnParticle(Particle.LARGE_SMOKE, entity.getLocation(), 100, 0, 0, 0, 0.4);
        entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation(), 100, 0, 0, 0, 0.6);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 2, 0, 0, 0, 0.2);
        entity.getWorld().spawnParticle(Particle.FIREWORK, entity.getLocation(), 100, 0, 0, 0, 0.4);
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 200, 0, 0, 0, 0.4);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "shrapnelGrenade";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.FIREWORK_STAR)
                .setName(TextContext.formatLegacy("&lShrapnel Grenade", false).color(TextColor.color(160,82,45)))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Shoot a grenade that,", false),
                        TextContext.formatLegacy("&7upon landing, explodes.", false),
                        TextContext.formatLegacy("&7Entities hit in the explosion", false),
                        TextContext.formatLegacy("&7will suffer damage and a", false),
                        TextContext.formatLegacy("&7large amount of knockback.", false),
                        TextContext.formatLegacy("&7This could be used as a", false),
                        TextContext.formatLegacy("&7movement ability, given", false),
                        TextContext.formatLegacy("&7sharp aim and great timing.", false)
                )).build();
    }
}
