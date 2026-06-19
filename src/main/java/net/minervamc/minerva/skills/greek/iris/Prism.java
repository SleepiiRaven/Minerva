package net.minervamc.minerva.skills.greek.iris;

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
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Iris PASSIVE + engine. Defines the Color system: every painted enemy carries one of a 4-color
 * palette (Red/Yellow/Green/Blue), shown as a steady DUST orb floating above the head. Iris's
 * damage skills gain bonus per nearby enemy of a DIFFERENT color (the Contrast rule). Colors fade
 * after ~6s if not refreshed. No real blocks placed (particles only); markers are steady, never
 * strobing.
 */
public class Prism extends Skill {
    public static final Color[] PALETTE = {
        Color.fromRGB(235, 64, 52),   // RED
        Color.fromRGB(240, 214, 50),  // YELLOW
        Color.fromRGB(64, 200, 84),   // GREEN
        Color.fromRGB(58, 120, 240),  // BLUE
    };

    private static final Map<UUID, Integer> COLOR = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> MARKERS = new HashMap<>();

    /** Paint a target with a specific palette index (friendly-fire safe). Spawns/refreshes its marker. */
    public static void paint(Player owner, LivingEntity target, int colorIdx) {
        if (target == null || target == owner || target.isDead()) return;
        if (target instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) return;
        if (PlayerStats.isSummoned(owner, target)) return;
        if (target.hasMetadata("NPC")) return;

        int idx = ((colorIdx % PALETTE.length) + PALETTE.length) % PALETTE.length;
        int prev = COLOR.getOrDefault(target.getUniqueId(), -1);
        COLOR.put(target.getUniqueId(), idx);

        if (prev != idx) {
            float pitch = 0.8f + idx * 0.18f;
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, pitch);
        }
        startMarker(target);
    }

    /** Get the current palette index of a target, or -1 if unpainted. */
    public static int getColor(LivingEntity target) {
        if (target == null) return -1;
        return COLOR.getOrDefault(target.getUniqueId(), -1);
    }

    /** Cycle the victim's color to the next in the palette (used by basic attacks). */
    public static void paintNext(Player owner, LivingEntity target) {
        int cur = getColor(target);
        paint(owner, target, cur < 0 ? 0 : cur + 1);
    }

    /** Number of nearby living entities within radius whose color differs from the target (and is painted). */
    public static int contrastCount(LivingEntity target, double radius) {
        if (target == null) return 0;
        int mine = getColor(target);
        if (mine == -1) return 0;
        int count = 0;
        for (Entity e : target.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || le == target) continue;
            int c = getColor(le);
            if (c != -1 && c != mine) count++;
        }
        return count;
    }

    /** Passive hook: a basic-attack hit cycles the victim's color, spreading the palette. */
    public static void onWeaponHit(Player attacker, LivingEntity victim, int level) {
        paintNext(attacker, victim);
    }

    private static void startMarker(LivingEntity target) {
        BukkitRunnable old = MARKERS.remove(target.getUniqueId());
        if (old != null) {
            try { old.cancel(); } catch (IllegalStateException ignored) {}
        }
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 120 || target.isDead() || getColor(target) == -1) {
                    COLOR.remove(target.getUniqueId());
                    MARKERS.remove(target.getUniqueId());
                    cancel();
                    return;
                }
                if (t % 4 == 0) {
                    int idx = getColor(target);
                    Color color = PALETTE[idx];
                    Location loc = target.getLocation().clone().add(0, target.getHeight() + 0.45, 0);
                    target.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(color, 1.1f));
                }
                t++;
            }
        };
        r.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        MARKERS.put(target.getUniqueId(), r);
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Attacks paint enemies a Color; damage skills scale with contrast.";
            case 2 -> ChatColor.GRAY + "Higher contrast bonus per differently-colored enemy.";
            case 3 -> ChatColor.GRAY + "The palette is fuller, allowing more clashing colors.";
            case 4 -> ChatColor.GRAY + "Attacks paint a fresh color, making spreads easier.";
            case 5 -> ChatColor.GRAY + "Max-contrast hits briefly blind the target.";
            default -> ChatColor.GRAY + "Attacks paint enemies a Color.";
        };
    }

    @Override
    public String toString() {
        return "prism";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.GLASS),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Prism]",
                ChatColor.GRAY + "Your attacks paint enemies a " + ChatColor.LIGHT_PURPLE + "Color" + ChatColor.GRAY + ".",
                ChatColor.GRAY + "Damage skills deal bonus per nearby enemy of a",
                ChatColor.GRAY + "" + ChatColor.LIGHT_PURPLE + "different color" + ChatColor.GRAY + " — spread a clashing rainbow.");
    }
}
