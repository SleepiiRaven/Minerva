package net.minervamc.minerva.skills.greek.hypnos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
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

/**
 * Hypnos PASSIVE engine. Drowsiness is a 0-100 per-enemy gauge. Weapon hits and Hypnos skills
 * build it; as it fills the target slows. At 100 the target falls asleep (soft-CC that breaks on
 * damage) and a contagion runnable creeps drowsiness onto it and spreads it to enemies adjacent
 * to any sleeper. No strobing: steady blue dream-motes only.
 */
public class Sandman extends Skill {
    public static final Color[] DREAM = {
        Color.fromRGB(120, 150, 255), Color.fromRGB(95, 120, 220),
        Color.fromRGB(150, 170, 255), Color.fromRGB(70, 90, 180),
    };

    private static final Map<UUID, Double> DROWSY = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> CONTAGION = new HashMap<>();

    public static double getDrowsiness(LivingEntity target) {
        if (target == null) return 0;
        return DROWSY.getOrDefault(target.getUniqueId(), 0.0);
    }

    public static void addDrowsiness(Player owner, LivingEntity target, double amt, int level) {
        if (target == null || target == owner || target.isDead()) return;
        if (target instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) return;
        if (PlayerStats.isSummoned(owner, target)) return;
        if (target.hasMetadata("NPC")) return;
        if (isAsleep(target)) return;

        double cur = Math.min(100, getDrowsiness(target) + amt);
        DROWSY.put(target.getUniqueId(), cur);

        // slowness scales with fill (up to amp 4 at full)
        int amp = (int) Math.floor((cur / 100.0) * 4);
        if (amp > 0) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, Math.min(amp, 5)));

        // drifting dream-motes + Z glyphs sized to fill
        Location head = target.getLocation().clone().add(0, target.getHeight() + 0.4, 0);
        int n = (int) (cur / 14);
        target.getWorld().spawnParticle(Particle.DUST, head, 2 + n, 0.25, 0.18, 0.25, 0,
                new Particle.DustOptions(DREAM[(int) (Math.random() * DREAM.length)], 1f));
        if (n > 0) target.getWorld().spawnParticle(Particle.NOTE, head, n, 0.25, 0.1, 0.25, 0.6);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.4f, 0.7f);

        if (cur >= 100) {
            long sleepTicks = level >= 2 ? 40 : 32;
            sleep(owner, target, sleepTicks);
            DROWSY.put(target.getUniqueId(), 0.0);
            startContagion(owner, target, level);
        }
    }

    /** Contagion: creep +5/sec on the sleeper and spread +10/sec to enemies adjacent to a sleeper. */
    private static void startContagion(Player owner, LivingEntity seed, int level) {
        BukkitRunnable old = CONTAGION.remove(seed.getUniqueId());
        if (old != null) {
            try { old.cancel(); } catch (IllegalStateException ignored) {}
        }
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (seed.isDead() || !owner.isOnline() || !isAsleep(seed) || t >= 200) {
                    CONTAGION.remove(seed.getUniqueId());
                    cancel();
                    return;
                }
                // creep drowsiness back onto the sleeper so it does not wake too cleanly
                DROWSY.put(seed.getUniqueId(), Math.min(100, getDrowsiness(seed) + 5));
                // spread to enemies adjacent to the sleeper
                for (Entity e : seed.getNearbyEntities(3, 3, 3)) {
                    if (!(e instanceof LivingEntity le) || e == owner || e == seed) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                    if (PlayerStats.isSummoned(owner, le)) continue;
                    if (isAsleep(le)) continue;
                    addDrowsiness(owner, le, 10, level);
                }
                Location loc = seed.getLocation().clone().add(0, 0.2, 0);
                seed.getWorld().spawnParticle(Particle.DUST, loc, 6, 1.2, 0.1, 1.2, 0,
                        new Particle.DustOptions(DREAM[3], 1f));
                t += 20;
            }
        };
        r.runTaskTimer(Minerva.getInstance(), 20L, 20L);
        CONTAGION.put(seed.getUniqueId(), r);
    }

    /** Passive hook: dealt-damage melee builds Drowsiness. Wired in SkillListener. */
    public static void onWeaponHit(Player attacker, LivingEntity victim, int level) {
        addDrowsiness(attacker, victim, 18, level);
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Attacks build Drowsiness. At max, enemies fall Asleep.";
            case 2 -> ChatColor.GRAY + "Sleep lasts longer.";
            case 3 -> ChatColor.GRAY + "Drowsiness spreads more reliably between enemies.";
            case 4 -> ChatColor.GRAY + "Sleepers infect their neighbours faster.";
            case 5 -> ChatColor.GRAY + "Contagion creeps relentlessly across packed enemies.";
            default -> ChatColor.GRAY + "Attacks build Drowsiness.";
        };
    }

    @Override
    public String toString() {
        return "sandman";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SAND),
                ChatColor.BLUE + "" + ChatColor.BOLD + "[Sandman]",
                ChatColor.GRAY + "Your attacks build " + ChatColor.BLUE + "Drowsiness" + ChatColor.GRAY + " on enemies.",
                ChatColor.GRAY + "At full Drowsiness they fall " + ChatColor.BLUE + "Asleep" + ChatColor.GRAY + " — a slumber",
                ChatColor.GRAY + "that spreads to anyone nearby, but " + ChatColor.WHITE + "breaks on damage" + ChatColor.GRAY + ".");
    }
}
