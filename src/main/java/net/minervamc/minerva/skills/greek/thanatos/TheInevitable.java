package net.minervamc.minerva.skills.greek.thanatos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.minervamc.minerva.utils.ItemUtils;

/**
 * Thanatos PASSIVE + engine. Damage marks an enemy with a Death Timer (160t = 8s). The timer
 * counts down with a steady hourglass DUST glyph above the target's head that thins as the sand
 * runs out. When the sand empties, if the target sits below the execution threshold (default 15%,
 * raised by Knell / levels) it is REAPED — executed, healing the owner and shaving Thanatos's
 * cooldowns. Toll halves remaining timers; Scythe accelerates the sand. No real blocks placed.
 */
public class TheInevitable extends Skill {
    public static final Color[] BONE = {
        Color.fromRGB(225, 225, 210), Color.fromRGB(150, 190, 120),
        Color.fromRGB(90, 120, 70), Color.fromRGB(30, 30, 30),
    };

    private static final int START_TICKS = 160;
    private static final double DEFAULT_THRESHOLD = 0.15;

    // Thanatos's own active cooldown keys — reaping shaves time off all of them.
    private static final String[] THANATOS_KEYS = {"shroudOfLetus", "knell", "scytheOfLetus", "tollTheBell"};

    private static final List<DeathTimer> TIMERS = new ArrayList<>();

    /** A single ticking Death Timer bound to one target. */
    private static class DeathTimer {
        UUID owner;
        LivingEntity target;
        int remaining;
        double thresholdPct;
        BukkitRunnable task;
    }

    private static DeathTimer find(LivingEntity target) {
        if (target == null) return null;
        for (DeathTimer dt : TIMERS) {
            if (dt.target != null && dt.target.getUniqueId().equals(target.getUniqueId())) return dt;
        }
        return null;
    }

    private static boolean isAlly(Player owner, LivingEntity target) {
        if (target == null || target == owner) return true;
        if (target.hasMetadata("NPC")) return true;
        if (target instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) return true;
        if (PlayerStats.isSummoned(owner, target)) return true;
        return false;
    }

    /** Apply or refresh a Death Timer on the target (length resets, threshold may rise by level). */
    public static void applyTimer(Player owner, org.bukkit.entity.LivingEntity target, int level) {
        if (owner == null || target == null || target.isDead()) return;
        if (isAlly(owner, target)) return;

        double threshold = switch (level) {
            case 4 -> 0.20;
            case 5 -> 0.22;
            case 3 -> 0.18;
            default -> DEFAULT_THRESHOLD;
        };

        DeathTimer existing = find(target);
        if (existing != null) {
            existing.owner = owner.getUniqueId();
            existing.remaining = START_TICKS;
            existing.thresholdPct = Math.max(existing.thresholdPct, threshold);
            return;
        }

        DeathTimer dt = new DeathTimer();
        dt.owner = owner.getUniqueId();
        dt.target = target;
        dt.remaining = START_TICKS;
        dt.thresholdPct = threshold;
        TIMERS.add(dt);

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.5f, 0.7f);

        dt.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (dt.target == null || dt.target.isDead() || dt.remaining <= 0) {
                    expire(dt);
                    TIMERS.remove(dt);
                    cancel();
                    return;
                }

