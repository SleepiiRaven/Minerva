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

public class KineticDispatch extends Skill {

    private static final Color[] DISPATCH_COLORS = {
        Color.fromRGB(255, 250, 218),
        Color.fromRGB(255, 220, 88),
        Color.fromRGB(255, 172, 48),
        Color.fromRGB(100, 230, 255)
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 9000;
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "kineticDispatch")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "kineticDispatch", cooldown);
        cooldownAlarm(player, cooldown, "Kinetic Dispatch");

        boolean inWake = player.getScoreboardTags().contains("hermesWake");
        int beforeStacks = FleetFootwork.getMomentumStacks(player);
        double spent = beforeStacks >= 3 ? FleetFootwork.consumeMomentum(player, Math.ceil(beforeStacks / 2.0)) : 0;
        FleetFootwork.addMomentum(player, 1);

        double speedScore = Math.max(FleetFootwork.getSpeedScore(player), spent / FleetFootwork.MAX_MOMENTUM);
        double range = 8.5 + speedScore * 5.5 + spent * 0.55 + (inWake ? 1.5 : 0);
        double maxWidth = 1.25 + speedScore * 1.0 + spent * 0.08;
        double damage = 5.5 + speedScore * 6.5 + spent * 0.9 + (inWake ? 1.25 : 0);

        Vector direction = player.getEyeLocation().getDirection();
        direction.setY(Math.max(direction.getY() * 0.35, -0.12));
        direction.normalize();
        Vector flat = direction.clone();
        flat.setY(0);
        if (flat.lengthSquared() < 0.001) flat = player.getLocation().getDirection().setY(0);
        if (flat.lengthSquared() < 0.001) flat = new Vector(0, 0, 1);
        flat.normalize();
        Vector right = new Vector(-flat.getZ(), 0, flat.getX()).normalize();
        final Vector castForward = flat.clone();
        final Vector castRight = right.clone();

        Location origin = player.getEyeLocation().add(direction.clone().multiply(0.7));
        origin.getWorld().playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.65f, 1.45f);
        origin.getWorld().playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7f, 1.75f);
        origin.getWorld().spawnParticle(Particle.FLASH, origin, 1, 0, 0, 0, 0);
        drawSeal(origin, right, flat, 1.0 + speedScore * 0.45);

        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks > 9) {
                    cancel();
                    return;
                }

                double distance = 1.2 + ticks * (range / 9.0);
                double width = 0.45 + (distance / range) * maxWidth;
                drawCrescent(origin, castForward, castRight, distance, width, ticks, speedScore);

                for (Entity entity : player.getWorld().getNearbyEntities(origin, range + 1.5, 3.0, range + 1.5)) {
                    if (!(entity instanceof LivingEntity living) || entity == player) continue;
                    if (entity instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, entity)) continue;
                    if (hit.contains(entity.getUniqueId())) continue;

                    Vector rel = living.getLocation().add(0, 1, 0).toVector().subtract(origin.toVector());
                    double forward = rel.dot(castForward);
                    if (forward < Math.max(0.2, distance - range / 9.0 - 0.6) || forward > distance + 1.0) continue;

                    Vector lateral = rel.clone().subtract(castForward.clone().multiply(forward));
                    lateral.setY(0);
                    if (lateral.length() > width + 0.55) continue;

                    hit.add(entity.getUniqueId());
                    FleetFootwork.addMomentum(player, inWake ? 2 : 1);
                    damage(living, damage, player, true, true);
                    Vector push = castForward.clone().multiply(0.55 + speedScore * 0.4).add(new Vector(0, 0.28, 0));
                    knockback(living, push);
                    hitBurst(living.getLocation().add(0, 1, 0), speedScore);
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void drawCrescent(Location origin, Vector forward, Vector right, double distance, double width, int ticks, double speedScore) {
        Location center = origin.clone().add(forward.clone().multiply(distance));
        int points = 15 + (int)(speedScore * 6);
        for (int i = -points; i <= points; i++) {
            double f = i / (double) points;
            double side = f * width;
            double lift = Math.cos(f * Math.PI / 2.0) * (0.35 + speedScore * 0.2);
            double curl = Math.sin(f * Math.PI) * 0.35;
            Location point = center.clone()
                .add(right.clone().multiply(side))
                .add(forward.clone().multiply(curl))
                .add(0, lift - 0.12, 0);
            Color color = DISPATCH_COLORS[(Math.abs(i) + ticks) % DISPATCH_COLORS.length];
            origin.getWorld().spawnParticle(Particle.DUST, point, 0, 0, 0, 0, 0,
                new Particle.DustOptions(color, 1.15f + (float)(speedScore * 0.3)));
            if (i % 5 == 0) {
                origin.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.02);
            }
        }

        if (ticks % 2 == 0) {
            origin.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 6, width * 0.25, 0.2, width * 0.25, 0.04);
        }
        if (speedScore > 0.75 && ticks == 5) {
            origin.getWorld().spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
        }
    }

    private void drawSeal(Location origin, Vector right, Vector forward, double scale) {
        for (Vector point : ParticleUtils.getCirclePoints(0.65 * scale, 26)) {
            origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(point), 0, 0, 0, 0, 0,
                new Particle.DustOptions(DISPATCH_COLORS[(int)(Math.random() * DISPATCH_COLORS.length)], 1.0f));
        }
        for (int side : new int[] {-1, 1}) {
            for (int i = 0; i < 8; i++) {
                double t = i / 7.0;
                Location point = origin.clone()
                    .add(right.clone().multiply(side * (0.25 + t * 1.0 * scale)))
                    .subtract(forward.clone().multiply(t * 0.45 * scale))
                    .add(0, Math.sin(t * Math.PI) * 0.45 * scale, 0);
                origin.getWorld().spawnParticle(Particle.DUST, point, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(DISPATCH_COLORS[i % DISPATCH_COLORS.length], 1.1f));
            }
        }
        origin.getWorld().spawnParticle(Particle.END_ROD, origin, 18, 0.4, 0.35, 0.4, 0.09);
    }

    private void hitBurst(Location loc, double speedScore) {
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.85f, 1.65f);
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.35f, 1.9f);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 24, 0.35, 0.35, 0.35, 0.5);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 18, 0.3, 0.3, 0.3, 0.12);
        for (Vector point : ParticleUtils.getCirclePoints(0.75 + speedScore * 0.45, 24)) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(point), 0, 0, 0, 0, 0,
                new Particle.DustOptions(DISPATCH_COLORS[(int)(Math.random() * DISPATCH_COLORS.length)], 1.25f));
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "kineticDispatch";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.CLOCK)
            .setName(TextContext.formatLegacy("&lKinetic Dispatch", false).color(TextColor.color(255, 220, 88)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Cash in Momentum stacks as a", false),
                TextContext.formatLegacy("&7wing-shaped shockwave in front of you.", false),
                TextContext.formatLegacy("&7Hits refund Momentum; Wake makes it", false),
                TextContext.formatLegacy("&7larger, brighter, and meaner.", false)
            )).build();
    }
}
