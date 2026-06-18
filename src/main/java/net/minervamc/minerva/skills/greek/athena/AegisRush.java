package net.minervamc.minerva.skills.greek.athena;

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

public class AegisRush extends Skill {

    private static final Color[] TRAIL_COLORS = {
        Color.fromRGB(120, 170, 255),
        Color.fromRGB(180, 210, 255),
        Color.fromRGB(255, 255, 255),
        Color.fromRGB(80, 140, 220),
        Color.fromRGB(200, 225, 255),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;
        double damage = 8;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "aegisRush")) {
            onCooldown(player);
            return;
        }

        boolean empowered = player.getScoreboardTags().contains("athenaParry");
        if (empowered) player.removeScoreboardTag("athenaParry");

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "aegisRush", cooldown);
        cooldownAlarm(player, cooldown, "Aegis Rush");

        double momentumMultiplier = TacticalAgility.detonateIfCharged(player);
        Vector dir = player.getEyeLocation().getDirection().setY(0).normalize();
        double finalDamage = (empowered ? damage * 1.8 : damage) * momentumMultiplier;

        // Sounds
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.9f);
        if (empowered) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
            Location castLoc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.FLASH, castLoc, 1, 0, 0, 0, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, castLoc, 30, 0.5, 0.5, 0.5, 0.25);
            for (Color c : TRAIL_COLORS) {
                player.getWorld().spawnParticle(Particle.DUST, castLoc, 10, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(c, 2f));
            }
        }

        // Launch — velocity is smoother and feels punchy; slight Y lifts player airborne
        // so air friction (0.91/tick) applies instead of the heavier ground friction
        double launchSpeed = empowered ? 2.5 : 2;
        player.setVelocity(dir.clone().multiply(launchSpeed).add(new Vector(0, 0.15, 0)));

        Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10 || !player.isOnline()) {
                    landingShockwave(player, empowered);
                    cancel();
                    return;
                }

                // Trail: body position + a ghost at the spot just left behind
                Location body = player.getLocation().add(0, 1, 0);
                Vector vel = player.getVelocity();
                Location ghost = body.clone().subtract(
                    vel.length() > 0.01 ? vel.clone().normalize().multiply(0.6) : new Vector());

                // Main dust cloud
                player.getWorld().spawnParticle(Particle.DUST, body, 8, 0.2, 0.35, 0.2, 0,
                    ParticleUtils.getDustOptionsFromGradient(TRAIL_COLORS, 1.5f));
                // Ghost streak behind
                player.getWorld().spawnParticle(Particle.DUST, ghost, 5, 0.12, 0.25, 0.12, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 225, 255), 1.2f));
                // White afterimage dots
                player.getWorld().spawnParticle(Particle.END_ROD, body, 4, 0.15, 0.2, 0.15, 0.04);
                // Impact sparks
                player.getWorld().spawnParticle(Particle.CRIT, body, 5, 0.2, 0.2, 0.2, 0.06);

                if (empowered) {
                    player.getWorld().spawnParticle(Particle.ENCHANT, body, 14, 0.35, 0.4, 0.35, 0.9);
                    player.getWorld().spawnParticle(Particle.END_ROD, ghost, 3, 0.1, 0.2, 0.1, 0.02);
                }

                // Foot-level sweep ring every other tick
                if (ticks % 2 == 0) {
                    for (Vector v : ParticleUtils.getCirclePoints(0.7, 12)) {
                        player.getWorld().spawnParticle(Particle.DUST,
                            player.getLocation().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TRAIL_COLORS[0], 0.9f));
                    }
                }

                // Hit detection
                for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 1.4, 1.9, 1.4)) {
                    if (!(e instanceof LivingEntity living) || e == player) continue;
                    if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, e)) continue;
                    if (hit.contains(e.getUniqueId())) continue;

                    hit.add(e.getUniqueId());
                    damage(living, finalDamage, player, true, true);

                    if (empowered) {
                        knockback(living, new Vector(dir.getX() * 1.2, 1.0, dir.getZ() * 1.2));
                        stun(player, living, 20);
                    } else {
                        knockback(living, new Vector(dir.getX() * 0.8, 0.4, dir.getZ() * 0.8));
                    }

                    Location hitLoc = living.getLocation().add(0, 1, 0);
                    player.getWorld().playSound(hitLoc, Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.3f);
                    player.getWorld().spawnParticle(Particle.CRIT, hitLoc, 20, 0.3, 0.3, 0.3, 0.5);
                    player.getWorld().spawnParticle(Particle.END_ROD, hitLoc, 12, 0.3, 0.3, 0.3, 0.2);
                    for (Color c : TRAIL_COLORS) {
                        player.getWorld().spawnParticle(Particle.DUST, hitLoc, 5, 0.3, 0.3, 0.3, 0,
                            new Particle.DustOptions(c, 1.6f));
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void landingShockwave(Player player, boolean empowered) {
        Location loc = player.getLocation();
        double maxRadius = empowered ? 3.5 : 2.2;

        // Two expanding rings at slightly different speeds for depth
        new BukkitRunnable() {
            double r = 0.3;
            @Override
            public void run() {
                if (r > maxRadius + 0.5) { cancel(); return; }
                int pts = Math.max(16, (int)(r * 20));
                for (Vector v : ParticleUtils.getCirclePoints(r, pts)) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v).add(0, 0.05, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(TRAIL_COLORS[(int)(Math.random() * TRAIL_COLORS.length)], 1.3f));
                }
                if (r < maxRadius * 0.6) {
                    for (Vector v : ParticleUtils.getCirclePoints(r * 0.6, pts / 2)) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(v).add(0, 0.08, 0), 0, 0, 0, 0, 0);
                    }
                }
                r += 0.52;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 25, 0.5, 0.25, 0.5, 0.18);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.3, 0), 18, 0.6, 0.2, 0.6, 0.12);
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 0.7f, 1.4f);
        if (empowered) {
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.5, 0), 60, 1.0, 0.4, 1.0, 1.5);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.1f);
        }
    }

    @Override
    public String getLevelDescription(int level) { return ""; }

    @Override
    public String toString() { return "aegisRush"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.DIAMOND_CHESTPLATE)
            .setName(TextContext.formatLegacy("&lAegis Rush", false).color(TextColor.color(120, 170, 255)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Dash forward with Athena's shield,", false),
                TextContext.formatLegacy("&7ramming enemies in your path.", false),
                TextContext.formatLegacy("&eCast in &6Parry stance &efor bonus damage,", false),
                TextContext.formatLegacy("&eupward knockback, and a stun.", false)
            )).build();
    }
}
