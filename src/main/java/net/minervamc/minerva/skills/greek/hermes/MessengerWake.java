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

public class MessengerWake extends Skill {

    private static final Color[] WAKE_COLORS = {
        Color.fromRGB(255, 246, 205),
        Color.fromRGB(255, 210, 82),
        Color.fromRGB(120, 232, 255),
        Color.fromRGB(255, 255, 255)
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10500;
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "messengerWake")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "messengerWake", cooldown);
        cooldownAlarm(player, cooldown, "Messenger Wake");

        player.addScoreboardTag("hermesWake");
        player.addScoreboardTag("hermesFallGrace");
        player.setFallDistance(0);

        double momentum = FleetFootwork.addMomentum(player, 1.65);
        double ratio = momentum / FleetFootwork.MAX_MOMENTUM;
        Vector launch = player.getEyeLocation().getDirection();
        launch.setY(Math.max(-0.05, Math.min(0.35, launch.getY())));
        if (launch.lengthSquared() < 0.001) launch = player.getLocation().getDirection();
        launch.normalize();
        player.setVelocity(capHorizontal(
            player.getVelocity().add(launch.clone().multiply(0.62 + ratio * 0.55)).add(new Vector(0, 0.12 + ratio * 0.08, 0)),
            1.28 + ratio * 0.3
        ));

        Location start = player.getLocation().add(0, 1, 0);
        start.getWorld().playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.75f, 1.7f);
        start.getWorld().playSound(start, Sound.BLOCK_BEACON_ACTIVATE, 0.45f, 1.45f);
        start.getWorld().spawnParticle(Particle.FLASH, start, 1, 0, 0, 0, 0);
        seal(start, 1.15 + ratio * 0.35);

        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;
            Location lastLoc = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= 72) {
                    endWake(player);
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                double moved = horizontalDistance(lastLoc, loc);
                double currentRatio = FleetFootwork.getMomentumRatio(player);
                boolean movingWithIntent = moved > 0.025 || player.isSprinting();
                Vector travel = player.getVelocity().clone();
                travel.setY(0);
                if (travel.lengthSquared() < 0.004) travel = player.getLocation().getDirection().setY(0);
                if (travel.lengthSquared() < 0.001) travel = new Vector(0, 0, 1);
                travel.normalize();

                if (movingWithIntent) {
                    if (ticks % 4 == 0) {
                        currentRatio = FleetFootwork.addMomentum(player, 0.18 + currentRatio * 0.08) / FleetFootwork.MAX_MOMENTUM;
                    }
                    if (ticks % 2 == 0) {
                        Vector pushed = player.getVelocity().add(travel.clone().multiply(0.025 + currentRatio * 0.035));
                        player.setVelocity(capHorizontal(pushed, 1.08 + currentRatio * 0.26));
                    }
                }

                drawWake(player, loc, ticks, currentRatio);

                if (ticks % 2 == 0) {
                    for (Entity entity : player.getWorld().getNearbyEntities(loc, 1.2, 1.4, 1.2)) {
                        if (!(entity instanceof LivingEntity living) || entity == player) continue;
                        if (entity instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, entity)) continue;
                        if (!hit.add(entity.getUniqueId())) continue;

                        double speedScore = FleetFootwork.getSpeedScore(player);
                        damage(living, 4.0 + speedScore * 3.2, player, true, true);
                        FleetFootwork.addMomentum(player, 0.75 + speedScore * 0.35);
                        Vector push = living.getLocation().toVector().subtract(player.getLocation().toVector());
                        push.setY(0);
                        if (push.lengthSquared() < 0.01) push = player.getLocation().getDirection().setY(0);
                        if (push.lengthSquared() > 0.01) {
                            knockback(living, push.normalize().multiply(0.58 + speedScore * 0.25).add(new Vector(0, 0.24, 0)));
                        }
                        wakeHit(living.getLocation().add(0, 1, 0));
                    }
                }

                lastLoc = loc;
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void drawWake(Player player, Location loc, int ticks, double momentumRatio) {
        Vector forward = player.getVelocity().clone();
        forward.setY(0);
        if (forward.lengthSquared() < 0.004) forward = player.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.001) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

        Location base = loc.clone().add(0, 0.18, 0);
        for (int side : new int[] {-1, 1}) {
            for (int i = 0; i < 7; i++) {
                double back = 0.18 + i * (0.2 + momentumRatio * 0.08);
                double wave = Math.sin(ticks * 0.45 + i * 0.7) * (0.18 + momentumRatio * 0.1);
                Location point = base.clone()
                    .subtract(forward.clone().multiply(back))
                    .add(right.clone().multiply(side * (0.28 + wave)))
                    .add(0, Math.sin(i * 0.65) * 0.12, 0);
                Color color = WAKE_COLORS[(ticks + i + side + WAKE_COLORS.length) % WAKE_COLORS.length];
                player.getWorld().spawnParticle(Particle.DUST, point, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 1.0f + (float)(momentumRatio * 0.35)));
            }
        }

        if (ticks % 3 == 0) {
            Location behind = base.clone().subtract(forward.clone().multiply(0.85));
            player.getWorld().spawnParticle(Particle.END_ROD, behind, 4, 0.18, 0.08, 0.18, 0.04);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, behind, 3, 0.12, 0.08, 0.12, 0.03);
        }

        if (ticks % 10 == 0) {
            for (Vector point : ParticleUtils.getCirclePoints(0.55 + momentumRatio * 0.25, 18)) {
                player.getWorld().spawnParticle(Particle.DUST, base.clone().add(point), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(WAKE_COLORS[(int)(Math.random() * WAKE_COLORS.length)], 0.85f));
            }
        }
    }

    private void wakeHit(Location loc) {
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.55f, 1.75f);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 14, 0.28, 0.28, 0.28, 0.08);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 18, 0.32, 0.32, 0.32, 0.1);
        seal(loc, 0.7);
    }

    private void seal(Location loc, double scale) {
        for (Vector point : ParticleUtils.getStarPoints(6, 0.75 * scale, 0.38, 1)) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(point), 0, 0, 0, 0, 0,
                new Particle.DustOptions(WAKE_COLORS[(int)(Math.random() * WAKE_COLORS.length)], 1.2f));
        }
    }

    private void endWake(Player player) {
        player.removeScoreboardTag("hermesWake");
        player.removeScoreboardTag("hermesFallGrace");
        Location loc = player.getLocation().add(0, 0.25, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.6f, 1.8f);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 16, 0.55, 0.08, 0.55, 0.05);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 18, 0.4, 0.12, 0.4, 0.07);
    }

    private Vector capHorizontal(Vector velocity, double maxHorizontal) {
        Vector horizontal = velocity.clone();
        horizontal.setY(0);
        if (horizontal.length() <= maxHorizontal) return velocity;

        Vector capped = horizontal.normalize().multiply(maxHorizontal);
        return new Vector(capped.getX(), velocity.getY(), capped.getZ());
    }

    private double horizontalDistance(Location from, Location to) {
        double dx = from.getX() - to.getX();
        double dz = from.getZ() - to.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "messengerWake";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.FIREWORK_ROCKET)
            .setName(TextContext.formatLegacy("&lMessenger Wake", false).color(TextColor.color(255, 246, 205)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Kick open a radiant slipstream.", false),
                TextContext.formatLegacy("&7Moving through it feeds &eMomentum&7,", false),
                TextContext.formatLegacy("&7pushes you forward, and leaves a", false),
                TextContext.formatLegacy("&7damaging wake behind you.", false)
            )).build();
    }
}
