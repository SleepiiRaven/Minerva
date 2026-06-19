package net.minervamc.minerva.skills.greek.janus;

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

/**
 * Janus PASSIVE engine — the law of the threshold. Anything that passes through one of your
 * Doorway portals is changed by the crossing: party allies receive Beginnings (a cleanse plus
 * speed and, at rank, a small shield) while enemies receive Endings (slow plus weakness and,
 * at rank, reduced damage). Doorway calls applyTransition every time an entity teleports.
 * gold = #FFCE5A (beginnings), indigo = #3B2E8C (endings).
 */
public class GodOfTransitions extends Skill {
    public static final Color GOLD = Color.fromRGB(255, 206, 90);
    public static final Color INDIGO = Color.fromRGB(59, 46, 140);

    /**
     * REQUIRED HOOK (verbatim signature). Called by Doorway whenever a traveler crosses a portal.
     * Allies (the owner and party members) are cleansed and sped up (Beginnings); enemies are
     * slowed and weakened (Endings). Higher levels strengthen both sides.
     */
    public static void applyTransition(Player owner, org.bukkit.entity.Entity traveler, int level) {
        if (traveler == null || owner == null) return;

        boolean ally = traveler == owner
                || (traveler instanceof Player p && Party.isPlayerInPlayerParty(owner, p))
                || PlayerStats.isSummoned(owner, traveler);

        if (ally) {
            if (traveler instanceof LivingEntity le) {
                applyBeginnings(le, level);
            }
            sparkle(traveler.getLocation().clone().add(0, traveler.getHeight() * 0.6, 0), GOLD, Particle.WAX_ON);
            traveler.getWorld().playSound(traveler.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
            return;
        }

        if (traveler instanceof LivingEntity le) {
            applyEndings(owner, le, level);
            sparkle(le.getLocation().clone().add(0, le.getHeight() * 0.6, 0), INDIGO, Particle.WITCH);
            le.getWorld().playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.7f);
        }
    }

    /** Beginnings: cleanse negative effects + speed (+ shield at L4+). 2s baseline. */
    public static void applyBeginnings(LivingEntity le, int level) {
        // cleanse a handful of common debuffs (a fresh start past the threshold)
        le.removePotionEffect(PotionEffectType.SLOWNESS);
        le.removePotionEffect(PotionEffectType.WEAKNESS);
        le.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        le.removePotionEffect(PotionEffectType.BLINDNESS);
        le.removePotionEffect(PotionEffectType.WITHER);
        le.removePotionEffect(PotionEffectType.NAUSEA);

        int speedAmp = level >= 2 ? 1 : 0;
        le.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, speedAmp));
        if (level >= 4) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60, 0));
        }
    }

    /** Endings: slow + weakness (+ reduced damage at L3+). 2s baseline. */
    public static void applyEndings(Player owner, LivingEntity le, int level) {
        if (le == owner) return;
        if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) return;
        if (PlayerStats.isSummoned(owner, le)) return;
        if (le.hasMetadata("NPC")) return;

        int amp = level >= 2 ? 1 : 0;
        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, amp));
        le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, amp));
        if (level >= 3) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 1));
        }
    }

    private static void sparkle(Location loc, Color color, Particle motes) {
        loc.getWorld().spawnParticle(motes, loc, 12, 0.4, 0.5, 0.4, 0.02);
        for (int i = 0; i < 6; i++) {
            loc.getWorld().spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 1.1f));
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Allies through your doors gain Beginnings; enemies gain Endings.";
            case 2 -> ChatColor.GRAY + "Beginnings and Endings grow stronger.";
            case 3 -> ChatColor.GRAY + "Endings also saps an enemy's strength further.";
            case 4 -> ChatColor.GRAY + "Beginnings grants a small shield.";
            case 5 -> ChatColor.GRAY + "The threshold's law is at its peak.";
            default -> ChatColor.GRAY + "Crossing your doors changes the traveler.";
        };
    }

    @Override
    public String toString() {
        return "godOfTransitions";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.OAK_DOOR),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[God of Transitions]",
                ChatColor.GRAY + "Every crossing has consequence. Allies through",
                ChatColor.GRAY + "your doors gain " + ChatColor.GOLD + "Beginnings" + ChatColor.GRAY + "; enemies suffer",
                ChatColor.GRAY + "" + ChatColor.DARK_PURPLE + "Endings" + ChatColor.GRAY + " — bait your foes through the threshold.");
    }
}