                // steady hourglass glyph above the head — thins as the sand drops, never strobes.
                double frac = dt.remaining / (double) START_TICKS;
                Location loc = dt.target.getLocation().clone().add(0, dt.target.getHeight() + 0.55, 0);
                int sand = Math.max(1, (int) Math.ceil(frac * 5));
                // upper bulb (top): fills with remaining sand
                for (int i = 0; i < sand; i++) {
                    double y = 0.18 + i * 0.06;
                    double r = 0.18 * (1.0 - i / 6.0);
                    Vector v = new Vector(Math.cos(i * 2.4) * r, y, Math.sin(i * 2.4) * r);
                    dt.target.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(BONE[0], 0.9f));
                }
                // pinched waist — a single steady mote marking the throat of the glass
                dt.target.getWorld().spawnParticle(Particle.DUST, loc.clone(), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(BONE[2], 0.7f));
                // lower bulb (bottom): grows with elapsed sand
                int fallen = 5 - sand;
                for (int i = 0; i < fallen; i++) {
                    double y = -0.18 - i * 0.06;
                    double r = 0.18 * (1.0 - i / 6.0);
                    Vector v = new Vector(Math.cos(i * 2.4 + 1.0) * r, y, Math.sin(i * 2.4 + 1.0) * r);
                    dt.target.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(BONE[1], 0.8f));
                }
                if (dt.remaining % 20 == 0) {
                    dt.target.getWorld().spawnParticle(Particle.SOUL, loc.clone(), 1, 0.05, 0.05, 0.05, 0.005);
                }

                // Shroud feeds off every ticking timer: heal the owner near a marked enemy.
                Player ownerPl = Bukkit.getPlayer(dt.owner);
                if (ownerPl != null && ownerPl.isOnline()) {
                    ShroudOfLetus.onTimerTick(ownerPl, dt.target);
                }

                dt.remaining--;
                if (dt.remaining <= 0) {
                    expire(dt);
                    TIMERS.remove(dt);
                    cancel();
                }
            }
        };
        dt.task.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    /** On natural expiry, reap if the target is below threshold; otherwise the clock simply runs out. */
    private static void expire(DeathTimer dt) {
        if (dt.target == null || dt.target.isDead()) return;
        Player owner = Bukkit.getPlayer(dt.owner);
        double maxHp = dt.target.getMaxHealth();
        if (owner != null && owner.isOnline() && dt.target.getHealth() <= dt.thresholdPct * maxHp) {
            // resolve the natural reap at the threshold the timer carried.
            doReap(owner, dt.target, 1, false);
        }
    }

    /** Speed up the sand on a marked target (shave ticks off the remaining timer). */
    public static void accelerate(org.bukkit.entity.LivingEntity target, int ticks) {
        DeathTimer dt = find(target);
        if (dt == null) return;
        dt.remaining = Math.max(1, dt.remaining - ticks);
        if (target != null && !target.isDead()) {
            target.getWorld().spawnParticle(Particle.DUST,
                    target.getLocation().clone().add(0, target.getHeight() + 0.4, 0), 6, 0.15, 0.1, 0.15, 0,
                    new Particle.DustOptions(BONE[1], 0.8f));
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRAVEL_FALL, 0.6f, 0.8f);
        }
    }

    /** Toll the Bell: halve the remaining timers of every marked enemy within radius of the owner. */
    public static void halveTimers(Player owner, double radius) {
        if (owner == null) return;
        Location c = owner.getLocation();
        for (DeathTimer dt : TIMERS) {
            if (dt.target == null || dt.target.isDead()) continue;
            if (!dt.target.getWorld().equals(owner.getWorld())) continue;
            if (dt.target.getLocation().distance(c) > radius) continue;
            dt.remaining = Math.max(1, dt.remaining / 2);
            Location loc = dt.target.getLocation().clone().add(0, dt.target.getHeight() + 0.5, 0);
            dt.target.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.2, 0.25, 0.2, 0,
                    new Particle.DustOptions(BONE[1], 0.9f));
            dt.target.getWorld().spawnParticle(Particle.SOUL, loc, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    public static boolean hasTimer(org.bukkit.entity.LivingEntity target) {
        return find(target) != null;
    }

    /** Public reap entry: execute a marked target now (used by Scythe and natural expiry). */
    public static void reap(Player owner, org.bukkit.entity.LivingEntity target, int level) {
        doReap(owner, target, level, true);
    }

    private static void doReap(Player owner, LivingEntity target, int level, boolean fromActive) {
        if (owner == null || target == null || target.isDead()) return;
        if (isAlly(owner, target)) return;

        DeathTimer dt = find(target);
        if (dt != null) {
            TIMERS.remove(dt);
            if (dt.task != null) {
                try { dt.task.cancel(); } catch (IllegalStateException ignored) {}
            }
        }

        Location c = target.getLocation().clone().add(0, target.getHeight() / 2, 0);

        // the execution — huge magic damage to finish a target already past the threshold.
        damage(target, 1000, owner, true, false);

        // owner siphons life from the harvested soul.
        double heal = 6 + (level >= 4 ? 2 : 0);
        double newHp = Math.min(owner.getMaxHealth(), owner.getHealth() + heal);
        owner.setHealth(newHp);

        // reaping shaves time off all Thanatos cooldowns (snowball into the next).
        CooldownManager cd = Minerva.getInstance().getCdInstance();
        long shave = level >= 2 ? 2500 : 1500;
        for (String key : THANATOS_KEYS) {
            long rem = cd.getCooldownLeft(owner.getUniqueId(), key);
            if (rem > 0) {
                cd.setCooldownFromNow(owner.getUniqueId(), key, Math.max(0L, rem - shave));
            }
        }

        // rising soul wisp — single-shot, no strobe.
        for (Vector v : net.minervamc.minerva.utils.ParticleUtils.getSpherePoints(0.7, 6)) {
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(BONE[0], 1.1f));
        }
        c.getWorld().spawnParticle(Particle.SOUL, c, 14, 0.25, 0.6, 0.25, 0.04);
        c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 0.4, 0), 8, 0.2, 0.5, 0.2, 0.03);
        c.getWorld().playSound(c, Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        c.getWorld().playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1f, 0.7f);
        owner.sendActionBar(ChatColor.DARK_GREEN + "Reaped.");
    }

    /** Every marked enemy currently within radius of the owner (used by Shroud L5 scaling). */
    public static java.util.List<org.bukkit.entity.LivingEntity> markedNear(Player owner, double radius) {
        java.util.List<org.bukkit.entity.LivingEntity> out = new ArrayList<>();
        if (owner == null) return out;
        Location c = owner.getLocation();
        for (Iterator<DeathTimer> it = TIMERS.iterator(); it.hasNext(); ) {
            DeathTimer dt = it.next();
            if (dt.target == null || dt.target.isDead()) continue;
            if (!dt.target.getWorld().equals(owner.getWorld())) continue;
            if (dt.target.getLocation().distance(c) <= radius) out.add(dt.target);
        }
        return out;
    }

    /** Passive hook: dealt-damage melee applies/refreshes a Death Timer. Wired in SkillListener. */
    public static void onWeaponHit(Player attacker, org.bukkit.entity.LivingEntity victim, int level) {
        applyTimer(attacker, victim, level);
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Your hits start an 8s Death Timer. At zero, low-HP foes are reaped.";
            case 2 -> ChatColor.GRAY + "Reaping shaves more time off your cooldowns.";
            case 3 -> ChatColor.GRAY + "Death Timers carry a higher execution threshold.";
            case 4 -> ChatColor.GRAY + "Higher threshold; reaps heal you more.";
            case 5 -> ChatColor.GRAY + "Even higher threshold — the inevitable comes sooner.";
            default -> ChatColor.GRAY + "Your hits start a Death Timer.";
        };
    }

    @Override
    public String toString() {
        return "theInevitable";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BONE),
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[The Inevitable]",
                ChatColor.GRAY + "Your strikes mark enemies with a " + ChatColor.DARK_GREEN + "Death Timer" + ChatColor.GRAY + ".",
                ChatColor.GRAY + "An hourglass drains above their head; when the sand",
                ChatColor.GRAY + "runs out, the weak are " + ChatColor.DARK_GREEN + "reaped" + ChatColor.GRAY + " — and you are fed.");
    }
}
