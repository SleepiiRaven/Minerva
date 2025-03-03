package net.minervamc.minerva.skills.greek.aphrodite;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Doves extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
    }

    public int stackDoves(Player player, int doves, long milisUntilOver) {
        int maxDoves = 10;

        doves = Math.min(doves, maxDoves);

        int ticksUntilOver = (int) (milisUntilOver / 50);

        int finalRadius = 3;
        int rotations = 3;
        double damage = 1;
        int blindnessDur = 20;
        int blindnessAmp = 5;

        doveEffect(player, finalRadius, rotations, doves, ticksUntilOver, damage, blindnessDur, blindnessAmp);

        return doves;
    }

    private void doveEffect(Player player, int finalRadius, int rotations, int feathers, int duration, double damage, int blindnessDur, int blindnessAmp) {
        new BukkitRunnable() {
            double ticks = 0;

            @Override
            public void run() {
                if (ticks > duration || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location pLoc = player.getLocation();
                double t = ticks/duration;
                double angle = Math.PI * 2 * t;
                double radius = finalRadius * Math.min((t * finalRadius), 1);

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity livingEntity) || entity == player ||
                            (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) continue;

                    damage(livingEntity, damage, player);
                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDur, blindnessAmp));
                }

                if (MirrorImage.mirrors.get(player) != null && MirrorImage.mirrors.get(player).isSpawned()) {
                    for (Entity entity : MirrorImage.mirrors.get(player).getEntity().getNearbyEntities(radius, radius, radius)) {
                        if (!(entity instanceof LivingEntity livingEntity) || entity == player ||
                                (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;

                        damage(livingEntity, damage, player);
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDur, blindnessAmp));
                    }
                }

                for (int i = 0; i < feathers; i++) {
                    double a = (rotations * angle) + (i * 2 * Math.PI / feathers);
                    double x = radius * Math.cos(a);
                    double z = radius * Math.sin(a);
                    player.getWorld().spawnParticle(Particle.CLOUD, pLoc.clone().add(new Vector(x, 1 + Math.sin(4*angle), z)), 1, 0, 0, 0, 0);
                    if (MirrorImage.mirrors.get(player) != null && MirrorImage.mirrors.get(player).isSpawned()) {
                        Entity mirror = MirrorImage.mirrors.get(player).getEntity();
                        mirror.getWorld().spawnParticle(Particle.CLOUD, mirror.getLocation().add(new Vector(x, 1, z)), 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }


    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "doves";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
