package net.minervamc.minerva.skills.greek.demeter;

import java.util.List;
import net.kyori.adventure.text.Component;
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

public class HarvestWrath extends Skill {

    private static final Color[] WINDUP_COLORS = {
        Color.fromRGB(200, 210, 60), Color.fromRGB(220, 190, 45),
        Color.fromRGB(255, 230, 85), Color.fromRGB(160, 200, 50),
        Color.fromRGB(240, 215, 70), Color.fromRGB(120, 185, 40),
    };
    private static final Color[] SHOCKWAVE_COLORS = {
        Color.fromRGB(130, 230, 65), Color.fromRGB(210, 225, 85),
        Color.fromRGB(255, 240, 110), Color.fromRGB(80, 200, 50),
    };

    private static final double SHOCKWAVE_RADIUS = 8.0;

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 20000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "harvestWrath")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "harvestWrath", cooldown);
        cooldownAlarm(player, cooldown, "Harvest Wrath");

        boolean thundering = player.getWorld().isThundering() || player.getWorld().hasStorm();
        int thornCount = HarvestBlessing.getActiveThorns(player.getUniqueId()).size();

        // Windup — leaves orbit the player rising in a spiral
        new BukkitRunnable() {
            int t = 0;
            double spiralAngle = 0;
            @Override
            public void run() {
                if (t >= 30) { cancel(); return; } // 1.5 second windup

                double r = 1.0 + t * 0.05;
                double y = 0.5 + t * 0.05;
                spiralAngle += 22;
                double rad = Math.toRadians(spiralAngle);

                // Three orbiting clusters of particles
                for (int arm = 0; arm < 3; arm++) {
                    double armRad = rad + arm * (Math.PI * 2.0 / 3.0);
                    Location pLoc = player.getLocation().add(
                        r * Math.cos(armRad), y, r * Math.sin(armRad));
                    Color c = WINDUP_COLORS[(int)(Math.random() * WINDUP_COLORS.length)];
                    pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(c, 1.4f - t * 0.01f));
                    if (t % 4 == 0) {
                        pLoc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, pLoc, 1, 0.05, 0.1, 0.05, 0.01);
                    }
                }

                // Cherry leaves swirling outward from arm tips
                if (t % 3 == 0) {
                    for (int arm = 0; arm < 3; arm++) {
                        double leafArmRad = rad + arm * (Math.PI * 2.0 / 3.0) + 0.4;
                        Location leafLoc = player.getLocation().add(
                            (r + 0.3) * Math.cos(leafArmRad), y + 0.2, (r + 0.3) * Math.sin(leafArmRad));
                        leafLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, leafLoc, 2, 0.2, 0.3, 0.2, 0.05);
                    }
                }
                // Glowing END_ROD at each arm tip every 5 ticks — golden corona
                if (t % 5 == 0) {
                    for (int arm = 0; arm < 3; arm++) {
                        double armRad = rad + arm * (Math.PI * 2.0 / 3.0);
                        Location tipLoc = player.getLocation().add(r * Math.cos(armRad), y, r * Math.sin(armRad));
                        tipLoc.getWorld().spawnParticle(Particle.END_ROD, tipLoc, 3, 0.06, 0.1, 0.06, 0.04);
                    }
                }

                // Pulsing ring on the ground
                if (t % 5 == 0) {
                    for (Vector v : ParticleUtils.getCirclePoints(1.5 + t * 0.12, 24)) {
                        player.getWorld().spawnParticle(Particle.DUST,
                            player.getLocation().add(v).add(0, 0.05, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(WINDUP_COLORS[t % WINDUP_COLORS.length], 1.1f));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 0.8f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.5f);

        // After 1.5 second windup — RELEASE
        new BukkitRunnable() {
            @Override
            public void run() {
                // Count combos for messaging
                boolean anyRooted = hasRootedEnemies(player);
                boolean anySeeded = hasSeededEnemies(player);
                boolean hasBlightZone = HarvestBlessing.getActiveThorns(player.getUniqueId())
                    .stream().anyMatch(t -> t.isBlightZone);

                // Detonate ALL thorn patches
                HarvestBlessing.detonateAll(player);

                // Shockwave — expanding ring + AoE damage
                doShockwave(player, thundering);

                // Combo feedback
                int combos = (anyRooted ? 1 : 0) + (anySeeded ? 1 : 0) + (hasBlightZone ? 1 : 0) + (thornCount >= 3 ? 1 : 0);
                if (combos >= 3) {
                    player.sendActionBar(Component.text("✦ DEMETER'S HARVEST ✦", TextColor.color(230, 220, 55)));
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.9f);
                } else if (combos >= 2) {
                    player.sendActionBar(Component.text("✦ Full Harvest! ✦", TextColor.color(140, 210, 60)));
                } else if (thornCount > 0) {
                    player.sendActionBar(Component.text("Harvest", TextColor.color(100, 180, 50)));
                }
            }
        }.runTaskLater(Minerva.getInstance(), 30L); // 1.5 seconds
    }

    private void doShockwave(Player player, boolean thundering) {
        Location origin = player.getLocation().add(0, 0.5, 0);

        // Central burst — the great harvest eruption
        origin.getWorld().spawnParticle(Particle.FLASH, origin, 1, 0, 0, 0, 0);
        origin.getWorld().spawnParticle(Particle.END_ROD, origin, 80, 1.2, 0.8, 1.2, 0.35);
        origin.getWorld().spawnParticle(Particle.COMPOSTER, origin, 50, 0.8, 0.5, 0.8, 0.3);
        origin.getWorld().spawnParticle(Particle.CHERRY_LEAVES, origin.clone().add(0, 1.5, 0),
            70, 2.0, 1.0, 2.0, 0.22);
        for (Color c : SHOCKWAVE_COLORS) {
            origin.getWorld().spawnParticle(Particle.DUST, origin, 25, 1.0, 0.7, 1.0, 0,
                new Particle.DustOptions(c, 2.2f));
        }

        // Sounds
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1f, 0.6f);
        origin.getWorld().playSound(origin, Sound.ITEM_BONE_MEAL_USE, 1f, 0.4f);
        origin.getWorld().playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.7f);
        if (thundering) {
            origin.getWorld().playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.2f);
        }

        // Expanding shockwave ring + petal scatter as it grows
        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > SHOCKWAVE_RADIUS) { cancel(); return; }
                int pts = Math.max(16, (int)(r * 18));
                for (Vector v : ParticleUtils.getCirclePoints(r, pts)) {
                    origin.getWorld().spawnParticle(Particle.DUST,
                        origin.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(SHOCKWAVE_COLORS[(int)(Math.random() * SHOCKWAVE_COLORS.length)], 1.8f));
                }
                // Cherry leaves scattered around the expanding front
                origin.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
                    origin.clone().add(0, 0.9, 0), 3, r * 0.5, 0.3, r * 0.5, 0.07);
                r += 0.7;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // Damage + knockback all enemies in shockwave
        for (Entity e : origin.getWorld().getNearbyEntities(origin, SHOCKWAVE_RADIUS, SHOCKWAVE_RADIUS, SHOCKWAVE_RADIUS)) {
            if (!(e instanceof LivingEntity living) || e == player) continue;
            if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, e)) continue;

            damage(living, 15, player, true, true);
            Vector kb = ParticleUtils.getDirection(origin, e.getLocation()).multiply(2.2).add(new Vector(0, 0.4, 0));
            knockback(e, kb);
            stun(player, e, 25);

            if (thundering) {
                // Cosmetic lightning on each hit target
                new BukkitRunnable() {
                    @Override public void run() {
                        if (e.isValid()) e.getWorld().strikeLightningEffect(e.getLocation());
                    }
                }.runTaskLater(Minerva.getInstance(), 3L);
            }
        }
    }

    private boolean hasRootedEnemies(Player player) {
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 20, 20, 20)) {
            if (!(e instanceof LivingEntity) || e == player) continue;
            if (e.getScoreboardTags().contains("demeterRooted")) return true;
        }
        return false;
    }

    private boolean hasSeededEnemies(Player player) {
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 20, 20, 20)) {
            if (!(e instanceof LivingEntity) || e == player) continue;
            if (e.getScoreboardTags().contains("demeterSeeded")) return true;
        }
        return false;
    }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "harvestWrath"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.GOLDEN_HOE)
            .setName(TextContext.formatLegacy("&lHarvest Wrath", false).color(TextColor.color(220, 210, 55)))
            .setLore(List.of(
                TextContext.formatLegacy("&7After 1.5s windup: all Thorn Patches and", false),
                TextContext.formatLegacy("&7Blight Zones detonate simultaneously.", false),
                TextContext.formatLegacy("&7Then: &a15 dmg &7shockwave in 8 block radius.", false),
                TextContext.formatLegacy("&2In thunder/rain&7: lightning strikes each detonation.", false),
                TextContext.formatLegacy("&7&oRooted = 1.5× detonate dmg. Seeded = +10 burst.", false)
            )).build();
    }
}
