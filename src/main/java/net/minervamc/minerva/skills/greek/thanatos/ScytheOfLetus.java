package net.minervamc.minerva.skills.greek.thanatos;

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
import org.bukkit.util.Vector;

/**
 * Thanatos RLL — the builder/finisher. A sweeping frontal scythe arc dealing 8 damage. Against a
 * MARKED target it also accelerates the sand (−2s) and, if that drops the target below the
 * execution threshold, instantly reaps it — turning the inevitable into now when the math lines up.
 */
public class ScytheOfLetus extends Skill {
    private static final Color[] CRESCENT = {
        Color.fromRGB(225, 255, 210), Color.fromRGB(180, 230, 150), Color.fromRGB(150, 190, 120),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 5500;
            case 4, 5 -> 5000;
            default -> 6000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "scytheOfLetus")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "scytheOfLetus", cooldown);
        cooldownAlarm(player, cooldown, "Scythe of Letus");

        double range = 3.5;
        double coneDeg = switch (level) {
            case 2, 3 -> 140;
            case 4, 5 -> 160;
            default -> 120;
        };
        double cosHalf = Math.cos(Math.toRadians(coneDeg / 2.0));
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 0.9f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);

        // green-white crescent slash sweeping in front of the player (single-shot arc, fixed gradient).
        Location eye = player.getEyeLocation();
        float yaw = player.getLocation().getYaw();
        java.util.List<Vector> arc = ParticleUtils.getVerticalCirclePoints(2.4, 90, yaw, 24);
        for (int i = 0; i < arc.size(); i++) {
            Vector v = arc.get(i);
            Location at = eye.clone().add(v.clone().multiply(0.6)).add(player.getLocation().getDirection().multiply(1.2));
            player.getWorld().spawnParticle(Particle.DUST, at, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(CRESCENT[i % CRESCENT.length], 1.1f));
        }
        player.getWorld().spawnParticle(Particle.SCULK_SOUL, eye.clone().add(player.getLocation().getDirection().multiply(2.0)), 10, 0.6, 0.4, 0.6, 0.02);

        Vector look = player.getEyeLocation().getDirection().normalize();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (le.hasMetadata("NPC")) continue;

            Vector to = le.getLocation().clone().add(0, le.getHeight() / 2, 0).toVector()
                    .subtract(player.getEyeLocation().toVector());
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            if (to.normalize().dot(look) < cosHalf) continue; // outside the cone

            damage(le, 8, player);
            le.getWorld().spawnParticle(Particle.DUST, le.getLocation().clone().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0,
                    new Particle.DustOptions(CRESCENT[1], 1.0f));

            if (TheInevitable.hasTimer(le)) {
                TheInevitable.accelerate(le, 40); // rush the sand by 2s
                double threshold = switch (fLevel) {
                    case 3 -> 0.20;
                    case 4 -> 0.24;
                    case 5 -> 0.27;
                    default -> 0.15;
                };
                if (le.getHealth() <= threshold * le.getMaxHealth()) {
                    TheInevitable.reap(player, le, fLevel);
                    onReap(player, fLevel, cooldownManager);
                }
            }
        }
    }

    /** Reap rewards specific to Scythe: L4 refunds Scythe's CD, L5 heals extra + refreshes Shroud. */
    private static void onReap(Player player, int level, CooldownManager cooldownManager) {
        if (level >= 4) {
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "scytheOfLetus", 0L);
            player.sendActionBar(ChatColor.DARK_GREEN + "Scythe refunded by the reap.");
        }
        if (level >= 5) {
            double heal = 4;
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "shroudOfLetus", 0L);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Sweep a scythe; marked targets rush toward the reap.";
            case 2 -> ChatColor.GRAY + "Wider arc.";
            case 3 -> ChatColor.GRAY + "Higher reap threshold on hit.";
            case 4 -> ChatColor.GRAY + "A reap refunds Scythe's cooldown.";
            case 5 -> ChatColor.GRAY + "A reap heals you extra and refreshes Shroud.";
            default -> ChatColor.GRAY + "Sweep a scythe arc; reap marked low-HP targets.";
        };
    }

    @Override
    public String toString() {
        return "scytheOfLetus";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.NETHERITE_HOE),
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Scythe of Letus]",
                ChatColor.GRAY + "Sweep a crescent scythe in front of you. A " + ChatColor.DARK_GREEN + "marked",
                ChatColor.GRAY + "target has its sand rushed — and if it drops below the",
                ChatColor.GRAY + "threshold, it is " + ChatColor.DARK_GREEN + "reaped" + ChatColor.GRAY + " on the spot.");
    }
}
