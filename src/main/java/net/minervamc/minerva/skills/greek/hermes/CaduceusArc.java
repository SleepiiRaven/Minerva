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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class CaduceusArc extends Skill {

    private static final Color GOLD = Color.fromRGB(255, 218, 92);
    private static final Color WHITE_GOLD = Color.fromRGB(255, 252, 220);
    private static final Color TEAL = Color.fromRGB(112, 238, 255);
    private static final Color BLUE = Color.fromRGB(70, 170, 255);

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 7500;
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "caduceusArc")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "caduceusArc", cooldown);
        cooldownAlarm(player, cooldown, "Caduceus Arc");

        boolean inWake = player.getScoreboardTags().contains("hermesWake");
        FleetFootwork.addMomentum(player, inWake ? 2 : 1);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation().add(direction.clone().multiply(1.35));
        double speedScore = FleetFootwork.getSpeedScore(player);
        final int maxPierces = inWake ? 4 : 3;

        ItemDisplay staff = player.getWorld().spawn(start, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(Material.BLAZE_ROD));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
        });

        start.getWorld().playSound(start, Sound.ITEM_TRIDENT_THROW, 0.75f, 1.8f);
        start.getWorld().playSound(start, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.55f);

        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;
            int pierces = 0;

            @Override
            public void run() {
                if (!staff.isValid() || !player.isOnline() || player.isDead()) {
                    cleanup();
                    return;
                }
                if (ticks > 26 || pierces >= maxPierces) {
                    burst(staff.getLocation(), 0.75);
                    cleanup();
                    return;
                }

                Matrix4f spin = new Matrix4f().identity()
                    .rotateY((float) Math.toRadians(ticks * 34f))
                    .rotateZ((float) Math.toRadians(ticks * 52f))
                    .scaling(1.25f);
                staff.setTransformationMatrix(spin);
                staff.setInterpolationDelay(0);
                staff.setInterpolationDuration(1);

                staff.teleport(staff.getLocation().add(direction.clone().multiply(1.45 + speedScore * 0.22)));
                Location loc = staff.getLocation();
                renderCaduceusTrail(loc, direction, ticks);

                for (Entity entity : staff.getWorld().getNearbyEntities(loc, 0.9, 0.9, 0.9)) {
                    if (!(entity instanceof LivingEntity living) || entity == player || entity == staff) continue;
                    if (entity instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, entity)) continue;
                    if (!hit.add(entity.getUniqueId())) continue;

                    pierces++;
                    FleetFootwork.addMomentum(player, inWake ? 2 : 1);
                    double damage = 6.5 + speedScore * 3.5 + (inWake ? 1.25 : 0);
                    damage(living, damage, player, true, true);
                    knockback(living, direction.clone().multiply(0.45 + speedScore * 0.35).add(new Vector(0, 0.25, 0)));
                    impact(living.getLocation().add(0, 1, 0), speedScore);
                    if (pierces >= maxPierces) {
                        cleanup();
                        return;
                    }
                }

                if (loc.getBlock().isSolid()) {
                    burst(loc, 1.0);
                    cleanup();
                    return;
                }

                ticks++;
            }

            private void cleanup() {
                if (staff.isValid()) staff.remove();
                cancel();
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void renderCaduceusTrail(Location loc, Vector direction, int ticks) {
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = right.clone().crossProduct(direction).normalize();

        double radius = 0.34 + Math.sin(ticks * 0.35) * 0.04;
        for (int strand = 0; strand < 2; strand++) {
            double phase = strand == 0 ? 0 : Math.PI;
            double angle = ticks * 0.78 + phase;
            Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                .add(up.clone().multiply(Math.sin(angle) * radius));
            Color color = strand == 0 ? GOLD : TEAL;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(offset), 0, 0, 0, 0, 0,
                new Particle.DustOptions(color, 1.05f));
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().subtract(offset.clone().multiply(0.65)), 0, 0, 0, 0, 0,
                new Particle.DustOptions(strand == 0 ? WHITE_GOLD : BLUE, 0.85f));
        }

        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.06, 0.06, 0.06, 0.035);
        if (ticks % 2 == 0) {
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 10, 0.2, 0.2, 0.2, 0.6);
        }
    }

    private void impact(Location loc, double speedScore) {
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 0.8f, 1.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.65f);
        burst(loc, 0.8 + speedScore * 0.5);
    }

    private void burst(Location loc, double scale) {
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 22, 0.35 * scale, 0.35 * scale, 0.35 * scale, 0.12);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 26, 0.32 * scale, 0.32 * scale, 0.32 * scale, 0.35);
        for (Vector point : ParticleUtils.getCirclePoints(0.75 * scale, 20)) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(point), 0, 0, 0, 0, 0,
                new Particle.DustOptions(Math.random() > 0.5 ? GOLD : TEAL, 1.2f));
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "caduceusArc";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.BLAZE_ROD)
            .setName(TextContext.formatLegacy("&lCaduceus Arc", false).color(TextColor.color(112, 238, 255)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Fling a spinning caduceus that carves", false),
                TextContext.formatLegacy("&7two bright helixes through the air.", false),
                TextContext.formatLegacy("&7Pierces up to &e3 &7targets (&e4 &7in Wake)", false),
                TextContext.formatLegacy("&espeed-scaled damage&7.", false)
            )).build();
    }
}
