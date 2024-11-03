package net.minervamc.minerva.skills.greek.ares;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import net.minervamc.minerva.utils.SkillUtils;
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
import org.bukkit.util.Vector;

public class PrimalScream extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double distance = 10;
        double kb = 1.5;
        int effAmp = 1;
        int effTime = 40;
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "primalScream")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "primalScream", cooldown);
        cooldownAlarm(player, cooldown, "Primal Scream");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1f);

        List<Vector> circle = ParticleUtils.getVerticalCirclePoints(1, player.getPitch(), player.getYaw(), 30);

        new BukkitRunnable() {
            int ticks = 0;
            Vector lDir;
            Location lLoc;

            @Override
            public void run() {
                Vector direction = player.getEyeLocation().getDirection();
                Location loc = player.getEyeLocation().add(direction);


                if (ticks == 20) {
                    player.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);
                    player.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
                    lDir = direction;
                    lLoc = loc;
                }
                if (ticks >= 20 + distance) {
                    this.cancel();
                    return;
                } else if (ticks >= 20) {
                    int time = (ticks - 20);
                    Location currLoc = lLoc.clone().add(lDir.clone().multiply(time));

                    for (Entity entity : player.getWorld().getNearbyEntities(currLoc, 1, 1, 1)) {
                        if (!(entity instanceof LivingEntity livingMonster) || entity.getScoreboardTags().contains(player.getUniqueId().toString()) || (entity == player) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;
                        livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, effTime, effAmp));
                        livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, effTime, effAmp));
                        Vector viewNormalized = (ParticleUtils.getDirection(loc, livingMonster.getLocation()).clone().normalize()).multiply(kb);
                        livingMonster.setVelocity(viewNormalized);
                    }

                    for (Vector vector : circle) {
                        Vector inverse = vector.clone().normalize().multiply(0.2 + time / (distance / 2.5));
                        player.getWorld().spawnParticle(Particle.DUST, currLoc.clone().add(inverse), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(75, 63, 61), 2f));
                        player.getWorld().spawnParticle(Particle.DUST, currLoc.clone().add(inverse), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(97, 9, 26), 2f));
                        player.getWorld().spawnParticle(Particle.DUST, currLoc.clone().add(inverse), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1f));
                    }
                } else {
                    for (Vector vector : circle) {
                        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(vector.clone().multiply((3 - (3 * ticks / 20f)))), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GRAY, 1f));
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
        return "primalScream";
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Material.VINE);
    }
}
