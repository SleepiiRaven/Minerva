package net.minervamc.minerva.skills.greek.hades;

import java.util.Collection;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class ChannelingOfTartarus extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        double distance;
        double range;
        int burstDistance;
        double burstRadius; // Radius of giant burst cyllander
        int burstHeight; // Goes down half the height and up half the height
        double missileDamage;
        double burstDamage;
        long timeBetweenMissiles;
        int burstWindUpTime;

        switch (level) {
            case 2 -> {
                cooldown = 11000;
                distance = 6;
                range = 1.5;
                burstDistance = 30;
                burstRadius = 4; // Radius of giant burst cyllander
                burstHeight = 20; // Goes down half the height and up half the height
                missileDamage = 1.5;
                burstDamage = 17;
                timeBetweenMissiles = 10;
                burstWindUpTime = 30;
            }
            case 3 -> {
                cooldown = 11000;
                distance = 7;
                range = 1.5;
                burstDistance = 30;
                burstRadius = 4.5; // Radius of giant burst cyllander
                burstHeight = 20; // Goes down half the height and up half the height
                missileDamage = 2;
                burstDamage = 20;
                timeBetweenMissiles = 10;
                burstWindUpTime = 30;
            }
            case 4 -> {
                cooldown = 11000;
                distance = 8;
                range = 1.5;
                burstDistance = 30;
                burstRadius = 4.5; // Radius of giant burst cyllander
                burstHeight = 20; // Goes down half the height and up half the height
                missileDamage = 2.25;
                burstDamage = 20;
                timeBetweenMissiles = 8;
                burstWindUpTime = 30;
            }
            case 5 -> {
                cooldown = 10000;
                distance = 10;
                range = 1.5;
                burstDistance = 30;
                burstRadius = 5; // Radius of giant burst cyllander
                burstHeight = 20; // Goes down half the height and up half the height
                missileDamage = 2.5;
                burstDamage = 22.5;
                timeBetweenMissiles = 5;
                burstWindUpTime = 30;
            }
            default -> {
                cooldown = 12000;
                distance = 5;
                range = 1.5;
                burstDistance = 30;
                burstRadius = 3; // Radius of giant burst cyllander
                burstHeight = 20; // Goes down half the height and up half the height
                missileDamage = 6;
                burstDamage = 20;
                timeBetweenMissiles = 20;
                burstWindUpTime = 30;
            }
        }

        double finalDistance = distance;
        double finalMissileDamage = missileDamage;
        double finalRange = range;
        int finalBurstDistance = burstDistance;
        double finalBurstDamage = burstDamage;
        int finalBurstHeight = burstHeight;
        double finalBurstRadius = burstRadius;
        int finalBurstWindUpTime = burstWindUpTime;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "channelingOfTartarus")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "channelingOfTartarus", cooldown);
        cooldownAlarm(player, cooldown, "Channeling of Tartarus");
        enum Direction {
            LEFT,
            RIGHT,
            UP
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "channelingOfTartarusCasting", ((timeBetweenMissiles * 3 + burstWindUpTime) * 50));

        new BukkitRunnable() {
            int hitEnemies = 0;
            Direction dir = Direction.LEFT;

            @Override
            public void run() {
                Vector beamStart;
                Vector beamControl;
                Vector beamEnd;
                Vector viewDir = player.getEyeLocation().getDirection();

                if (player.isDead() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                switch (dir) {
                    case LEFT:
                        beamStart = ParticleUtils.rotateYAxis(viewDir, -90);
                        beamControl = beamStart.clone().multiply(3).add(viewDir.clone().multiply(1.5));
                        beamEnd = viewDir.clone().multiply(finalDistance);
                        if (beam(beamStart, beamControl, beamEnd, player, finalMissileDamage, finalRange)) hitEnemies++;
                        dir = Direction.RIGHT;
                        break;
                    case RIGHT:
                        beamStart = ParticleUtils.rotateYAxis(viewDir, 90);
                        beamControl = beamStart.clone().multiply(3).add(viewDir.clone().multiply(1.5));
                        beamEnd = viewDir.clone().multiply(finalDistance);
                        if (beam(beamStart, beamControl, beamEnd, player, finalMissileDamage, finalRange)) hitEnemies++;
                        dir = Direction.UP;
                        break;
                    case UP:
                        beamStart = new Vector(0, 1, 0);
                        beamControl = beamStart.clone().multiply(3).add(viewDir.clone().multiply(1.5));
                        beamEnd = viewDir.clone().multiply(finalDistance);
                        if (beam(beamStart, beamControl, beamEnd, player, finalMissileDamage, finalRange)) hitEnemies++;
                        if (hitEnemies == 3) {
                            blast(player, finalBurstDistance, finalBurstDamage, finalBurstHeight, finalBurstRadius, finalBurstWindUpTime);
                        } else player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                        this.cancel();
                        break;
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, timeBetweenMissiles);
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

    private boolean beam(Vector start, Vector control, Vector end, Player player, double damage, double range) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.5f, 0.3f);
        boolean foundEnemy = false;
        for (Vector vector : ParticleUtils.getQuadraticBezierPoints(start, control, end, 10)) {
            Location particleLoc = player.getEyeLocation().add(vector);
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 10, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(32, 32, 40), 2));
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 10, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(54, 55, 66), 2));
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(102, 235, 237), 2));
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(74, 149, 150), 2));
            Collection<Entity> closebyMonsters = particleLoc.getWorld().getNearbyEntities(particleLoc, range, range, range);
            for (Entity closebyMonster : closebyMonsters) {
                // make sure it's a living entity, not an armor stand or something, continue skips the current loop
                if (!(closebyMonster instanceof LivingEntity livingMonster) || (closebyMonster == player)) continue;
                // Get the entity's collision box
                BoundingBox monsterBoundingBox = livingMonster.getBoundingBox();
                BoundingBox collisionBox = BoundingBox.of(particleLoc, range, range, range);
                if (!(monsterBoundingBox.overlaps(collisionBox))) continue;
                SkillUtils.damage(livingMonster, damage, player);
                livingMonster.getLocation().getWorld().playSound(livingMonster.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1f, 0.5f);
                livingMonster.getWorld().spawnParticle(Particle.SOUL, livingMonster.getLocation(), 2, 0, 0, 0);
                foundEnemy = true;
            }
        }
        if (foundEnemy) {
            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 0.5f);
        }

        return foundEnemy;
    }

    private void blast(Player player, int range, double damage, int height, double radius, int time) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.2f, 2f);
        player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 2f, 2f);
        player.sendMessage(ChatColor.RED + "You have hit all three of your missiles. Aim at a block within " + range + " blocks of you!");
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, time, 1));
        new BukkitRunnable() {

            @Override
            public void run() {
                RayTraceResult resultBlocks = player.rayTraceBlocks(range);
                RayTraceResult resultEntities = player.getWorld().rayTraceEntities(player.getLocation(), player.getLocation().getDirection(), range);
                Location effectLocation;
                if (resultBlocks != null && resultBlocks.getHitBlock() != null && !player.isDead() && player.isOnline()) {
                    try {
                        effectLocation = resultBlocks.getHitBlock().getLocation();
                    } catch (Exception e) {
                        effectLocation = null;
                    }
                } else if (resultEntities != null && resultEntities.getHitEntity() != null && !player.isDead() && player.isOnline()) {
                    try {
                        effectLocation = resultEntities.getHitEntity().getLocation();
                    } catch (Exception e) {
                        effectLocation = null;
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You were not looking at a block or an entity within " + range + " blocks of you.");
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.2f, 2f);
                    this.cancel();
                    return;
                }
                if (effectLocation == null) {
                    this.cancel();
                    run();

                }
                Collection<Entity> closebyEntities = effectLocation.getWorld().getNearbyEntities(effectLocation, 3, height, 3);
                for (Vector point : ParticleUtils.getCylinderPoints(radius, height)) {
                    Location particleLoc = effectLocation.clone().add(point);
                    particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(102, 235, 237), 2));
                    particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(74, 149, 150), 2));
                }
                for (Entity entity : closebyEntities) {
                    if (entity instanceof LivingEntity enemy && entity != player && !(enemy instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                        SkillUtils.damage(enemy, damage, player);
                    }
                }

            }

        }.runTaskLater(Minerva.getInstance(), time);
    }

    @Override
    public String toString() {
        return "channelingOfTartarus";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SOUL_LANTERN), ChatColor.AQUA + "" + ChatColor.BOLD + "[Channeling of Tartarus]", ChatColor.GRAY + "Shoot three souls at an enemy. If the souls hit the enemy,", ChatColor.GRAY + "you will levitate while the souls will descend into Tartarus and", ChatColor.GRAY + "unleash its wrath upon wherever you choose.");
    }
}
