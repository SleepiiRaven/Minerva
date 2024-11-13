package net.minervamc.minerva.skills.greek.zeus;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
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

public class WindWall extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double wallWidth;
        double wallHeight;
        double distance;
        double damage;
        double maxTriggers;
        long ticksBetweenTriggers;
        long cooldown;
        int particlesCurve = 40;
        double lineLengthBetweenParticles = 0.2;

        switch (level) {
            case 2 -> {
                wallWidth = 8;
                wallHeight = 5;
                distance = 3;
                damage = 3;
                maxTriggers = 17;
                ticksBetweenTriggers = 10;
                cooldown = 14000;
            }
            case 3 -> {
                wallWidth = 10;
                wallHeight = 6;
                distance = 3;
                damage = 3.5;
                maxTriggers = 20;
                ticksBetweenTriggers = 10;
                cooldown = 13000;
            }
            case 4 -> {
                wallWidth = 12;
                wallHeight = 8;
                distance = 3;
                damage = 4;
                maxTriggers = 20;
                ticksBetweenTriggers = 10;
                cooldown = 12500;
            }
            case 5 -> {
                wallWidth = 15;
                wallHeight = 10;
                distance = 3;
                damage = 5;
                maxTriggers = 20;
                ticksBetweenTriggers = 10;
                cooldown = 12500;
            }
            default -> {
                wallWidth = 8;
                wallHeight = 5;
                distance = 3;
                damage = 1;
                maxTriggers = 15;
                ticksBetweenTriggers = 10;
                cooldown = 15000;
            }
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "windWall")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "windWall", cooldown);
        cooldownAlarm(player, cooldown, "Wind Wall");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.3f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_PLACE, 0.5f, 0.5f);

        new BukkitRunnable() {
            final Location playerLocation = player.getLocation();
            final Vector playerDirection = player.getEyeLocation().getDirection();
            final Vector horizontalDir = playerDirection.clone().setY(0).normalize();

            final Vector A = horizontalDir.clone().multiply(wallWidth/2).rotateAroundY(-90).add(playerDirection.clone().multiply(distance));
            final Vector B = playerDirection.clone().add(new Vector(0, 1, 0)).multiply(distance);
            final Vector C = horizontalDir.clone().multiply(wallWidth/2).rotateAroundY(90).add(playerDirection.clone().multiply(distance));

            final Vector heightOffset = new Vector(0, wallHeight/2, 0);

            final List<Vector> topCurve = ParticleUtils.getQuadraticBezierPoints(A.clone().add(heightOffset), B.clone().add(heightOffset), C.clone().add(heightOffset), particlesCurve);
            final List<Vector> bottomCurve = ParticleUtils.getQuadraticBezierPoints(A.clone().subtract(heightOffset), B.clone().subtract(heightOffset), C.clone().subtract(heightOffset), particlesCurve);

            int triggers = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || triggers >= maxTriggers) {
                    this.cancel();
                }

                for (int i = 0; i < particlesCurve; i++) {
                    List<Vector> line = ParticleUtils.getLinePoints(topCurve.get(i), bottomCurve.get(i), lineLengthBetweenParticles);
                    for (Vector point : line) {
                        Location pointLocation = playerLocation.clone().add(point);
                        player.getWorld().spawnParticle(Particle.CLOUD, pointLocation, 1, 0, 0, 0, 0);
                        for (Entity entity : (pointLocation).getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                                damage(livingEntity, damage, player);
                                knockback(livingEntity, livingEntity.getLocation().getDirection().setY(0).multiply(-1));
                            }
                        }
                    }
                }

                player.getWorld().playSound(playerLocation.clone().add(playerDirection.clone().multiply(distance)), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f);

                triggers++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, ticksBetweenTriggers);
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
        return "windWall";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PAPER), ChatColor.WHITE + "" + ChatColor.BOLD + "[Wind Wall]", ChatColor.GRAY + "Raise a wall of wind that pushes", ChatColor.GRAY + "back and damages any enemies", ChatColor.GRAY + "that enter into the wall.");
    }
}
