package net.minervamc.minerva.skills.greek.demeter;

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

public class RootSurge extends Skill {

    private static final Color[] ROOT_COLORS = {
        Color.fromRGB(55, 140, 35), Color.fromRGB(80, 170, 55),
        Color.fromRGB(110, 185, 70), Color.fromRGB(40, 110, 25),
        Color.fromRGB(160, 120, 50),
    };
    private static final Color[] BURST_COLORS = {
        Color.fromRGB(120, 220, 65), Color.fromRGB(205, 225, 85),
        Color.fromRGB(80, 195, 50), Color.fromRGB(255, 235, 110),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "rootSurge")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "rootSurge", cooldown);
        cooldownAlarm(player, cooldown, "Root Surge");

        // Minecraft interaction: on natural ground the cone is doubled
        Location playerLoc = player.getLocation();
        boolean enhanced = HarvestBlessing.isNaturalGround(
            player.getWorld().getBlockAt(playerLoc.getBlockX(), playerLoc.getBlockY() - 1, playerLoc.getBlockZ()));
        double coneLen = enhanced ? 10.0 : 5.0;
        double coneHalfAngle = enhanced ? 40.0 : 30.0; // degrees
        double ringRadius = 2.5;
        int rootTicks = 40; // 2s

        Location origin = player.getLocation().add(0, 0.5, 0);
        Vector facing = player.getEyeLocation().getDirection().setY(0).normalize();

        // Cast sounds
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 0.7f);
        if (enhanced) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.8f, 0.6f);
        }

        Set<UUID> hit = new HashSet<>();

        // Eruption visual — ground crack ring + forward spike line
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 12) { cancel(); return; }

                // Expanding ground-crack ring — earth breaking open
                double r = 0.5 + t * 0.22;
                for (Vector v : ParticleUtils.getCirclePoints(r, 22)) {
                    Location pl = origin.clone().add(v);
                    pl.getWorld().spawnParticle(Particle.DUST, pl, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(ROOT_COLORS[(int)(Math.random() * ROOT_COLORS.length)], 1.2f));
                    // Earth-crack feel: block fragments every other point
                    if ((int)(Math.random() * 3) == 0) {
                        pl.getWorld().spawnParticle(Particle.BLOCK, pl.clone().add(0, 0.1, 0),
                            3, 0.05, 0.15, 0.05, 0.08,
                            org.bukkit.Material.GRASS_BLOCK.createBlockData());
                    }
                }

                // Erupting root tendrils along the cone axis — shoot upward then fall
                if (t < 10) {
                    double dist = t * (coneLen / 10.0);
                    Location spike = origin.clone().add(facing.clone().multiply(dist));
                    // Main root spike
                    spike.getWorld().spawnParticle(Particle.DUST, spike, 8, 0.35, 0.6, 0.35, 0,
                        new Particle.DustOptions(ROOT_COLORS[1], 1.5f));
                    // Upward shoot
                    spike.getWorld().spawnParticle(Particle.DUST, spike.clone().add(0, 0.6, 0), 4, 0.2, 0.4, 0.2, 0,
                        new Particle.DustOptions(ROOT_COLORS[0], 1.2f));
                    spike.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, spike, 4, 0.2, 0.6, 0.2, 0.015);
                    spike.getWorld().spawnParticle(Particle.CHERRY_LEAVES, spike.clone().add(0, 0.5, 0),
                        2, 0.3, 0.2, 0.3, 0.05);
                    // Cracked earth under each tendril
                    spike.getWorld().spawnParticle(Particle.BLOCK, spike.clone().add(0, 0.05, 0),
                        5, 0.2, 0.05, 0.2, 0.1,
                        org.bukkit.Material.DIRT.createBlockData());
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // Slightly delayed hit detection so visual precedes it
        new BukkitRunnable() {
            @Override
            public void run() {
                // Cone hit
                for (Entity e : player.getWorld().getNearbyEntities(origin, coneLen, 3, coneLen)) {
                    if (!(e instanceof LivingEntity living) || e == player) continue;
                    if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, e)) continue;
                    if (hit.contains(e.getUniqueId())) continue;

                    Vector toEnt = e.getLocation().subtract(origin).toVector().setY(0);
                    if (toEnt.length() < 0.01) { applyHit(player, living, rootTicks, hit); continue; }
                    double angle = Math.toDegrees(Math.acos(Math.min(1, facing.dot(toEnt.normalize()))));
                    if (angle <= coneHalfAngle) applyHit(player, living, rootTicks, hit);
                }

                // Close-range ring around player (regardless of facing)
                for (Entity e : player.getWorld().getNearbyEntities(origin, ringRadius, 2.5, ringRadius)) {
                    if (!(e instanceof LivingEntity living) || e == player) continue;
                    if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, e)) continue;
                    if (hit.contains(e.getUniqueId())) continue;
                    applyHit(player, living, rootTicks, hit);
                }

                // Ground burst visual at cast center
                for (Vector v : ParticleUtils.getCirclePoints(ringRadius, 28)) {
                    origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(v), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(BURST_COLORS[(int)(Math.random() * BURST_COLORS.length)], 1.5f));
                }
                origin.getWorld().spawnParticle(Particle.COMPOSTER, origin.clone().add(0, 0.3, 0), 30, 1.5, 0.3, 1.5, 0.1);
                origin.getWorld().spawnParticle(Particle.END_ROD, origin.clone().add(0, 0.5, 0), 15, 0.8, 0.3, 0.8, 0.1);
                if (enhanced) {
                    origin.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, origin.clone().add(0, 0.5, 0), 20, 2, 0.5, 2, 0.1);
                }
            }
        }.runTaskLater(Minerva.getInstance(), 5L);
    }

    private void applyHit(Player player, LivingEntity target, int rootTicks, Set<UUID> hit) {
        hit.add(target.getUniqueId());
        damage(target, 12, player, true, true);

        // Extra 2 ticks if already in a thorn patch (combo)
        boolean inThorn = HarvestBlessing.getActiveThorns(player.getUniqueId()).stream()
            .anyMatch(t -> t.center.distanceSquared(target.getLocation()) <= t.radius * t.radius);
        int finalRoot = inThorn ? rootTicks + 40 : rootTicks;

        applyRoot(player, target, finalRoot);
        HarvestBlessing.spawnThorn(player, target.getLocation());

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.6f, 0.8f);
        for (Color c : ROOT_COLORS) {
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 0.5, 0),
                4, 0.3, 0.5, 0.3, 0, new Particle.DustOptions(c, 1.2f));
        }
    }

    // Roots the target in place with green vine visuals
    static void applyRoot(Player inflictor, Entity target, int ticks) {
        if (target.getScoreboardTags().contains("demeterRooted")) return;
        target.addScoreboardTag("demeterRooted");
        Location rootLoc = target.getLocation().clone();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= ticks || (target instanceof LivingEntity le && le.getHealth() <= 0)) {
                    target.removeScoreboardTag("demeterRooted");
                    cancel();
                    return;
                }
                target.teleport(rootLoc.clone().setDirection(target.getLocation().getDirection()));

                if (t % 4 == 0) {
                    for (Vector v : ParticleUtils.getCirclePoints(0.6, 12)) {
                        target.getWorld().spawnParticle(Particle.DUST,
                            rootLoc.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(55, 130, 30), 1f));
                    }
                    target.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                        rootLoc.clone().add(0, 0.2, 0), 3, 0.3, 0.5, 0.3, 0.01);
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "rootSurge"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.VINE)
            .setName(TextContext.formatLegacy("&lRoot Surge", false).color(TextColor.color(65, 160, 40)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Roots erupt in a cone and close ring,", false),
                TextContext.formatLegacy("&7dealing &a12 dmg &7and rooting enemies for &a2s&7.", false),
                TextContext.formatLegacy("&2On grass/dirt/farmland&7: cone doubles in size.", false),
                TextContext.formatLegacy("&7Rooted enemies in Thorn Patches take &a2× &7patch damage.", false),
                TextContext.formatLegacy("&7Enemies already in a patch get &a+2s &7extra root.", false)
            )).build();
    }
}
