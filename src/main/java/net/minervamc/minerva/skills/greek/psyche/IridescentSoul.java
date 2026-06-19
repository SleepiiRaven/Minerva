package net.minervamc.minerva.skills.greek.psyche;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Psyche PASSIVE engine. The lower Psyche's health, the more her soul-magic burns: every Psyche
 * damage skill multiplies its damage by {@link #bonusMultiplier}. At rank 5 the soul refuses to
 * scatter — a once-per-minute cheat-death keeps her at 1 HP. No strobe: a single steady DUST halo
 * marks the cheat-death, never a per-tick flash.
 */
public class IridescentSoul extends Skill {
    /** Shared iridescent pastel palette for the whole Psyche kit (spatial/fixed gradient). */
    public static final Color[] IRIDESCENT = {
        Color.fromRGB(255, 200, 235), Color.fromRGB(200, 220, 255),
        Color.fromRGB(210, 255, 230), Color.fromRGB(245, 225, 255),
    };

    private static final Map<UUID, Long> CHEAT_DEATH_CD = new HashMap<>();
    private static final long CHEAT_DEATH_MS = 60_000L;

    /** The lower the caster's current HP, the higher the multiplier. */
    public static double bonusMultiplier(Player player, int level) {
        double max = player.getMaxHealth();
        if (max <= 0) return 1.0;
        double missingFrac = 1.0 - (player.getHealth() / max);
        if (missingFrac < 0) missingFrac = 0;
        return 1.0 + missingFrac * (0.4 + 0.1 * level);
    }

    /** Rank-5 cheat-death: a lethal blow is refused once per minute, leaving Psyche at 1 HP. */
    public static void onTakeDamage(Player player, EntityDamageEvent event, int level) {
        if (level < 5) return;
        if (event.isCancelled()) return;
        if (event.getFinalDamage() < player.getHealth()) return;

        long now = System.currentTimeMillis();
        long ready = CHEAT_DEATH_CD.getOrDefault(player.getUniqueId(), 0L);
        if (now < ready) return;

        event.setCancelled(true);
        player.setHealth(1);
        CHEAT_DEATH_CD.put(player.getUniqueId(), now + CHEAT_DEATH_MS);

        Location c = player.getLocation().clone().add(0, 1, 0);
        for (Vector v : net.minervamc.minerva.utils.ParticleUtils.getSpherePoints(1.0, 7)) {
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(IRIDESCENT[(int) (Math.random() * IRIDESCENT.length)], 1.2f));
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.2f);
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "The lower your health, the more your soul-magic burns.";
            case 2 -> ChatColor.GRAY + "Stronger damage scaling from missing health.";
            case 3 -> ChatColor.GRAY + "Even stronger scaling from missing health.";
            case 4 -> ChatColor.GRAY + "Your soul empowers Psyche skills further when wounded.";
            case 5 -> ChatColor.GRAY + "Cheat death: a lethal blow leaves you at 1 HP (once per minute).";
            default -> ChatColor.GRAY + "The lower your health, the stronger your skills.";
        };
    }

    @Override
    public String toString() {
        return "iridescentSoul";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.NAUTILUS_SHELL),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Iridescent Soul]",
                ChatColor.GRAY + "Your soul shines brightest at the edge — the lower your",
                ChatColor.GRAY + "health, the more your Psyche skills " + ChatColor.LIGHT_PURPLE + "amplify" + ChatColor.GRAY + ".",
                ChatColor.GRAY + "At its peak, a lethal blow cannot scatter you.");
    }
}
