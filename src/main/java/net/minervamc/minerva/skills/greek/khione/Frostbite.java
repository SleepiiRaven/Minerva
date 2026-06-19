package net.minervamc.minerva.skills.greek.khione;

import java.util.HashMap;
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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Khione PASSIVE + engine. Frost is a 0-100 per-enemy gauge. At 100 the enemy is Encased
 * (a brief root) and becomes Brittle; Shatterpoint detonates Brittle targets. Frost slows as
 * it fills and decays if not refreshed. No real ice blocks are ever placed (particles only).
 */
public class Frostbite extends Skill {
    public static final Color[] FROST = {
        Color.fromRGB(207, 232, 255), Color.fromRGB(156, 203, 255),
        Color.fromRGB(232, 246, 255), Color.fromRGB(127, 180, 240),
    };

    private static final Map<UUID, Double> FROST_LEVEL = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> DECAY = new HashMap<>();

    public static double getFrost(LivingEntity e) {
        return FROST_LEVEL.getOrDefault(e.getUniqueId(), 0.0);
    }

    public static boolean isBrittle(LivingEntity e) {
        return e.getScoreboardTags().contains("khioneBrittle") || e.getScoreboardTags().contains("khioneEncased");
    }

    private static double decayPerTick(int level) {
        return level >= 4 ? 0.25 : 0.4;
    }

    /**
     * Locate the living entity the player is aiming at within range (friendly-fire safe).
     * Aims at the target's CENTER (not its feet) and also accepts a point-blank foe even at a
     * steep angle, so an enemy standing right next to you is reliably picked up. Used by every
     * aimed skill across the new heritages.
     */
    public static LivingEntity frontTarget(Player p, double range) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        LivingEntity best = null;
        double bestScore = -2;
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == p) continue;
            if (le instanceof Player pl && Party.isPlayerInPlayerParty(p, pl)) continue;
            if (PlayerStats.isSummoned(p, le)) continue;
            if (le.hasMetadata("NPC")) continue;
            Vector to = le.getLocation().toVector().add(new Vector(0, le.getHeight() * 0.5, 0)).subtract(eye.toVector());
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            double dot = to.clone().normalize().dot(dir);
            boolean inCone = dot > 0.35;                     // ~70 degree aim cone
            boolean pointBlank = dist <= 3.0 && dot > -0.2;  // right beside you, roughly facing
            if (!inCone && !pointBlank) continue;
            double score = dot - dist / (range * 4);         // prefer centered + close
            if (score > bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    public static void addFrost(Player owner, LivingEntity target, double amount, int level) {
        if (target == null || target == owner || target.isDead()) return;
        if (target instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) return;
        if (PlayerStats.isSummoned(owner, target)) return;
        if (target.hasMetadata("NPC")) return;

        double cur = Math.min(100, getFrost(target) + amount);
        FROST_LEVEL.put(target.getUniqueId(), cur);

        double maxSlow = level >= 5 ? 0.5 : level >= 2 ? 0.4 : 0.3;
        int amp = (int) Math.floor((cur / 100.0) * (maxSlow / 0.15));
        if (amp > 0) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, Math.min(amp, 6)));

        Location loc = target.getLocation().clone().add(0, target.getHeight() * 0.6, 0);
        int n = (int) (cur / 12);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 2 + n, 0.3, 0.4, 0.3, 0.01);
        if (n > 0) target.getWorld().spawnParticle(Particle.DUST, loc, n, 0.3, 0.4, 0.3, 0,
                new Particle.DustOptions(FROST[(int) (Math.random() * FROST.length)], 1f));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 1.4f);

        if (cur >= 100 && !isBrittle(target)) {
            encase(owner, target, level);
        }
        startDecay(target, level);
    }

    public static void encase(Player owner, LivingEntity target, int level) {
        if (target == null || target.isDead()) return;
        FROST_LEVEL.put(target.getUniqueId(), 100.0);
        long encaseTicks = level >= 5 ? 30 : level >= 2 ? 32 : 24;
        target.addScoreboardTag("khioneEncased");
        target.addScoreboardTag("khioneBrittle");
        stun(owner, target, encaseTicks);

        Location c = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
        for (Vector v : ParticleUtils.getSpherePoints(target.getWidth() * 0.8 + 0.3, 6)) {
            target.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(FROST[(int) (Math.random() * FROST.length)], 1.2f));
        }
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.8f);

        int brittleTicks = level >= 4 ? 120 : 80;
        new BukkitRunnable() {
            @Override
            public void run() {
                target.removeScoreboardTag("khioneEncased");
                target.removeScoreboardTag("khioneBrittle");
            }
        }.runTaskLater(Minerva.getInstance(), brittleTicks);
    }

    private static void startDecay(LivingEntity target, int level) {
        BukkitRunnable old = DECAY.remove(target.getUniqueId());
        if (old != null) {
            try { old.cancel(); } catch (IllegalStateException ignored) {}
        }
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isDead() || getFrost(target) <= 0) {
                    FROST_LEVEL.remove(target.getUniqueId());
                    DECAY.remove(target.getUniqueId());
                    cancel();
                    return;
                }
                if (isBrittle(target)) return;
                double cur = getFrost(target) - decayPerTick(level);
                if (cur <= 0) {
                    FROST_LEVEL.remove(target.getUniqueId());
                    DECAY.remove(target.getUniqueId());
                    cancel();
                    return;
                }
                FROST_LEVEL.put(target.getUniqueId(), cur);
            }
        };
        r.runTaskTimer(Minerva.getInstance(), 20L, 1L);
        DECAY.put(target.getUniqueId(), r);
    }

    /** Passive hook: dealt-damage melee applies Frost. Wired in SkillListener. */
    public static void onWeaponHit(Player attacker, LivingEntity victim, int level) {
        boolean wasBrittle = isBrittle(victim);
        addFrost(attacker, victim, level >= 5 ? 22 : 18, level);
        if (level >= 5 && wasBrittle) {
            Shatterpoint.shatter(attacker, victim, 5, level);
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Attacks apply Frost. At max Frost, enemies are Encased.";
            case 2 -> ChatColor.GRAY + "Frost slows harder (40%) and Encase lasts longer.";
            case 3 -> ChatColor.GRAY + "Frost lingers longer before it decays.";
            case 4 -> ChatColor.GRAY + "Frost decays slowly; Brittle window extended.";
            case 5 -> ChatColor.GRAY + "Frost slows up to 50%; melee on a Brittle target auto-shatters.";
            default -> ChatColor.GRAY + "Attacks apply Frost.";
        };
    }

    @Override
    public String toString() {
        return "frostbite";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SNOWBALL),
                ChatColor.AQUA + "" + ChatColor.BOLD + "[Frostbite]",
                ChatColor.GRAY + "Your attacks build " + ChatColor.AQUA + "Frost" + ChatColor.GRAY + " on enemies.",
                ChatColor.GRAY + "At full Frost they are " + ChatColor.AQUA + "Encased" + ChatColor.GRAY + " (rooted) and",
                ChatColor.GRAY + "left " + ChatColor.AQUA + "Brittle" + ChatColor.GRAY + " — ready to " + ChatColor.WHITE + "Shatter" + ChatColor.GRAY + ".");
    }
}
