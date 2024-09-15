package net.minervamc.minerva.skills.greek.ares;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrimalScream extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double distance = 10;

        // use red dust particles and ash
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1f);
        List<Vector> unitCircle = ParticleUtils.getVerticalCirclePoints(1, player.getPitch(), player.getYaw(), 20);
        for (Vector vector : unitCircle) {
            Vector inverse = vector.clone().normalize().multiply(-1);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5)).add(vector), 0, inverse.getX(), inverse.getY(), inverse.getZ(), 0.1);
            player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5).add(vector)), 0, inverse.getX(), inverse.getY(), inverse.getZ(), 0.1);
        }

        Location loc = player.getEyeLocation();
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);

                for (int i = 0; i < distance/4; i++) {
                    
                }

                for (Vector particleOffset : spiral) {
                    player.getWorld().spawnParticle(Particle.DUST, loc.add(particleOffset), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GRAY, 1));
                    player.getWorld().spawnParticle(Particle.DUST, loc.add(particleOffset), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1));
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
