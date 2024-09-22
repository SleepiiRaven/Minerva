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
        double kb = 1;
        int effAmp = 1;
        int effTime = 4;


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1f);

        List<Vector> unitCircle = ParticleUtils.getVerticalCirclePoints(3, player.getPitch(), player.getYaw(), 20);
        List<Vector> circle = ParticleUtils.getVerticalCirclePoints(0.1, player.getPitch(), player.getYaw(), 20);
        Vector direction = player.getEyeLocation().getDirection();
        Location loc = player.getEyeLocation().add(direction);

        for (Vector vector : unitCircle) {
            Vector inverse = vector.clone().normalize().multiply(-1);
            player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(vector), 0, inverse.getX(), inverse.getY(), inverse.getZ(), 0.25);
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
                for (double i = 0; i < distance; i++) {
                    Location currLoc = loc.clone().add(direction.clone().multiply(i));

                    for (Entity entity : player.getWorld().getNearbyEntities(currLoc, 1, 1, 1)) {
                        if (!(entity instanceof LivingEntity livingMonster) || (entity.getScoreboardTags().contains("aresSummoned") || (entity == player) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))))
                            continue;
                        livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, effTime, effAmp));
                        livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, effTime, effAmp));
                        Vector viewNormalized = (ParticleUtils.getDirection(player.getLocation(), livingMonster.getLocation()).clone().normalize()).multiply(kb);
                        livingMonster.setVelocity(viewNormalized);
                    }

                    for (Vector vector : circle) {
                        Vector inverse = vector.clone().normalize().multiply(0.2 + i/(distance/2.5));
                        player.getWorld().spawnParticle(Particle.DUST, currLoc.clone().add(inverse), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(75, 63, 61), 2f));
                        player.getWorld().spawnParticle(Particle.DUST, currLoc.clone().add(inverse), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(97, 9, 26), 2f));
                        player.getWorld().spawnParticle(Particle.DUST, currLoc.clone().add(inverse), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1f));
                    }
                }
            }
        }.runTaskLater(Minerva.getInstance(), 20L);
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
