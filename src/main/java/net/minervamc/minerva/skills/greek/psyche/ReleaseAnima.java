package net.minervamc.minerva.skills.greek.psyche;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Psyche RRR (the keystone). First press drops an Anima: a snapshot of position + health, grants
 * wings (SPEED), and plants a chrysalis anchor. Re-pressing within the window — or letting the
 * window expire — RECALLS: teleport back to the anchor (or onto a Soul-Lance mark if one is set),
 * heal toward the snapshot health (never overheal), and cleanse your own debuffs. The whole undo
 * loop. No strobe: wings are a steady mirrored CUBIC bezier of fixed-gradient DUST.
 */
public class ReleaseAnima extends Skill {
    /** Per-player stored snapshot. */
    private static class Snapshot {
        Location loc;
        double health;
        long expiresAt;
        LivingEntity recallTarget;
        int level;
        BukkitRunnable anchorTask;
    }

    private static final Map<UUID, Snapshot> ANIMA = new HashMap<>();

    public static boolean hasAnima(Player player) {
        return ANIMA.containsKey(player.getUniqueId());
    }

    public static Location anchorOf(Player player) {
        Snapshot s = ANIMA.get(player.getUniqueId());
        return s == null ? null : s.loc;
    }

    /** Soul Lance calls this to point the next recall at a marked enemy. */
    public static void setRecallTarget(Player player, LivingEntity target) {
        Snapshot s = ANIMA.get(player.getUniqueId());
        if (s != null) {
            s.recallTarget = target;
        }
    }

    /** Used internally (and by Soul Lance L5) to inspect the current mark. */
    public static LivingEntity getRecallTarget(Player player) {
        Snapshot s = ANIMA.get(player.getUniqueId());
        return s == null ? null : s.recallTarget;
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        // If an anima is active, this press is the RECALL — no cooldown gate on recall itself.
        if (hasAnima(player)) {
            recall(player);
            return;
        }

        long cooldown = switch (level) {
            case 2, 3 -> 11000;
            case 4, 5 -> 10000;
            default -> 12000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "releaseAnima")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "releaseAnima", cooldown);
        cooldownAlarm(player, cooldown, "Release Anima");

        Snapshot s = new Snapshot();
        s.loc = player.getLocation().clone();
        s.health = player.getHealth();
        s.level = level;
        int windowTicks = level >= 2 ? 120 : 100;
        s.expiresAt = System.currentTimeMillis() + windowTicks * 50L;
        ANIMA.put(player.getUniqueId(), s);

