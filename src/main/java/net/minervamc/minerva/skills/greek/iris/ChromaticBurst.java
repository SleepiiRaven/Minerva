package net.minervamc.minerva.skills.greek.iris;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * Iris RRL — the teamfight cash-in. Detonate every painted enemy nearby: each takes damage scaled
 * by how many OTHER colors are present around it (mass-contrast finisher), plus a brief blind. Each
 * bursts once in its own hue (single pulse — no strobe despite the blind). Higher ranks widen the
 * radius, lengthen the blind, heal Iris per color detonated, and leave the area prismatic so
 * enemies entering get auto-painted.
 */
public class ChromaticBurst extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 15500;
            case 4, 5 -> 15000;
            default -> 16000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "chromaticBurst")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "chromaticBurst", cooldown);
        cooldownAlarm(player, cooldown, "Chromatic Burst");

        double radius = switch (level) {
            case 2, 3 -> 8.0;
            case 4, 5 -> 9.0;
            default -> 7.0;
        };
        int blindTicks = switch (level) {
            case 3, 4 -> 60;
            case 5 -> 80;
            default -> 40;
        };
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);

        // gather painted enemies in range
        List<LivingEntity> painted = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (Prism.getColor(le) != -1) painted.add(le);
        }

        Set<Integer> detonatedColors = new HashSet<>();
        for (LivingEntity le : painted) {
            int mine = Prism.getColor(le);
            // count distinct OTHER colors present near this enemy
            Set<Integer> others = new HashSet<>();
            for (Entity e : le.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity other) || other == le) continue;
                int c = Prism.getColor(other);
                if (c != -1 && c != mine) others.add(c);
            }
            double dmg = 3 * others.size();
            if (dmg > 0) damage(le, dmg, player);
            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, blindTicks, 0));
            detonatedColors.add(mine);

            // single-pulse burst in this enemy's own hue
            Location c = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
            Color hue = Prism.PALETTE[mine];
            c.getWorld().spawnParticle(Particle.DUST, c, 20, 0.4, 0.5, 0.4, 0,
                    new Particle.DustOptions(hue, 1.3f));
            c.getWorld().spawnParticle(Particle.GLOW, c, 6, 0.4, 0.5, 0.4, 0);
            c.getWorld().playSound(le.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.9f, 1.0f + mine * 0.15f);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.8f, 1.3f);

        // L4: heal Iris a little per distinct color detonated
        if (fLevel >= 4 && !detonatedColors.isEmpty()) {
            double heal = 2.0 * detonatedColors.size();
            double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                    ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0;
            player.setHealth(Math.min(max, player.getHealth() + heal));
        }

        // L5: leave the area prismatic for 3s — enemies entering get auto-painted
        if (fLevel >= 5) {
            final Location center = player.getLocation().clone();
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (t >= 60) {
                        cancel();
                        return;
                    }
                    if (t % 3 == 0) {
                        for (Color c : Prism.PALETTE) {
                            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.2, 0), 2, radius * 0.45, 0.2, radius * 0.45, 0,
                                    new Particle.DustOptions(c, 1.0f));
                        }
                        int idx = 0;
                        for (Entity e : center.getWorld().getNearbyEntities(center, radius, 4, radius)) {
                            if (!(e instanceof LivingEntity le) || e == player) continue;
                            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                            if (PlayerStats.isSummoned(player, le)) continue;
                            if (Prism.getColor(le) == -1) Prism.paint(player, le, idx++);
                        }
                    }
                    t++;
                }
            }.runTaskTimer(Minerva.getInstance(), 5L, 1L);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Detonate painted enemies; damage scales with nearby colors. Blinds.";
            case 2 -> ChatColor.GRAY + "Bigger radius.";
            case 3 -> ChatColor.GRAY + "Longer blind.";
            case 4 -> ChatColor.GRAY + "Heals you per color detonated.";
            case 5 -> ChatColor.GRAY + "Leaves the area prismatic, auto-painting enemies for 3s.";
            default -> ChatColor.GRAY + "Detonate the whole painted palette.";
        };
    }

    @Override
    public String toString() {
        return "chromaticBurst";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.FIREWORK_STAR),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Chromatic Burst]",
                ChatColor.GRAY + "Detonate every painted enemy at once — each takes",
                ChatColor.GRAY + "damage per " + ChatColor.LIGHT_PURPLE + "other color" + ChatColor.GRAY + " nearby, and is blinded.",
                ChatColor.GRAY + "Your reward for a well-spread board.");
    }
}
