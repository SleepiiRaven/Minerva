package net.minervamc.minerva.skills.greek.demeter;

import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ThornBlight extends Skill {

    private static final Color[] BLIGHT_BEAM = {
        Color.fromRGB(25, 80, 18), Color.fromRGB(40, 100, 28),
        Color.fromRGB(18, 60, 12), Color.fromRGB(55, 115, 38),
    };
    private static final Color[] ROT_BEAM = {
        Color.fromRGB(10, 50, 30), Color.fromRGB(20, 70, 40),
        Color.fromRGB(5, 35, 20), Color.fromRGB(30, 90, 50),
    };
    private static final Color[] PULSE_COLORS = {
        Color.fromRGB(30, 90, 20), Color.fromRGB(50, 115, 35),
        Color.fromRGB(70, 140, 50), Color.fromRGB(20, 70, 15),
    };

    private static final int MAX_RANGE = 18;

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 12000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "thornBlight")) {
            onCooldown(player);
            return;
        }

        // Raycast to find target location
        Location target = raycast(player, MAX_RANGE);

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "thornBlight", cooldown);
        cooldownAlarm(player, cooldown, "Thorn Blight");

        // Check for nearby water blocks at target
        boolean nearWater = hasNearbyWater(target, 3);
        Color[] beamColors = nearWater ? ROT_BEAM : BLIGHT_BEAM;

        // Beam visual toward target during windup
        Location from = player.getEyeLocation();
        Vector direction = target.clone().subtract(from).toVector().normalize();
        double beamDist = from.distance(target);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 10) { cancel(); return; }
                // Beam — denser with corruption wisps
                for (double d = 0.5; d < beamDist; d += 0.5) {
                    Location bLoc = from.clone().add(direction.clone().multiply(d));
                    bLoc.getWorld().spawnParticle(Particle.DUST, bLoc, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(beamColors[(int)(Math.random() * beamColors.length)], 0.9f));
                    if (Math.random() < 0.25) {
                        bLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, bLoc, 1, 0.08, 0.15, 0.08, 0.02);
                    }
                }
                // Outer pulsing ring at target
                double pulseR = 0.5 + t * 0.25;
                for (Vector v : ParticleUtils.getCirclePoints(pulseR, 16)) {
                    target.getWorld().spawnParticle(Particle.DUST, target.clone().add(v).add(0, 0.05, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(PULSE_COLORS[(int)(Math.random() * PULSE_COLORS.length)], 1.1f));
                }
                // Dark inner ring — ominous core
                for (Vector v : ParticleUtils.getCirclePoints(pulseR * 0.45, 8)) {
                    target.getWorld().spawnParticle(Particle.DUST, target.clone().add(v).add(0, 0.05, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(5, 20, 5), 0.8f));
                }
                if (t % 2 == 0) {
                    target.getWorld().spawnParticle(Particle.END_ROD, target.clone().add(0, 0.4, 0),
                        2, pulseR * 0.3, 0.3, pulseR * 0.3, 0.02);
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_VINE_BREAK, 0.7f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.4f, 1.6f);

        // Windup delay before blight appears
        new BukkitRunnable() {
            @Override
            public void run() {
                HarvestBlessing.spawnBlightZone(player, target, nearWater);

                // Activation burst — dramatic zone formation
                double r = nearWater ? 4.5 : 4.0;
                target.getWorld().spawnParticle(Particle.FLASH, target.clone().add(0, 0.3, 0), 1, 0, 0, 0, 0);
                target.getWorld().spawnParticle(Particle.END_ROD, target.clone().add(0, 0.5, 0),
                    30, r / 2.5, 0.5, r / 2.5, 0.15);
                for (Vector v : ParticleUtils.getCirclePoints(r, 32)) {
                    target.getWorld().spawnParticle(Particle.DUST, target.clone().add(v).add(0, 0.08, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(beamColors[(int)(Math.random() * beamColors.length)], 1.4f));
                }
                target.getWorld().spawnParticle(Particle.CHERRY_LEAVES, target.clone().add(0, 1.5, 0),
                    nearWater ? 15 : 25, r / 2, 0.6, r / 2, 0.1);
                target.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, target.clone().add(0, 0.5, 0),
                    45, r / 2, 0.8, r / 2, 0.02);
                if (nearWater) {
                    target.getWorld().spawnParticle(Particle.DRIPPING_DRIPSTONE_WATER, target.clone().add(0, 1.5, 0),
                        25, r / 2, 0.5, r / 2, 0.05);
                    target.getWorld().spawnParticle(Particle.DRIPPING_WATER, target.clone().add(0, 2, 0),
                        12, r / 2.5, 0.2, r / 2.5, 0.04);
                    target.getWorld().playSound(target, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.6f, 0.7f);
                }
                target.getWorld().playSound(target, Sound.BLOCK_GRASS_BREAK, 1f, 0.5f);
                target.getWorld().playSound(target, Sound.ITEM_BONE_MEAL_USE, 0.8f, 0.6f);
            }
        }.runTaskLater(Minerva.getInstance(), 10L); // 0.5s windup
    }

    private Location raycast(Player player, int maxRange) {
        Location loc = player.getEyeLocation().clone();
        Vector step = loc.getDirection().normalize().multiply(0.5);
        for (int i = 0; i < maxRange * 2; i++) {
            loc.add(step);
            Block block = loc.getBlock();
            if (block.isSolid()) {
                loc.subtract(step);
                break;
            }
        }
        return loc.clone().subtract(0, loc.getY() - player.getWorld()
            .getBlockAt(loc).getLocation().getY(), 0);
    }

    private boolean hasNearbyWater(Location loc, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material m = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z).getType();
                    if (m == Material.WATER || m == Material.KELP_PLANT || m == Material.SEAGRASS) return true;
                }
            }
        }
        return false;
    }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "thornBlight"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.POISONOUS_POTATO)
            .setName(TextContext.formatLegacy("&lThorn Blight", false).color(TextColor.color(35, 100, 25)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Mark a location up to 18 blocks away.", false),
                TextContext.formatLegacy("&7After 0.5s: a &aBlight Zone &7erupts, dealing", false),
                TextContext.formatLegacy("&7damage/s + Slowness to enemies inside (6s).", false),
                TextContext.formatLegacy("&2Near water&7: &aRot Zone &7— adds Weakness + 2× damage.", false),
                TextContext.formatLegacy("&7Zones detonate with &aHarvest Wrath&7.", false)
            )).build();
    }
}
