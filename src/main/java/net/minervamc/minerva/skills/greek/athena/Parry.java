package net.minervamc.minerva.skills.greek.athena;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
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

public class Parry extends Skill {

    private static final Color[] SHIELD_GRADIENT = {
        Color.fromRGB(200, 220, 255),
        Color.fromRGB(160, 195, 255),
        Color.fromRGB(120, 170, 255),
        Color.fromRGB(180, 210, 255),
        Color.fromRGB(230, 240, 255),
        Color.fromRGB(255, 255, 255),
        Color.fromRGB(140, 180, 255),
    };

    private static final Color[] PARRY_BURST = {
        Color.fromRGB(255, 255, 255),
        Color.fromRGB(200, 225, 255),
        Color.fromRGB(120, 170, 255),
        Color.fromRGB(80, 140, 255),
        Color.fromRGB(255, 230, 100),
        Color.fromRGB(255, 200, 50),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 12000;
        int duration = 40; // 4 seconds

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "parry")) {
            onCooldown(player);
            return;
        }

        if (player.getScoreboardTags().contains("athenaParry")) return;

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "parry", cooldown);
        cooldownAlarm(player, cooldown, "Parry");

        player.addScoreboardTag("athenaParry");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.8f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
                    player.removeScoreboardTag("athenaParry");
                    player.removeScoreboardTag("athenaParrySuccess");
                    // Fade-out sound
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.5f, 0.8f);
                    cancel();
                    return;
                }

                if (player.getScoreboardTags().contains("athenaParrySuccess")) {
                    player.removeScoreboardTag("athenaParry");
                    player.removeScoreboardTag("athenaParrySuccess");
                    cleave(player, ticks);
                    cancel();
                    return;
                }

                // Draw the shield every tick, always facing the player's current direction
                drawShield(player, ticks, duration);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 2L);
    }

    private void drawShield(Player player, int ticks, int duration) {
        // Aegis outline — four bezier arcs forming a kite/shield silhouette
        List<Vector> shieldPoints = ParticleUtils.getQuadraticBezierPoints(
            new Vector(0, 1.4, 0), new Vector(0, 1.0, 0), new Vector(-1.1, 1.0, 0), 12);
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(
            new Vector(0, 1.4, 0), new Vector(0, 1.0, 0), new Vector( 1.1, 1.0, 0), 12));
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(
            new Vector(0, -1.2, 0), new Vector( 1.1, -0.3, 0), new Vector( 1.1, 1.0, 0), 12));
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(
            new Vector(0, -1.2, 0), new Vector(-1.1, -0.3, 0), new Vector(-1.1, 1.0, 0), 12));

        // Inner cross — Athena's aegis detail
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(
            new Vector(-0.5, 0.4, 0), new Vector(0, 0.4, 0), new Vector(0.5, 0.4, 0), 6));
        shieldPoints.addAll(ParticleUtils.getQuadraticBezierPoints(
            new Vector(0, 0.9, 0), new Vector(0, 0.4, 0), new Vector(0, -0.2, 0), 6));

        Location pLoc = player.getLocation().add(0, 1, 0);
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        Color[] activeGradient = SHIELD_GRADIENT;
        double pulse = 0.85 + 0.15 * Math.sin(ticks * 0.3);
        boolean nearExpiry = ticks > (int)(duration * 0.75);

        for (Vector point : shieldPoints) {
            Vector rotated = ParticleUtils.rotatePitchYawFromXY(point, pitch, yaw);
            Location particleLoc = pLoc.clone().add(rotated.multiply(pulse));
            Color color = activeGradient[(int)(Math.random() * activeGradient.length)];
            player.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0,
                new Particle.DustOptions(color, nearExpiry ? 0.8f : 1.1f));
        }

        if (ticks % 3 == 0) {
            Location center = pLoc.clone().add(
                ParticleUtils.rotatePitchYawFromXY(new Vector(0, 0.1, 0), pitch, yaw));
            player.getWorld().spawnParticle(Particle.END_ROD, center, 1, 0.3, 0.3, 0.05, 0.01);
        }

        if (nearExpiry && ticks % 6 < 3) {
            player.sendActionBar(Component.text("⚔ PARRY ⚔", TextColor.color(255, 80, 80)));
        } else if (!nearExpiry) {
            player.sendActionBar(Component.text("⚔ PARRY ⚔", TextColor.color(120, 170, 255)));
        }
    }

    private void cleave(Player player, int ticks) {
        // 0–12 ticks (0–0.6s): PERFECT | 13–35 ticks: GOOD | 36–80 ticks: LATE
        boolean perfect = ticks <= 12;
        boolean good    = ticks <= 35;

        double momentumMultiplier = TacticalAgility.detonateIfCharged(player);
        double damage   = (perfect ? 18.0 : good ? 12.0 : 7.0) * momentumMultiplier;
        double radius   = perfect ? 5.5 : good ? 3.5 : 2.0;
        float  kb       = perfect ? 2.5f : good ? 1.6f : 0.8f;
        int    stunTicks = perfect ? 40  : good ? 20  : 0;

        Location loc = player.getLocation();

        // Tier label + color
        Component label;
        TextColor ringColor;
        if (perfect) {
            label     = Component.text("✦ PERFECT PARRY! ✦", TextColor.color(255, 215, 0));
            ringColor = TextColor.color(255, 215, 0);
        } else if (good) {
            label     = Component.text("GOOD PARRY!", TextColor.color(180, 220, 255));
            ringColor = TextColor.color(120, 170, 255);
        } else {
            label     = Component.text("PARRY", TextColor.color(200, 200, 200));
            ringColor = TextColor.color(180, 180, 180);
        }
        player.sendActionBar(label);

        // Shockwave ring — size scales with tier
        final double maxRing = radius;
        new BukkitRunnable() {
            double r = 0.3;
            @Override
            public void run() {
                if (r > maxRing) { cancel(); return; }
                int points = perfect ? 32 : good ? 24 : 16;
                for (Vector v : ParticleUtils.getCirclePoints(r, points)) {
                    Location ring = loc.clone().add(v).add(0, 1, 0);
                    Color color = PARRY_BURST[(int)(Math.random() * PARRY_BURST.length)];
                    player.getWorld().spawnParticle(Particle.DUST, ring, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(color, perfect ? 1.8f : good ? 1.4f : 1f));
                }
                r += 0.45;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // Central burst — scales with tier
        Location center = loc.clone().add(0, 1, 0);
        if (perfect) {
            loc.getWorld().spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
            loc.getWorld().spawnParticle(Particle.END_ROD, center, 60, 0.6, 0.6, 0.6, 0.3);
            loc.getWorld().spawnParticle(Particle.ENCHANT, center, 150, 1, 1, 1, 2);
            for (Color c : PARRY_BURST) {
                loc.getWorld().spawnParticle(Particle.DUST, center, 20, 0.7, 0.7, 0.7, 0, new Particle.DustOptions(c, 2f));
            }
            loc.getWorld().spawnParticle(Particle.CRIT, center, 40, 0.5, 0.5, 0.5, 0.8);
        } else if (good) {
            loc.getWorld().spawnParticle(Particle.END_ROD, center, 30, 0.4, 0.4, 0.4, 0.2);
            loc.getWorld().spawnParticle(Particle.ENCHANT, center, 60, 0.7, 0.7, 0.7, 1.2);
            loc.getWorld().spawnParticle(Particle.CRIT, center, 20, 0.4, 0.4, 0.4, 0.5);
        } else {
            loc.getWorld().spawnParticle(Particle.END_ROD, center, 10, 0.3, 0.3, 0.3, 0.1);
            loc.getWorld().spawnParticle(Particle.CRIT, center, 8, 0.3, 0.3, 0.3, 0.3);
        }

        // Sounds — bigger and more dramatic for perfect
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, perfect ? 1f : 0.7f, perfect ? 1.6f : 1.3f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, perfect ? 0.8f : 0.4f, 1.8f);
        if (perfect) {
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
            loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.7f, 1.5f);
        }

        // Damage + knockback all nearby enemies
        for (Entity e : loc.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity lE) || e == player) continue;
            if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;

            damage(lE, damage, player, true, true);
            Vector kbVec = ParticleUtils.getDirection(loc, e.getLocation()).multiply(kb).add(new Vector(0, perfect ? 0.5 : 0.2, 0));
            knockback(e, kbVec);
            if (stunTicks > 0) stun(player, e, stunTicks);
        }
    }

    @Override
    public String getLevelDescription(int level) { return ""; }

    @Override
    public String toString() { return "parry"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.SHIELD)
            .setName(TextContext.formatLegacy("&lParry", false).color(TextColor.color(120, 170, 255)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Raise Athena's aegis for &b4 seconds&7.", false),
                TextContext.formatLegacy("&7Taking a hit triggers a counter-shockwave.", false),
                TextContext.formatLegacy("&6✦ Perfect &7(under 0.6s): max damage, stun,", false),
                TextContext.formatLegacy("&b  Good &7(under 1.75s): reduced damage & stun.", false),
                TextContext.formatLegacy("&f  Late &7(after 1.75s): weak damage only.", false)
            )).build();
    }
}
