package net.minervamc.minerva.skills.greek.hermes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
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

public class TalariaStep extends Skill {

    private static final Color[] WING_COLORS = {
        Color.fromRGB(255, 252, 220),
        Color.fromRGB(255, 223, 105),
        Color.fromRGB(255, 178, 55),
        Color.fromRGB(130, 235, 255)
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 4500;
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "talariaStep")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "talariaStep", cooldown);
        cooldownAlarm(player, cooldown, "Talaria Step");

        boolean inWake = player.getScoreboardTags().contains("hermesWake");
        FleetFootwork.addMomentum(player, inWake ? 2 : 1);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        if (direction.getY() < -0.2) direction.setY(-0.2).normalize();

        double speedScore = FleetFootwork.getSpeedScore(player);
        double launch = 1.18 + speedScore * 0.42 + (inWake ? 0.18 : 0);
        player.setFallDistance(0);
        player.addScoreboardTag("hermesFallGrace");
        player.setVelocity(direction.clone().multiply(launch).add(new Vector(0, 0.18, 0)));

        Location start = player.getLocation().add(0, 1, 0);
        start.getWorld().playSound(start, Sound.ENTITY_PHANTOM_FLAP, 0.9f, 1.85f);
        start.getWorld().playSound(start, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.55f, 1.55f);
        start.getWorld().spawnParticle(Particle.FLASH, start, 1, 0, 0, 0, 0);
        drawWingBurst(player, start, direction, 1.0 + speedScore * 0.4 + (inWake ? 0.25 : 0));

        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= 12) {
                    cancel();
                    return;
                }

                if (ticks < 7) {
                    Vector vel = player.getVelocity().add(direction.clone().multiply(0.08 + speedScore * 0.05));
                    if (vel.length() > 1.85) vel = vel.normalize().multiply(1.85);
                    player.setVelocity(vel);
                }

                Location body = player.getLocation().add(0, 1, 0);
                drawWingTrail(player, body, direction, ticks);

                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 1.35, 1.6, 1.35)) {
                    if (!(entity instanceof LivingEntity living) || entity == player) continue;
                    if (entity instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, entity)) continue;
                    if (!hit.add(entity.getUniqueId())) continue;

                    FleetFootwork.addMomentum(player, inWake ? 2 : 1);
                    double damage = 4.0 + speedScore * 3.0 + (inWake ? 1.5 : 0);
                    damage(living, damage, player, true, true);
                    knockback(living, direction.clone().multiply(0.75 + speedScore * 0.45).add(new Vector(0, 0.34, 0)));
                    impact(player, living.getLocation().add(0, 1, 0));
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeScoreboardTag("hermesFallGrace");
            }
        }.runTaskLater(Minerva.getInstance(), 90L);
    }

    private void drawWingBurst(Player player, Location center, Vector direction, double size) {
        for (int side : new int[] {-1, 1}) {
            drawWing(player, center, direction, side, size, 0);
        }
        for (Vector point : ParticleUtils.getCirclePoints(0.9 * size, 24)) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(point), 0, 0, 0, 0, 0,
                new Particle.DustOptions(WING_COLORS[(int)(Math.random() * WING_COLORS.length)], 1.35f));
        }
        center.getWorld().spawnParticle(Particle.END_ROD, center, 26, 0.45, 0.35, 0.45, 0.13);
    }

    private void drawWingTrail(Player player, Location center, Vector direction, int ticks) {
        double size = 0.72 + Math.min(ticks, 6) * 0.04;
        for (int side : new int[] {-1, 1}) {
            drawWing(player, center, direction, side, size, ticks);
        }
        Location tail = center.clone().subtract(direction.clone().multiply(0.72));
        tail.getWorld().spawnParticle(Particle.END_ROD, tail, 4, 0.14, 0.14, 0.14, 0.05);
        tail.getWorld().spawnParticle(Particle.FIREWORK, tail, 2, 0.12, 0.08, 0.12, 0.035);
    }

    private void drawWing(Player player, Location center, Vector direction, int side, double size, int tickOffset) {
        Vector forward = direction.clone();
        forward.setY(0);
        if (forward.lengthSquared() < 0.001) forward = player.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.001) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

        for (int i = 0; i < 8; i++) {
            double t = i / 7.0;
            double sweep = side * (0.2 + t * 1.15 * size);
            double back = t * 0.85 * size;
            double lift = Math.sin(t * Math.PI) * 0.54 * size;
            Location point = center.clone()
                .add(right.clone().multiply(sweep))
                .subtract(forward.clone().multiply(back))
                .add(0, lift - 0.14, 0);
            Color color = WING_COLORS[(i + tickOffset + WING_COLORS.length) % WING_COLORS.length];
            player.getWorld().spawnParticle(Particle.DUST, point, 0, 0, 0, 0, 0, new Particle.DustOptions(color, 1.15f));
            if (i % 3 == 0) {
                player.getWorld().spawnParticle(Particle.CRIT, point, 1, 0.02, 0.02, 0.02, 0.03);
            }
        }
    }

    private void impact(Player player, Location loc) {
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 0.8f, 1.65f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.8f);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 24, 0.35, 0.35, 0.35, 0.35);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 14, 0.28, 0.28, 0.28, 0.1);
        for (Vector point : ParticleUtils.getStarPoints(5, 0.85, 0.42, 1)) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(point), 0, 0, 0, 0, 0,
                new Particle.DustOptions(WING_COLORS[(int)(Math.random() * WING_COLORS.length)], 1.25f));
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "talariaStep";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.GOLDEN_BOOTS)
            .setName(TextContext.formatLegacy("&lTalaria Step", false).color(TextColor.color(255, 214, 92)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Kick off with winged sandals, gliding", false),
                TextContext.formatLegacy("&7through the air in your chosen direction.", false),
                TextContext.formatLegacy("&7Enemies brushed by the wings take", false),
                TextContext.formatLegacy("&espeed-scaled damage &7and knockback.", false)
            )).build();
    }
}
