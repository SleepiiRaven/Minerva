package net.minervamc.minerva.skills.greek.demeter;

import java.util.List;
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

public class SeedBarrage extends Skill {

    private static final Color[] SEED_TRAIL = {
        Color.fromRGB(180, 210, 80), Color.fromRGB(210, 195, 60),
        Color.fromRGB(255, 235, 100), Color.fromRGB(130, 200, 55),
    };
    private static final Color[] SPROUT_COLORS = {
        Color.fromRGB(60, 160, 35), Color.fromRGB(90, 185, 55),
        Color.fromRGB(120, 210, 75), Color.fromRGB(50, 130, 28),
    };

    // Spread angles in degrees (horizontal)
    private static final double[] SPREAD = { -16, -8, 0, 8, 16 };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 8000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "seedBarrage")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "seedBarrage", cooldown);
        cooldownAlarm(player, cooldown, "Seed Barrage");

        boolean raining = player.getWorld().hasStorm();
        double baseDamage = raining ? 10.0 : 8.0;
        double speed = raining ? 1.3 : 1.0;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.5f, 1.5f);
        if (raining) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.5f, 1.6f);
        }

        Vector baseDir = player.getEyeLocation().getDirection().normalize();

        for (double angle : SPREAD) {
            // Rotate base direction horizontally by spread angle
            double rad = Math.toRadians(angle);
            double cos = Math.cos(rad), sin = Math.sin(rad);
            Vector dir = new Vector(
                baseDir.getX() * cos - baseDir.getZ() * sin,
                baseDir.getY(),
                baseDir.getX() * sin + baseDir.getZ() * cos
            ).normalize();

            ItemDisplay seed = player.getWorld().spawn(
                player.getEyeLocation().add(dir.clone().multiply(1.2)),
                ItemDisplay.class,
                e -> {
                    e.setItemStack(new ItemStack(Material.WHEAT_SEEDS));
                    e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                }
            );

            launchSeed(player, seed, dir, speed, baseDamage, raining);
        }
    }

    private void launchSeed(Player player, ItemDisplay seed, Vector dir, double speed, double baseDamage, boolean raining) {
        new BukkitRunnable() {
            int t = 0;
            double dist = 0;
            Vector currentDir = dir.clone();

            @Override
            public void run() {
                if (!seed.isValid()) { cancel(); return; }
                if (dist >= 22 || t > 50) {
                    seed.remove();
                    cancel();
                    return;
                }

                // Gentle homing toward nearest enemy when raining
                if (raining && t % 3 == 0) {
                    LivingEntity nearest = nearestEnemy(player, seed.getLocation(), 6.0);
                    if (nearest != null) {
                        Vector pull = nearest.getEyeLocation().subtract(seed.getLocation()).toVector().normalize().multiply(0.15);
                        currentDir = currentDir.clone().add(pull).normalize();
                    }
                }

                seed.teleport(seed.getLocation().add(currentDir.clone().multiply(speed)));
                dist += speed;

                // Trail — vivid sparkle + petal scatter
                Location loc = seed.getLocation();
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.06, 0.06, 0.06, 0,
                    new Particle.DustOptions(SEED_TRAIL[(int)(Math.random() * SEED_TRAIL.length)], 1.1f));
                loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 1, 0.1, 0.1, 0.1, 0.04);
                if (t % 2 == 0) {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.04, 0.04, 0.04, 0.02);
                }
                if (t % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 2, 0.06, 0.12, 0.06, 0.01);
                }

                // Entity collision
                for (Entity e : seed.getWorld().getNearbyEntities(seed.getLocation(), 0.7, 0.7, 0.7)) {
                    if (!(e instanceof LivingEntity living) || e == player || e == seed) continue;
                    if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, e)) continue;

                    onEntityHit(player, seed, living, baseDamage);
                    seed.remove();
                    cancel();
                    return;
                }

                // Ground / block collision → sprout a thorn patch
                if (seed.getLocation().getBlock().isSolid() ||
                    seed.getLocation().clone().subtract(0, 0.2, 0).getBlock().isSolid()) {
                    onGroundHit(player, seed.getLocation());
                    seed.remove();
                    cancel();
                }

                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    private void onEntityHit(Player player, ItemDisplay seed, LivingEntity target, double damage) {
        damage(target, damage, player, true, true);

        // Tag the enemy as Seeded (combo state for Harvest Wrath and RootSurge)
        target.addScoreboardTag("demeterSeeded");
        new BukkitRunnable() {
            @Override public void run() { target.removeScoreboardTag("demeterSeeded"); }
        }.runTaskLater(Minerva.getInstance(), 80L); // 4 seconds

        // Combo: if already rooted, force-sprout a large thorn
        if (target.getScoreboardTags().contains("demeterRooted")) {
            HarvestBlessing.ThornPatch forced = new HarvestBlessing.ThornPatch(
                target.getLocation(), 3.5, player.getUniqueId(), false, false, 2.0, 22.0, 100);
            HarvestBlessing.ACTIVE_THORNS.add(forced);
            forced.start();
            target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 0.8f);
        } else {
            HarvestBlessing.spawnThorn(player, target.getLocation());
        }

        // Visual on hit
        Location loc = target.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 12, 0.25, 0.3, 0.25, 0,
            new Particle.DustOptions(Color.fromRGB(130, 215, 60), 1.3f));
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 8, 0.2, 0.4, 0.2, 0.02);
        loc.getWorld().playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.8f, 1.4f);
    }

    private void onGroundHit(Player player, Location loc) {
        HarvestBlessing.spawnSeedThorn(player, loc.clone().add(0, 0.1, 0));

        // Flower bloom — petals burst upward + green dust eruption
        for (Color c : SPROUT_COLORS) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.2, 0), 5, 0.15, 0.35, 0.15, 0,
                new Particle.DustOptions(c, 1.2f));
        }
        loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc.clone().add(0, 0.5, 0), 10, 0.5, 0.4, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.4, 0), 10, 0.3, 0.35, 0.3, 0.08);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.3, 0), 5, 0.2, 0.25, 0.2, 0.12);
        loc.getWorld().spawnParticle(Particle.COMPOSTER, loc.clone().add(0, 0.1, 0), 6, 0.25, 0.1, 0.25, 0.06);
        loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.5f, 1.2f);
        loc.getWorld().playSound(loc, Sound.ITEM_BONE_MEAL_USE, 0.3f, 1.4f);
    }

    private LivingEntity nearestEnemy(Player player, Location from, double maxDist) {
        LivingEntity best = null;
        double bestDist = maxDist * maxDist;
        for (Entity e : from.getWorld().getNearbyEntities(from, maxDist, maxDist, maxDist)) {
            if (!(e instanceof LivingEntity living) || e == player) continue;
            if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            double d = from.distanceSquared(e.getLocation());
            if (d < bestDist) { bestDist = d; best = living; }
        }
        return best;
    }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "seedBarrage"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.WHEAT_SEEDS)
            .setName(TextContext.formatLegacy("&lSeed Barrage", false).color(TextColor.color(190, 215, 75)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Fire 5 seeds in a spread. Hits: &a8 dmg &7+ Seeded.", false),
                TextContext.formatLegacy("&7Misses: thorn patch sprouts on the ground.", false),
                TextContext.formatLegacy("&2In rain&7: seeds home + deal &a10 dmg&7.", false),
                TextContext.formatLegacy("&7Hit a &arooted &7enemy: force-sprout a &alarge &7patch.", false),
                TextContext.formatLegacy("&7&oSeeded enemies take +10 burst from Harvest.", false)
            )).build();
    }
}
