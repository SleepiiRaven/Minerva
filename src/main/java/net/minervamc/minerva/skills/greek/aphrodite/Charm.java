package net.minervamc.minerva.skills.greek.aphrodite;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Charm extends Skill {
    Color[] gradient = {
            Color.fromRGB(	255,194,205),
            Color.fromRGB(	255,147,172),
            Color.fromRGB(255,98,137),
            Color.fromRGB(	252,52,104),
            Color.fromRGB(	255,8,74)
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;
        int duration = 20;
        double distanceStart = 1.5;
        double initRadius = 0.5;
        double distanceBetweenBeams = 2.5;
        double radiusIncrease = 0.3;
        long ticksBetweenBeams = 2L;
        int beams = 5;
        int charmDur = 60;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "charm")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "charm", cooldown);
        cooldownAlarm(player, cooldown, "Charm");

        Skill.stack(player, "doves", 1, "Doves", 6000);

        player.getWorld().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 1.25f);
        player.getWorld().playSound(player, Sound.BLOCK_CHISELED_BOOKSHELF_INSERT_ENCHANTED, 2f, 1f);
        player.getWorld().playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.77f);
        player.getWorld().playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 1f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > duration) {
                    beam(player, initRadius, distanceBetweenBeams, radiusIncrease, ticksBetweenBeams, beams, distanceStart, charmDur);
                    this.cancel();
                    return;
                }

                double t = (double) ticks/duration;
                double angle = 2 * Math.PI * t;
                double x = initRadius * Math.pow(Math.sin(angle), 3);
                double y = (initRadius / 16) * ((13 * Math.cos(angle)) - (5 * Math.cos(2*angle)) - (2 * Math.cos(3*angle)) - Math.cos(4*angle));
                float pitch = player.getPitch();
                float yaw = player.getYaw();

                // because we wrote the heart in the x and y axes,
                // with this function we can rotate the effect based
                // on where the player is facing
                Vector point = ParticleUtils.rotatePitchYawFromXY(new Vector(x, y, 0), pitch, yaw);

                point.add(player.getEyeLocation().getDirection().multiply(distanceStart));

                player.getWorld().spawnParticle(
                        Particle.DUST, player.getEyeLocation().clone().add(point), 0, 0, 0, 0, 0,
                        ParticleUtils.getDustOptionsFromGradient(gradient, 1f)
                );

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void beam(Player player, double initRadius, double distanceBetweenBeams, double radiusIncrease, long ticksBetweenBeams, int beams, double distanceStart, int charmDur) {
        List<Entity> entitiesCharmed = new ArrayList<>();

        player.getWorld().playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 2f);
        player.getWorld().playSound(player, Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1.7f);
        player.getWorld().playSound(player, Sound.ENTITY_SHULKER_BULLET_HURT, 2f, 1f);
        player.getWorld().playSound(player, Sound.ENTITY_ELDER_GUARDIAN_HURT, 2f, 0.86f);

        new BukkitRunnable() {
            final float pitch = player.getPitch();
            final float yaw = player.getYaw();
            final Location playerLoc = player.getEyeLocation();

            int ticks = 0;

            @Override
            public void run() {
                if (ticks > beams) {
                    this.cancel();
                    return;
                }

                double radius = initRadius + (radiusIncrease * (ticks + 1));
                double distance = distanceStart + (distanceBetweenBeams * (ticks));

                for (double t = 0; t <= 1; t += 1.0/20.0) {
                    double angle = 2 * Math.PI * t;
                    double x = radius * Math.pow(Math.sin(angle), 3);
                    double y = (radius / 16) * ((13 * Math.cos(angle)) - (5 * Math.cos(2 * angle)) - (2 * Math.cos(3 * angle)) - Math.cos(4 * angle));


                    // because we wrote the heart in the x and y axes,
                    // with this function we can rotate the effect based
                    // on where the player is facing
                    Vector point = ParticleUtils.rotatePitchYawFromXY(new Vector(x, y, 0), pitch, yaw);

                    point.add(playerLoc.getDirection().clone().multiply(distance));

                    Location particleLoc = playerLoc.clone().add(point);

                    player.getWorld().spawnParticle(
                            Particle.DUST, particleLoc, 0, 0, 0, 0, 0,
                            ParticleUtils.getDustOptionsFromGradient(gradient, ticks+1)
                    );

                    for (Entity entity : player.getWorld().getNearbyEntities(particleLoc, 1, 2, 1)) {
                        if (!(entity instanceof LivingEntity livingEntity) || entity == player ||
                                (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)) ||
                                entitiesCharmed.contains(entity) || entity.getScoreboardTags().contains("charmedHolder"))
                            continue;

                        entitiesCharmed.add(entity);

                        if (livingEntity instanceof Player playerAff) {
                            playerAff.playSound(playerAff, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 2f);
                            playerAff.playSound(playerAff, Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1.7f);
                            playerAff.playSound(playerAff, Sound.ENTITY_SHULKER_BULLET_HURT, 2f, 1f);
                            playerAff.playSound(playerAff, Sound.ENTITY_ELDER_GUARDIAN_HURT, 2f, 0.86f);
                        }
                        charm(player, livingEntity, charmDur);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, ticksBetweenBeams);
    }

    private void charm(Player charmer, LivingEntity charmed, int duration) {
        Location charmedLoc = charmed.getLocation().clone();

        Pig pig = (Pig) charmed.getWorld().spawnEntity(charmedLoc, EntityType.PIG);
        pig.setInvisible(true);
        pig.setInvulnerable(true);
        //pig.setAI(false);
        pig.addScoreboardTag("charmedHolder");
        charmed.addScoreboardTag("charmed");
        charmed.addScoreboardTag(charmer.getUniqueId().toString());

        //NMS.setNavigationTarget(pig, charmer, 1f);
        int finalDuration = duration / 2;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= finalDuration) {
                    pig.removePassenger(charmed);
                    charmed.removeScoreboardTag("charmed");
                    pig.remove();
                    this.cancel();
                    return;
                }

                if (charmed.isDead() || (charmed instanceof Player && (!((Player) charmed).isOnline()))) {
                    charmed.removeScoreboardTag("charmed");
                    pig.remove();
                    this.cancel();
                    return;
                }

                ((CraftPig) pig).getHandle().setSilent(true);
                pig.getPathfinder().moveTo(charmer);
                pig.addPassenger(charmed);

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 2L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "charm";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
