package net.minervamc.minerva.skills.greek.persephone;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Persephone PASSIVE — the engine. A regenerating pool of Pomegranate Seeds (the first charge
 * pool in Minerva). Seeds are spent by Bloom (life) and Wither (death); they regrow slowly over
 * time. At a full pool the next cast is "free" (consumeFreeCast). Six aril orbs orbit the player
 * as a diegetic resource bar — lit for available seeds, dim for spent (steady, no strobe).
 */
public class SeedsOfTheUnderworld extends Skill {
    public static final Color[] PALETTE = {
        Color.fromRGB(220, 90, 150), Color.fromRGB(150, 200, 90),
        Color.fromRGB(120, 70, 130), Color.fromRGB(90, 40, 80),
    };

    private static final int START = 6;

    private static final Map<UUID, Integer> SEEDS = new HashMap<>();
    private static final Map<UUID, Integer> REGROW_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> LEVEL = new HashMap<>();

    private static int levelOf(Player player) {
        return LEVEL.getOrDefault(player.getUniqueId(), 1);
    }

    private static int cap(int level) {
        return level >= 4 ? 7 : 6;
    }

    private static int regrowInterval(int level) {
        return level >= 2 ? 3 : 4;
    }

    public static int getSeeds(Player player) {
        return SEEDS.getOrDefault(player.getUniqueId(), START);
    }

    public static boolean spendSeeds(Player player, int n) {
        int cur = getSeeds(player);
        if (cur < n) return false;
        SEEDS.put(player.getUniqueId(), cur - n);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 0.8f, 1.1f);
        showOrbs(player, levelOf(player));
        return true;
    }

    public static void addSeed(Player player) {
        addSeedCapped(player, cap(levelOf(player)));
    }

    private static void addSeedCapped(Player player, int cap) {
        int cur = getSeeds(player);
        if (cur >= cap) return;
        SEEDS.put(player.getUniqueId(), cur + 1);
        Location loc = player.getLocation().clone().add(0, 1.0, 0);
        float pitch = 0.8f + 0.1f * Math.min(cur + 1, 6);
        player.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, pitch);
        if (cur + 1 >= cap) {
            player.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.1f);
        }
        showOrbs(player, 1);
    }

    /** Engine tick — call ~1/sec from the heritage tick loop. Regrows seeds and paints the orbs. */
    public static void tick(Player player, int level) {
        UUID id = player.getUniqueId();
        LEVEL.put(id, level);
        SEEDS.putIfAbsent(id, START);
        int rt = REGROW_TICKS.getOrDefault(id, 0) + 1;
        int interval = regrowInterval(level);
        if (rt >= interval) {
            rt = 0;
            if (getSeeds(player) < cap(level)) {
                addSeedCapped(player, cap(level));
            }
        }
        REGROW_TICKS.put(id, rt);
        showOrbs(player, level);
    }

    /** If the pool is full, returns true (the next cast is free/empowered); otherwise false. */
    public static boolean consumeFreeCast(Player player) {
        if (getSeeds(player) >= cap(levelOf(player))) {
            player.addScoreboardTag("persephoneEmpowered");
            return true;
        }
        return false;
    }

    /** Paint the orbiting aril orbs: lit = available seed, dim = spent. Steady, spatial gradient. */
    private static void showOrbs(Player player, int level) {
        int seeds = getSeeds(player);
        Location base = player.getLocation().clone().add(0, 1.1, 0);
        double radius = 0.9;
        int slots = 6;
        for (int i = 0; i < slots; i++) {
            double angle = (2 * Math.PI * i) / slots;
            Vector v = new Vector(Math.cos(angle) * radius, 0.05 * Math.sin(angle * 2), Math.sin(angle) * radius);
            Location loc = base.clone().add(v);
            boolean lit = i < seeds;
            Color c = lit ? PALETTE[0] : PALETTE[3];
            float size = lit ? 1.1f : 0.7f;
            player.getWorld().spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, size));
        }
        if (seeds >= 6) {
            // steady crimson ring when full (no flashing)
            for (Vector v : ParticleUtils.getCirclePoints(1.1, 14)) {
                player.getWorld().spawnParticle(Particle.DUST, base.clone().add(v), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(PALETTE[0], 0.8f));
            }
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Hold 6 Pomegranate Seeds; one regrows every 4s.";
            case 2 -> ChatColor.GRAY + "Seeds regrow faster (every 3s).";
            case 3 -> ChatColor.GRAY + "Stronger lifesteal and shields from your seeds.";
            case 4 -> ChatColor.GRAY + "Carry up to 7 seeds.";
            case 5 -> ChatColor.GRAY + "A full pool makes your next cast free and empowered.";
            default -> ChatColor.GRAY + "Hold 6 regenerating Pomegranate Seeds.";
        };
    }

    @Override
    public String toString() {
        return "seedsOfTheUnderworld";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SWEET_BERRIES),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Seeds of the Underworld]",
                ChatColor.GRAY + "Hold " + ChatColor.LIGHT_PURPLE + "6 Pomegranate Seeds" + ChatColor.GRAY + " — a regenerating",
                ChatColor.GRAY + "pool spent on " + ChatColor.GREEN + "Bloom" + ChatColor.GRAY + " (life) or " + ChatColor.DARK_PURPLE + "Wither" + ChatColor.GRAY + " (death).",
                ChatColor.GRAY + "Six aril orbs orbit you, lit for each seed you can spend.");
    }
}
