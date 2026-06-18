package net.minervamc.minerva.skills.greek.athena;

import java.util.ArrayList;
import java.util.Comparator;
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
import org.bukkit.ChatColor;
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

public class SpearRain extends Skill {

    private static final Color[] SPEAR_COLORS = {
        Color.fromRGB(255, 215, 0),
        Color.fromRGB(255, 230, 80),
        Color.fromRGB(255, 255, 180),
        Color.fromRGB(200, 220, 255),
        Color.fromRGB(255, 255, 255),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 14000;
        int maxTargets = 3;
        double searchRadius = 15;
        double spearDamage = 10;
        double aoeDamage = 5;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "spearRain")) {
            onCooldown(player);
            return;
        }

        // Find nearest enemies sorted by distance
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), searchRadius, searchRadius, searchRadius)) {
            if (!(e instanceof LivingEntity living) || e == player) continue;
            if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, e)) continue;
            targets.add(living);
        }
        targets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())));
        if (targets.size() > maxTargets) targets = targets.subList(0, maxTargets);

        if (targets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No enemies in range.");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "spearRain", cooldown);
        cooldownAlarm(player, cooldown, "Spear Rain");

        double momentumMultiplier = TacticalAgility.detonateIfCharged(player);

        // Cast sounds + overhead flash
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.2);

        for (LivingEntity target : targets) {
            launchSpear(player, target, spearDamage * momentumMultiplier, aoeDamage * momentumMultiplier);
        }
    }

    private void launchSpear(Player player, LivingEntity target, double spearDamage, double aoeDamage) {
        Location spawnLoc = target.getLocation().clone().add(0, 7, 0);

        ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.TRIDENT));
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
            // Point tip downward
            e.setTransformationMatrix(new Matrix4f().identity().rotateX((float) Math.toRadians(180)));
        });

        // Charge-up hover phase
        new BukkitRunnable() {
            int ticks = 0;
            float spin = 0;

            @Override
            public void run() {
                if (!display.isValid()) { cancel(); return; }

                // Track target + slight bob
                Location hover = target.getLocation().clone().add(0, 7 + Math.sin(ticks * 0.5) * 0.15, 0);
                display.teleport(hover);

                // Spin while charging
                spin += 18;
                display.setTransformationMatrix(
                    new Matrix4f().identity()
                        .rotateX((float) Math.toRadians(180))
                        .rotateY((float) Math.toRadians(spin))
                );
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(1);

                // Expanding gold ring below spear at target feet
                double ringRadius = 0.4 + ticks * 0.06;
                for (Vector v : ParticleUtils.getCirclePoints(ringRadius, 20)) {
                    target.getWorld().spawnParticle(Particle.DUST,
                        target.getLocation().clone().add(v).add(0, 0.05, 0),
                        0, 0, 0, 0, 0,
                        ParticleUtils.getDustOptionsFromGradient(SPEAR_COLORS, 1f));
                }
                // Beam of light from spear downward toward target
                if (ticks % 2 == 0) {
                    for (double y = 1; y < 6; y += 0.5) {
                        target.getWorld().spawnParticle(Particle.END_ROD,
                            target.getLocation().clone().add(0, y, 0),
                            0, 0, 0, 0, 0);
                    }
                }

                ticks++;
                if (ticks >= 18) {
                    cancel();
                    plunge(player, display, target, spearDamage, aoeDamage);
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void plunge(Player player, ItemDisplay display, LivingEntity target, double spearDamage, double aoeDamage) {
        player.getWorld().playSound(display.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.9f);
        player.getWorld().playSound(display.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 0.6f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!display.isValid()) { cancel(); return; }

                display.teleport(display.getLocation().subtract(0, 1.1, 0));

                // Falling trail
                display.getWorld().spawnParticle(Particle.END_ROD, display.getLocation(), 2, 0.05, 0.15, 0.05, 0.01);
                display.getWorld().spawnParticle(Particle.DUST, display.getLocation(), 4, 0.1, 0.15, 0.1, 0,
                    ParticleUtils.getDustOptionsFromGradient(SPEAR_COLORS, 1.5f));

                boolean hitBlock = display.getLocation().getBlock().isSolid();
                boolean hitTarget = display.getLocation().distanceSquared(target.getLocation().add(0, 1, 0)) < 3.0;

                if (hitBlock || hitTarget) {
                    onImpact(player, display.getLocation(), target, spearDamage, aoeDamage);
                    display.remove();
                    cancel();
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void onImpact(Player player, Location loc, LivingEntity primary, double spearDamage, double aoeDamage) {
        if (primary.isValid() && !primary.isDead()) {
            damage(primary, spearDamage, player, true, true);
            stun(player, primary, 20);
        }

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (!(e instanceof LivingEntity living) || e == primary || e == player) continue;
            if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, e)) continue;
            damage(living, aoeDamage, player, true, true);
            Vector kb = ParticleUtils.getDirection(loc, e.getLocation()).multiply(1.2);
            knockback(e, kb);
        }

        // Impact burst
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 45, 0.5, 0.5, 0.5, 0.28);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 30, 0.3, 0.3, 0.3, 0.6);
        for (Color c : SPEAR_COLORS) {
            loc.getWorld().spawnParticle(Particle.DUST, loc, 16, 0.45, 0.45, 0.45, 0, new Particle.DustOptions(c, 1.8f));
        }

        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 1.2f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
    }

    @Override
    public String getLevelDescription(int level) { return ""; }

    @Override
    public String toString() { return "spearRain"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.TRIDENT)
            .setName(TextContext.formatLegacy("&lSpear Rain", false).color(TextColor.color(255, 200, 50)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Call divine spears from the heavens,", false),
                TextContext.formatLegacy("&7targeting up to 3 nearby enemies.", false),
                TextContext.formatLegacy("&7Each spear charges up, then plunges —", false),
                TextContext.formatLegacy("&7stunning the target and splashing nearby foes.", false)
            )).build();
    }
}
