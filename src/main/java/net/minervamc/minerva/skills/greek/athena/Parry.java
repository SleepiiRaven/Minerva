package net.minervamc.minerva.skills.greek.athena;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.Random;
import org.bukkit.Particle;

import java.util.List;

public class Parry extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        Vector A1 = new Vector(0, 1.4, 0);
        Vector A2 = new Vector(0, 1, 0);
        Vector A3 = new Vector(-1, 1, 0);
        Vector B1 = new Vector(0, 1.4, 0);
        Vector B2 = new Vector(0, 1, 0);
        Vector B3 = new Vector(1, 1, 0);
        Vector C1 = new Vector(0, -1, 0);
        Vector C2 = new Vector(1, -0.4, 0);
        Vector C3 = new Vector(1, 1, 0);
        Vector D1 = new Vector(0, -1, 0);
        Vector D2 = new Vector(-1, -0.4, 0);
        Vector D3 = new Vector(-1, 1, 0);

        Color[] gradient = {
                Color.fromRGB(0, 0, 0),
                Color.fromRGB(16, 2, 1),
                Color.fromRGB(26, 5, 3),
                Color.fromRGB(33, 7, 4),
                Color.fromRGB(40, 9, 6),
                Color.fromRGB(47, 10, 8),
                Color.fromRGB(54, 8, 9),
                Color.fromRGB(61, 6, 11),
                Color.fromRGB(69, 4, 13),
                Color.fromRGB(77, 0, 14),
                Color.fromRGB(77, 0, 14)
        };

        List<Vector> shieldPoints = ParticleUtils.getQuadraticBezierPoints(A1, A2, A3, 10);
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(B1, B2, B3, 10));
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(C1, C2, C3, 10));
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(D1, D2, D3, 10));

        Random random = new Random();

        Location pLoc = player.getLocation();

        for (Vector point : shieldPoints) {
            int colorIndex = random.nextInt(0, 10);
            Color color = gradient[colorIndex];

            Vector rotatedPoint = ParticleUtils.rotatePitchYawFromXY(point, player.getPitch(), Math.clamp(player.getYaw(), -45, 45));

            Location particleLocation = pLoc.add(rotatedPoint);

            player.getWorld().spawnParticle(Particle.DUST, particleLocation, 0, 0, 0, 0, 0, new Particle.DustOptions(color, 1f));
        }

        player.addScoreboardTag("athenaParry");

        // make the player go into a state so when they click, THEN they parry.

        int duration = 20;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                if (player.getScoreboardTags().contains("athenaParrySuccess")) {
                    player.removeScoreboardTag("athenaParry");
                    player.removeScoreboardTag("athenaParrySuccess");
                    cleave(ticks);
                    cancel();
                    return;
                }

                ticks += 1;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void cleave(int ticks) {

    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
