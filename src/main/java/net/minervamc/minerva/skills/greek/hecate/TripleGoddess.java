package net.minervamc.minerva.skills.greek.hecate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Hecate PASSIVE + engine. Hecate has Three Faces — Maiden, Mother, Crone — that the whole kit
 * reads from. Shift Face cycles the current Face; every other skill behaves completely differently
 * depending on which Face is active. The passive grants a steady Face-matched aura and orbits three
 * slow recoloured runes around the caster, so being in a Face always pays off.
 *
 * Palettes (spatial / steady — never strobing):
 *   MAIDEN silver  #D8E0FF
 *   MOTHER green-gold #A8E06A / #FFE9A0
 *   CRONE  violet-black #5A2E8C / #1A0F2E
 */
public class TripleGoddess extends Skill {
    public enum Face { MAIDEN, MOTHER, CRONE }

    public static final Color MAIDEN_SILVER = Color.fromRGB(216, 224, 255);
    public static final Color MOTHER_GREEN = Color.fromRGB(168, 224, 106);
    public static final Color MOTHER_GOLD = Color.fromRGB(255, 233, 160);
    public static final Color CRONE_VIOLET = Color.fromRGB(90, 46, 140);
    public static final Color CRONE_BLACK = Color.fromRGB(26, 15, 46);

    private static final Map<UUID, Face> FACE = new HashMap<>();
    private static final Map<UUID, Double> RUNE_ANGLE = new HashMap<>();

    /** The Face a player is currently wearing. Defaults to MAIDEN. */
    public static Face getFace(Player player) {
        return FACE.getOrDefault(player.getUniqueId(), Face.MAIDEN);
    }

    public static void setFace(Player player, Face face) {
        FACE.put(player.getUniqueId(), face);
    }

    /** Advance the Face: Maiden -> Mother -> Crone -> Maiden. */
    public static void cycle(Player player) {
        Face next = switch (getFace(player)) {
            case MAIDEN -> Face.MOTHER;
            case MOTHER -> Face.CRONE;
            case CRONE -> Face.MAIDEN;
        };
        FACE.put(player.getUniqueId(), next);
    }

    /** Steady single colour for the active Face (used by other Hecate skills for trails/impacts). */
    public static Color faceColor(Face face) {
        return switch (face) {
            case MAIDEN -> MAIDEN_SILVER;
            case MOTHER -> MOTHER_GREEN;
            case CRONE -> CRONE_VIOLET;
        };
    }

    /**
     * Passive tick — called once per second by the heritage engine. Applies the current-Face aura
     * and orbits three slow recoloured rune points around the player.
     */
    public static void tick(Player player, int level) {
        Face face = getFace(player);

        switch (face) {
            case MAIDEN -> {
                int amp = level >= 4 ? 1 : 0;
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, amp));
                List<Player> party = Party.partyList(player);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(player.getLocation()) > 36) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, amp));
                    }
                }
            }
            case MOTHER -> {
                int amp = level >= 4 ? 1 : 0;
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, amp));
                List<Player> party = Party.partyList(player);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(player.getLocation()) > 36) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, amp));
                    }
                }
            }
            case CRONE -> {
                int amp = level >= 4 ? 1 : 0;
                for (Entity e : player.getNearbyEntities(6, 6, 6)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, amp));
                }
            }
        }

        // three slow orbiting runes, recoloured to the active Face (steady spatial gradient)
        double base = RUNE_ANGLE.getOrDefault(player.getUniqueId(), 0.0) + 0.35;
        RUNE_ANGLE.put(player.getUniqueId(), base);
        Location center = player.getLocation().clone().add(0, 1.1, 0);
        double radius = 1.0;
        for (int i = 0; i < 3; i++) {
            double angle = base + (i * (2 * Math.PI / 3));
            Vector off = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Color color = switch (face) {
                case MAIDEN -> MAIDEN_SILVER;
                case MOTHER -> (i % 2 == 0) ? MOTHER_GREEN : MOTHER_GOLD;
                case CRONE -> (i % 2 == 0) ? CRONE_VIOLET : CRONE_BLACK;
            };
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(off), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 1.1f));
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Your current Face grants a steady aura to you and allies.";
            case 2 -> ChatColor.GRAY + "The Face aura reaches a touch further.";
            case 3 -> ChatColor.GRAY + "Runes orbit faster; your Face is easier to read.";
            case 4 -> ChatColor.GRAY + "Stronger Face auras (Speed/Regen/Weakness II).";
            case 5 -> ChatColor.GRAY + "Mastery of the Three Faces.";
            default -> ChatColor.GRAY + "Your current Face grants a steady aura.";
        };
    }

    @Override
    public String toString() {
        return "tripleGoddess";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.AMETHYST_CLUSTER),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Triple Goddess]",
                ChatColor.GRAY + "You wear one of three " + ChatColor.LIGHT_PURPLE + "Faces" + ChatColor.GRAY + ":",
                ChatColor.WHITE + "Maiden" + ChatColor.GRAY + " (Speed), " + ChatColor.GREEN + "Mother" + ChatColor.GRAY + " (Regen),",
                ChatColor.DARK_PURPLE + "Crone" + ChatColor.GRAY + " (Weakness). The whole kit reads your Face.");
    }
}
