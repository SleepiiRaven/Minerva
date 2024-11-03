package net.minervamc.minerva.skills.greek.zeus;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import net.minervamc.minerva.utils.SkillUtils;
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
            final Vector directionLeft = ParticleUtils.rotateYAxis(playerDirection, -90);
            final Vector wallStart = (directionLeft.clone().multiply(wallWidth / 2)).add(playerDirection.clone().multiply(distance));
            final Vector directionRight = ParticleUtils.rotateYAxis(playerDirection, 90);
            int triggers = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || triggers >= maxTriggers) {
                    this.cancel();
                }

                for (double i = 0; i < wallWidth; i += 0.2) {
                    for (double j = 0; j < wallHeight; j += 0.2) {
                        Vector point = (wallStart.clone().add(directionRight.clone().multiply(i))).add(new Vector(0, j, 0));
                        Location pointLocation = playerLocation.clone().add(point);
                        player.getWorld().spawnParticle(Particle.CLOUD, pointLocation, 1, 0, 0, 0, 0);
                        for (Entity entity : (pointLocation).getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                                SkillUtils.damage(livingEntity, damage, player);
                                livingEntity.setVelocity(livingEntity.getLocation().getDirection().setY(0).multiply(-1));
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
        return ItemUtils.getItem(new ItemStack(Material.PAPER), ChatColor.WHITE + "" + ChatColor.BOLD + "[Wind Wall]", ChatColor.GRAY + "Raise a wall of wind that pushes back and damages any enemies that enter into the wall.");
    }
}
