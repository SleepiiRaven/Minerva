package net.minervamc.minerva.skills.greek.hestia;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hestia RLL — the payoff. Spends the ENTIRE Ember bank: the more you saved, the bigger the pyre.
 * A telegraph contracts the ember ring inward, then a forward cone of fire erupts, scaling damage
 * by Embers spent. A large spend also leaves a scorch patch on the ground.
 */
public class PyreRelease extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 7500;
            case 4, 5 -> 7000;
            default -> 8000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "pyreRelease")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "pyreRelease", cooldown);
        cooldownAlarm(player, cooldown, "Pyre Release");

        final int spent = BankedEmbers.spendAll(player);
        final int fLevel = level;
        final double baseDmg = 4 + 1.3 * spent;
        final int scorchThreshold = level >= 2 ? 6 : 8;
        final boolean pierces = level >= 3;

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);

        // telegraph: ember ring contracting inward for 10 ticks
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 10) {
                    fireCone(player, baseDmg, spent, fLevel, scorchThreshold, pierces);
                    cancel();
                    return;
                }
                double r = 2.4 - (t / 10.0) * 1.9;
                Location c = player.getLocation().clone().add(0, 0.5, 0);
                List<Vector> ring = ParticleUtils.getCirclePoints(r, Math.max(10, (int) (r * 10)));
                for (int i = 0; i < ring.size(); i++) {
                    Color col = BankedEmbers.HEARTH[i % BankedEmbers.HEARTH.length];
                    world.spawnParticle(Particle.DUST, c.clone().add(ring.get(i)), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(col, 1.1f));
                }
                if (t % 3 == 0) {
                    world.playSound(player.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.2f + t * 0.05f);
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static void fireCone(Player player, double dmg, int spent, int level, int scorchThreshold, boolean pierces) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        final double range = 6.0;
        final double dotMin = 0.6; // ~53 degree half-cone

        world.playSound(eye, Sound.ITEM_FIRECHARGE_USE, 1.4f, 0.6f);
        world.playSound(eye, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
        world.playSound(eye, Sound.BLOCK_FIRE_AMBIENT, 1f, 0.8f);

        // cone visual: a forward fan of flame + spatial DUST gradient
        for (double d = 1.0; d <= range; d += 0.7) {
            Location base = eye.clone().add(dir.clone().multiply(d));
            double spread = 0.2 * d;
            List<Vector> circle = ParticleUtils.getVerticalCirclePoints(spread, eye.getPitch(), eye.getYaw(), 8);
            world.spawnParticle(Particle.FLAME, base, 3, spread * 0.4, spread * 0.4, spread * 0.4, 0.01);
            for (int i = 0; i < circle.size(); i++) {
                Color col = BankedEmbers.HEARTH[(int) ((d / range) * (BankedEmbers.HEARTH.length - 1))];
                world.spawnParticle(Particle.DUST, base.clone().add(circle.get(i)), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(col, 1.2f));
            }
        }

        // damage enemies inside the cone
        int hits = 0;
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            Vector to = le.getLocation().clone().add(0, le.getHeight() / 2, 0).toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            if (to.normalize().dot(dir) < dotMin) continue;
            targets.add(le);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = le;
            }
        }
        for (LivingEntity le : targets) {
            damage(le, dmg, player);
            hits++;
            if (!pierces && hits >= 6) break; // soft cap when non-piercing
        }

        // L5: a full-bank pyre stuns the nearest target
        if (level >= 5 && spent >= 14 && nearest != null) {
            stun(player, nearest, 10);
        }

        // scorch patch when a big bank was spent
        if (spent >= scorchThreshold) {
            Location patch = eye.clone().add(dir.clone().multiply(range * 0.6));
            patch.setY(player.getLocation().getY());
            scorch(player, patch, level);
        }
    }

    /** A brief ground field that damages (and at L4 slows) enemies who stand in it. */
    private static void scorch(Player owner, Location center, int level) {
        World world = center.getWorld();
        final boolean slows = level >= 4;
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 60 || world == null) {
                    cancel();
                    return;
                }
                if (t % 4 == 0) {
                    List<Vector> ring = ParticleUtils.getCirclePoints(1.6, 16);
                    for (int i = 0; i < ring.size(); i++) {
                        Color col = BankedEmbers.HEARTH[i % BankedEmbers.HEARTH.length];
                        world.spawnParticle(Particle.DUST, center.clone().add(ring.get(i)).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(col, 1.1f));
                    }
                    world.spawnParticle(Particle.SMALL_FLAME, center.clone().add(0, 0.2, 0), 6, 1.2, 0.1, 1.2, 0.0);
                }
                if (t % 20 == 0) {
                    for (Entity e : world.getNearbyEntities(center, 1.6, 1.5, 1.6)) {
                        if (!(e instanceof LivingEntity le) || e == owner) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                        if (PlayerStats.isSummoned(owner, le)) continue;
                        damage(le, 2, owner, true, true);
                        if (slows) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Spend all Embers in a fire cone — bigger bank, bigger blast.";
            case 2 -> ChatColor.GRAY + "Scorch patch ignites with fewer Embers spent.";
            case 3 -> ChatColor.GRAY + "The cone pierces through enemies.";
            case 4 -> ChatColor.GRAY + "The scorch patch also slows enemies.";
            case 5 -> ChatColor.GRAY + "A full-bank pyre stuns the nearest enemy.";
            default -> ChatColor.GRAY + "Spend all Embers in a scaling fire cone.";
        };
    }

    @Override
    public String toString() {
        return "pyreRelease";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.FIRE_CHARGE),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Pyre Release]",
                ChatColor.GRAY + "Spend " + ChatColor.GOLD + "all your Embers" + ChatColor.GRAY + " in a forward cone of fire.",
                ChatColor.GRAY + "Damage scales with the " + ChatColor.GOLD + "Embers" + ChatColor.GRAY + " you banked —",
                ChatColor.GRAY + "a big spend leaves a " + ChatColor.GOLD + "scorch patch" + ChatColor.GRAY + " behind.");
    }
}