        // wings buff
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, windowTicks, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);

        drawWings(player, windowTicks);
        drawAnchor(player, s, windowTicks);

        // auto-recall when the window expires
        final int fLevel = level;
        new BukkitRunnable() {
            @Override
            public void run() {
                Snapshot cur = ANIMA.get(player.getUniqueId());
                if (cur == s && player.isOnline() && !player.isDead()) {
                    recall(player);
                } else if (cur == s) {
                    ANIMA.remove(player.getUniqueId());
                }
            }
        }.runTaskLater(Minerva.getInstance(), windowTicks);
    }

    /** Perform the recall: teleport, heal toward snapshot, cleanse, optionally tug/cleanse party. */
    public static void recall(Player player) {
        Snapshot s = ANIMA.remove(player.getUniqueId());
        if (s == null) return;
        if (s.anchorTask != null) {
            try { s.anchorTask.cancel(); } catch (IllegalStateException ignored) {}
        }

        Location from = player.getLocation().clone();
        Location dest;
        if (s.recallTarget != null && !s.recallTarget.isDead()
                && s.recallTarget.getWorld().equals(player.getWorld())) {
            dest = s.recallTarget.getLocation().clone();
        } else {
            dest = s.loc != null ? s.loc.clone() : from;
        }

        // 0.5-size DUST streak from current pos to anchor/mark
        World w = player.getWorld();
        if (from.getWorld().equals(dest.getWorld())) {
            int idx = 0;
            for (Vector v : ParticleUtils.getLinePoints(from.clone().add(0, 1, 0).toVector(),
                    dest.clone().add(0, 1, 0).toVector(), 0.5)) {
                w.spawnParticle(Particle.DUST, v.toLocation(w), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(IridescentSoul.IRIDESCENT[idx++ % IridescentSoul.IRIDESCENT.length], 0.5f));
            }
        }

        // tug threaded enemies toward where we land (and L4 Soul Lance flavor: recall-to-mark tugs)
        SoulThread.tugThreaded(player, dest);

        player.teleport(dest);

        // heal toward snapshot, never overheal and never reduce current health
        double target = Math.min(player.getMaxHealth(), Math.max(player.getHealth(), s.health));
        player.setHealth(target);

        // cleanse own debuffs
        cleanseSelf(player);

        // L3: short ABSORPTION on recall
        if (s.level >= 3) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 1));
        }

        // L5: cleanse party near the anchor (include caster)
        if (s.level >= 5) {
            List<Player> party = Party.partyList(player);
            if (party != null) {
                for (Player ally : party) {
                    if (ally == null || !ally.isOnline()) continue;
                    if (ally.getWorld().equals(dest.getWorld()) && ally.getLocation().distanceSquared(dest) <= 64) {
                        cleanseSelf(ally);
                        ally.getWorld().spawnParticle(Particle.DUST, ally.getLocation().clone().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0,
                                new Particle.DustOptions(IridescentSoul.IRIDESCENT[2], 0.9f));
                    }
                }
            }
        }

        w.spawnParticle(Particle.DUST, dest.clone().add(0, 1, 0), 26, 0.5, 0.7, 0.5, 0,
                new Particle.DustOptions(IridescentSoul.IRIDESCENT[3], 1f));
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.4f);
        w.playSound(dest, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.6f);
        w.playSound(dest, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.6f);
    }

    private static void cleanseSelf(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.POISON);
        unfear(player);
        wake(player);
    }

    /** Mirrored CUBIC bezier wings drawn on the player's back (steady fixed gradient). */
    private static void drawWings(Player player, int durTicks) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || player.isDead() || !player.isOnline() || !hasAnima(player)) {
                    cancel();
                    return;
                }
                if (t % 3 == 0) {
                    World w = player.getWorld();
                    Location back = player.getLocation().clone().add(0, 1.1, 0);
                    Vector facing = player.getLocation().getDirection().setY(0).normalize();
                    Vector right = new Vector(-facing.getZ(), 0, facing.getX()).normalize();
                    Vector up = new Vector(0, 1, 0);

                    // four cubic control points for one wing (in local right/up plane)
                    Vector p0 = new Vector(0, 0, 0);
                    Vector p1 = right.clone().multiply(0.6).add(up.clone().multiply(0.9));
                    Vector p2 = right.clone().multiply(1.4).add(up.clone().multiply(0.5));
                    Vector p3 = right.clone().multiply(1.2).add(up.clone().multiply(-0.6));

                    List<Vector> rightWing = ParticleUtils.getNthBezierPoints(14, p0, p1, p2, p3);
                    int idx = 0;
                    for (Vector v : rightWing) {
                        w.spawnParticle(Particle.DUST, back.clone().add(v), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(IridescentSoul.IRIDESCENT[idx++ % IridescentSoul.IRIDESCENT.length], 0.8f));
                    }
                    // mirror across the spine (negate the right component)
                    Vector p1m = right.clone().multiply(-0.6).add(up.clone().multiply(0.9));
                    Vector p2m = right.clone().multiply(-1.4).add(up.clone().multiply(0.5));
                    Vector p3m = right.clone().multiply(-1.2).add(up.clone().multiply(-0.6));
                    List<Vector> leftWing = ParticleUtils.getNthBezierPoints(14, p0, p1m, p2m, p3m);
                    idx = 0;
                    for (Vector v : leftWing) {
                        w.spawnParticle(Particle.DUST, back.clone().add(v), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(IridescentSoul.IRIDESCENT[idx++ % IridescentSoul.IRIDESCENT.length], 0.8f));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** A steady chrysalis cocoon marker at the anchor (DUST cocoon, no strobe). */
    private static void drawAnchor(Player player, Snapshot s, int durTicks) {
        final Location anchor = s.loc.clone();
        final World w = anchor.getWorld();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || !hasAnima(player)) {
                    cancel();
                    return;
                }
                if (t % 4 == 0) {
                    Location c = anchor.clone().add(0, 0.9, 0);
                    int idx = 0;
                    for (Vector v : ParticleUtils.getSpherePoints(0.6, 6)) {
                        w.spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(IridescentSoul.IRIDESCENT[idx++ % IridescentSoul.IRIDESCENT.length], 0.7f));
                    }
                }
                t++;
            }
        };
        task.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        s.anchorTask = task;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Drop an Anima, gain wings, then recall to undo your move.";
            case 2 -> ChatColor.GRAY + "Longer recall window.";
            case 3 -> ChatColor.GRAY + "Recall grants brief Absorption.";
            case 4 -> ChatColor.GRAY + "Faster cooldown.";
            case 5 -> ChatColor.GRAY + "Recall also cleanses party near the anchor.";
            default -> ChatColor.GRAY + "Drop an Anima, then recall to undo your move.";
        };
    }

    @Override
    public String toString() {
        return "releaseAnima";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.DRAGON_BREATH),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Release Anima]",
                ChatColor.GRAY + "Release your soul: snapshot your place and health, take",
                ChatColor.GRAY + "" + ChatColor.LIGHT_PURPLE + "wings" + ChatColor.GRAY + ", then " + ChatColor.LIGHT_PURPLE + "recall" + ChatColor.GRAY + " to your anchor — or onto",
                ChatColor.GRAY + "a Soul-Lance mark — healing toward your old self.");
    }
}
