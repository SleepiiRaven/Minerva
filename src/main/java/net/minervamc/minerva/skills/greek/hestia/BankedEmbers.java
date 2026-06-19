package net.minervamc.minerva.skills.greek.hestia;

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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Hestia PASSIVE engine — the Hearthfire Bank. While the player is safe (no damage taken for a
 * few seconds) they slowly bank Embers; near the Hearth they bank twice as fast. Embers are the
 * fuel the active kit spends: bigger pyres, longer veils, stronger domes. Patience loads the
 * magazine. No real fire is ever placed (particles only).
 */
public class BankedEmbers extends Skill {
    public static final Color[] HEARTH = {
        Color.fromRGB(255, 233, 160), Color.fromRGB(255, 180, 80),
        Color.fromRGB(255, 138, 60), Color.fromRGB(200, 90, 30),
    };

    private static final Map<UUID, Integer> EMBERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_DMG = new HashMap<>();

    private static int cap(int level) {
        return level >= 2 ? 18 : 14;
    }

    public static int getEmbers(Player player) {
        return EMBERS.getOrDefault(player.getUniqueId(), 0);
    }

    public static void addEmbers(Player player, int n) {
        if (n <= 0) return;
        int cur = getEmbers(player) + n;
        EMBERS.put(player.getUniqueId(), cur);
    }

    public static boolean spendEmbers(Player player, int n) {
        if (n <= 0) return true;
        int cur = getEmbers(player);
        if (cur < n) return false;
        EMBERS.put(player.getUniqueId(), cur - n);
        return true;
    }

    public static int spendAll(Player player) {
        int cur = getEmbers(player);
        EMBERS.put(player.getUniqueId(), 0);
        return cur;
    }

    /** External hook: call when the player takes damage to pause banking for a few seconds. */
    public static void onTakeDamage(Player player) {
        LAST_DMG.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * External hook: called once per second. If recently hurt, banking is paused. Otherwise add
     * an Ember (two near the Hearth), capped, and draw a steady ring of warm motes sized to the
     * current bank.
     */
    public static void tick(Player player, int level) {
        if (player == null || !player.isOnline() || player.isDead()) return;

        long last = LAST_DMG.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < 3000) {
            return;
        }

        int gain = TendTheHearth.nearHearth(player, 5) ? 2 : 1;
        int max = cap(level);
        int cur = getEmbers(player);
        if (cur < max) {
            cur = Math.min(max, cur + gain);
            EMBERS.put(player.getUniqueId(), cur);
        }

        if (cur <= 0) return;

        // Steady warm ring of motes sized to the ember count (no strobe, fixed spatial gradient).
        Location loc = player.getLocation().clone().add(0, 0.15, 0);
        double radius = 0.6 + Math.min(cur, max) * 0.06;
        int pts = Math.min(20, 6 + cur);
        java.util.List<Vector> circle = ParticleUtils.getCirclePoints(radius, pts);
        for (int i = 0; i < circle.size(); i++) {
            Color c = HEARTH[i % HEARTH.length];
            player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(circle.get(i)), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, 0.9f));
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Bank Embers while safe (caps at 14). Spend them on your actives.";
            case 2 -> ChatColor.GRAY + "Larger Ember bank (caps at 18).";
            case 3 -> ChatColor.GRAY + "Banking continues steadily — patience pays off.";
            case 4 -> ChatColor.GRAY + "Your full kit scales harder with banked Embers.";
            case 5 -> ChatColor.GRAY + "Mastery of the hearth — a full bank fuels devastating actives.";
            default -> ChatColor.GRAY + "Bank Embers while safe and spend them on your actives.";
        };
    }

    @Override
    public String toString() {
        return "bankedEmbers";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.CAMPFIRE),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Banked Embers]",
                ChatColor.GRAY + "While safe, slowly bank " + ChatColor.GOLD + "Embers" + ChatColor.GRAY + " — twice as",
                ChatColor.GRAY + "fast near your " + ChatColor.GOLD + "Hearth" + ChatColor.GRAY + ". Taking damage pauses it.",
                ChatColor.GRAY + "Your active skills " + ChatColor.GOLD + "spend" + ChatColor.GRAY + " Embers to grow stronger.");
    }
}
