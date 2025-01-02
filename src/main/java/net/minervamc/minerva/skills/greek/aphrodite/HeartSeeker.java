package net.minervamc.minerva.skills.greek.aphrodite;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HeartSeeker extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 5000;
        double speed = 0.6;
        double damage = 5;
        Color[] gradient = {
                ParticleUtils.colorFromHex("#ffccdd"),
                ParticleUtils.colorFromHex("#ff99bb"),
                ParticleUtils.colorFromHex("#ff6699"),
                ParticleUtils.colorFromHex("#ff3377"),
                ParticleUtils.colorFromHex("#ff0055")
        };

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "heartSeeker")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "heartSeeker", cooldown);
        cooldownAlarm(player, cooldown, "Heart Seeker");

        Location loc = player.getEyeLocation();

        player.getWorld().playSound(player, Sound.ITEM_TRIDENT_RETURN, 1f, 2f);
        player.getWorld().playSound(player, Sound.BLOCK_FLOWERING_AZALEA_BREAK, 2f, 0.5f);
        player.getWorld().playSound(player, Sound.ENTITY_ALLAY_HURT, 1f, 2f);

        new BukkitRunnable() {
            LivingEntity target = null;
            Location currloc = loc.clone();
            Vector direction = currloc.clone().getDirection().normalize().multiply(speed);
            int ticks = 0;

            @Override
            public void run() {
                if (target == null || currloc.distance(target.getLocation()) > 100 || currloc.getWorld() != target.getWorld() || target.isDead()) {
                    target = null;
                    for (Entity entity : currloc.getNearbyEntities(10, 10, 10)) {
                        if (!(entity instanceof LivingEntity livingEntity) || entity == player || PlayerStats.getStats(player.getUniqueId()).getSummoned().get(player).contains(livingEntity) ||
                                (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;

                        if (player.hasLineOfSight(livingEntity) || livingEntity.hasLineOfSight(currloc)) {
                            target = livingEntity;
                            break;
                        }
                    }
                }

                if (target != null && target.isValid()) {
                    Vector to_target = ParticleUtils.getDirection(currloc, target.getEyeLocation()).multiply(speed);
                    direction.add(to_target.subtract(direction).multiply(0.2)).normalize().multiply(speed); // smooth transition
                }

                currloc.add(direction);
                currloc.setDirection(direction.clone().normalize());

                double theta = ticks * Math.PI / 25;
                double x1 = Math.cos(theta);
                double y1 = Math.sin(theta);

                Vector point1 = ParticleUtils.rotatePitchYawFromXY(new Vector(x1, y1, 0), currloc.getPitch(), currloc.getYaw());

                double x2 = -Math.cos(theta);
                double y2 = -Math.sin(theta);

                Vector point2 = ParticleUtils.rotatePitchYawFromXY(new Vector(x2, y2, 0), currloc.getPitch(), currloc.getYaw());


                currloc.getWorld().spawnParticle(Particle.DUST, currloc.clone().add(point1), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1f));
                currloc.getWorld().spawnParticle(Particle.DUST, currloc.clone().add(point2), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1f));

                if (currloc.getBlock().getType().isSolid()) {
                    this.cancel();
                    return;
                }

                currloc.getWorld().spawnParticle(Particle.DUST, currloc, 0, 0, 0, 0, 0, ParticleUtils.getDustOptionsFromGradient(gradient, 2f));

                for (Entity entity : currloc.getNearbyEntities(0.5, 0.5, 0.5)) {
                    if (entity == target) {
                        player.getWorld().playSound(currloc, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.5f);
                        player.getWorld().playSound(currloc, Sound.ENTITY_PAINTING_PLACE, 1.8f, 0.9f);
                        player.getWorld().playSound(currloc, Sound.ENTITY_ALLAY_DEATH, 0.8f, 1.5f);
                        this.cancel();
                    }

                    if (!(entity instanceof LivingEntity livingEntity) || entity == player ||
                            (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                        continue;

                    damage(livingEntity, damage, player);
                }

                ticks++;
                if (ticks > 100) {
                    this.cancel();
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "heartSeeker";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
