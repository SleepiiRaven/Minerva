package net.minervamc.minerva.skills.greek.athena;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class TacticalAgility extends Skill {

    public static final int MAX_STACKS = 5;
    public static final long STACK_EXPIRY_MS = 3500;
    public static final long SWITCH_COOLDOWN_MS = 400;
    private static final double DAMAGE_PER_STACK = 0.20;

    // Tracks the item type last used to hit an enemy per player
    private static final Map<UUID, Material> LAST_HIT_ITEM = new ConcurrentHashMap<>();

    private static final TextColor[] LABEL_COLORS = {
        TextColor.color(120, 170, 255),
        TextColor.color(80, 140, 255),
        TextColor.color(130, 90, 255),
        TextColor.color(190, 70, 220),
        TextColor.color(255, 215, 0),
    };

    private static final Color[][] PARTICLE_COLORS = {
        { Color.fromRGB(120, 170, 255) },
        { Color.fromRGB(80, 140, 255), Color.fromRGB(140, 200, 255) },
        { Color.fromRGB(130, 90, 255), Color.fromRGB(180, 140, 255) },
        { Color.fromRGB(190, 70, 220), Color.fromRGB(220, 130, 255) },
        { Color.fromRGB(255, 215, 0), Color.fromRGB(255, 230, 80), Color.fromRGB(255, 255, 150) },
    };

    public static int getStacks(Player player) {
        if (Minerva.getInstance().getCdInstance().isCooldownDone(player.getUniqueId(), "tacticalAgility")) {
            PlayerStats.getStats(player.getUniqueId()).getStackingAbilities().remove("tacticalAgility");
            return 0;
        }
        return PlayerStats.getStats(player.getUniqueId()).getStackingAbilities().getOrDefault("tacticalAgility", 0);
    }

    // Call at cast time — consumes all stacks, spawns detonation burst, returns damage multiplier.
    public static double detonateIfCharged(Player player) {
        int stacks = getStacks(player);
        if (stacks == 0) return 1.0;

        PlayerStats.getStats(player.getUniqueId()).getStackingAbilities().remove("tacticalAgility");
        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "tacticalAgility", 0L);

        Location loc = player.getLocation().add(0, 1, 0);
        Color[] colors = PARTICLE_COLORS[stacks - 1];
        int points = 16 + stacks * 8;
        double radius = 0.7 + stacks * 0.25;
        for (Vector v : ParticleUtils.getCirclePoints(radius, points)) {
            player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 0, 0, 0, 0, 0,
                new Particle.DustOptions(colors[(int)(Math.random() * colors.length)], 1.3f + stacks * 0.15f));
        }
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 6 + stacks * 3, 0.25, 0.25, 0.25, 0.08 + stacks * 0.025);
        if (stacks >= MAX_STACKS) {
            player.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.4f + stacks * 0.1f, 1.5f);
        }

        return 1.0 + stacks * DAMAGE_PER_STACK;
    }

    // Called from EntityDamageByEntityEvent when a player hits an enemy
    public static void onWeaponHit(Player player, ItemStack heldItem) {
        Skill passive = PlayerStats.getStats(player.getUniqueId()).getPassive();
        if (passive == null || !passive.toString().equals("tacticalAgility")) return;
        if (!PlayerStats.getStats(player.getUniqueId()).getPassiveActive()) return;

        // Bare hand never counts
        if (heldItem == null || heldItem.getType() == Material.AIR) return;
        Material held = heldItem.getType();

        // If stacks expired, clear last-hit memory so any weapon counts fresh
        if (getStacks(player) == 0) LAST_HIT_ITEM.remove(player.getUniqueId());

        // Same item as last hit — no stack
        if (held == LAST_HIT_ITEM.get(player.getUniqueId())) return;

        LAST_HIT_ITEM.put(player.getUniqueId(), held);

        if (!Minerva.getInstance().getCdInstance().isCooldownDone(player.getUniqueId(), "tacticalAgilitySwitch")) return;
        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "tacticalAgilitySwitch", SWITCH_COOLDOWN_MS);

        Map<String, Integer> abilities = PlayerStats.getStats(player.getUniqueId()).getStackingAbilities();
        int next = Math.min(getStacks(player) + 1, MAX_STACKS);
        abilities.put("tacticalAgility", next);
        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "tacticalAgility", STACK_EXPIRY_MS);

        onStackGained(player, next);
    }

    private static void onStackGained(Player player, int stacks) {
        Location loc = player.getLocation().add(0, 1, 0);
        Color[] colors = PARTICLE_COLORS[stacks - 1];

        for (Vector v : ParticleUtils.getCirclePoints(0.35 + stacks * 0.12, 8 + stacks * 4)) {
            player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 0, 0, 0, 0, 0,
                new Particle.DustOptions(colors[(int)(Math.random() * colors.length)], 0.9f + stacks * 0.1f));
        }

        int bonusPct = (int)(stacks * DAMAGE_PER_STACK * 100);
        String pips = "◆".repeat(stacks) + "◇".repeat(MAX_STACKS - stacks);

        if (stacks == MAX_STACKS) {
            player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.3, 0.3, 0.3, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 1.6f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.8f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
            player.sendActionBar(Component.text(pips + "  +" + bonusPct + "% DMG — RELEASE!", LABEL_COLORS[stacks - 1]));
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.25f + stacks * 0.08f, 0.9f + stacks * 0.15f);
            player.sendActionBar(Component.text(pips + "  +" + bonusPct + "% DMG", LABEL_COLORS[stacks - 1]));
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "tacticalAgility"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.IRON_SWORD)
            .setName(TextContext.formatLegacy("&lTactical Agility", false).color(TextColor.color(140, 190, 255)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Switching weapons builds &bMomentum &7stacks", false),
                TextContext.formatLegacy("&7(max 5, decay in 3.5s without switching).", false),
                TextContext.formatLegacy("&7Each stack grants &b+20% damage &7on your", false),
                TextContext.formatLegacy("&7next skill cast, then resets.", false),
                TextContext.formatLegacy("&65 stacks: &f+100% damage &6+ Speed I.", false)
            )).build();
    }
}
