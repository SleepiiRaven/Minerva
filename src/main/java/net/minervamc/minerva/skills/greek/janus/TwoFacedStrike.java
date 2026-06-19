package net.minervamc.minerva.skills.greek.janus;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.janus.Doorway.Portal;
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
 * Janus RLL — strike in front, and mirror the same blow out of your Exit portal at the same
 * instant. An enemy caught by BOTH instances (standing in a portal mouth) takes +50% bonus.
 * At L3 the Entry portal erupts too (three angles). gold arc at you, indigo arcs at the gates.
 */
public class TwoFacedStrike extends Skill {
    public static final Color GOLD = Color.fromRGB(255, 206, 90);
    public static final Color INDIGO = Color.fromRGB(59, 46, 140);

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 6500;
            case 4, 5 -> 6000;
            default -> 7000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "twoFacedStrike")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "twoFacedStrike", cooldown);
        cooldownAlarm(player, cooldown, "Two-Faced Strike");

        double dmg = switch (level) {
            case 2, 3 -> 6;
            case 4, 5 -> 8;
            default -> 6;
        };
        final int fLevel = level;

        // who got hit where, to compute the both-hit bonus
        Set<UUID> hitFront = new HashSet<>();
        Set<UUID> hitMirror = new HashSet<>();

        // FRONT strike (gold arc at the player)
        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection();
        goldArc(player.getLocation().clone().add(0, 1, 0), dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        for (Entity e : player.getNearbyEntities(3, 3, 3)) {
            if (!isHittable(player, e)) continue;
            LivingEntity le = (LivingEntity) e;
            Vector to = le.getLocation().toVector().subtract(origin.toVector());
            if (to.lengthSquared() < 0.01) {
                hitFront.add(le.getUniqueId());
                continue;
            }
            if (to.normalize().dot(dir) > 0.3) {
                hitFront.add(le.getUniqueId());
            }
        }

        Portal portal = Doorway.portalOf(player);

        // MIRROR strike at the Exit portal (indigo arc)
        if (portal != null && portal.exit != null) {
            mirrorStrike(player, portal.exit, fLevel, hitMirror);
        }
        // L3: a second mirror erupts from the Entry portal too
        if (fLevel >= 3 && portal != null && portal.entry != null) {
            mirrorStrike(player, portal.entry, fLevel, hitMirror);
        }

        // resolve damage. Both-hit -> +50% and (L5) a brief stun.
        Set<UUID> all = new HashSet<>();
        all.addAll(hitFront);
        all.addAll(hitMirror);
        for (Entity e : nearbyUnion(player, portal)) {
            if (!isHittable(player, e)) continue;
            LivingEntity le = (LivingEntity) e;
            UUID id = le.getUniqueId();
            if (!all.contains(id)) continue;

            boolean both = hitFront.contains(id) && hitMirror.contains(id);
            double total = both ? dmg * 1.5 : dmg;
            damage(le, total, player);
            if (fLevel >= 2 && hitMirror.contains(id)) {
                GodOfTransitions.applyEndings(player, le, fLevel);
            }
            if (both) {
                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().clone().add(0, 1, 0), 16, 0.3, 0.4, 0.3, 0.2);
                le.getWorld().playSound(le.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.1f);
                if (fLevel >= 5) {
                    stun(player, le, 14L);
                }
            }
        }
    }

    /** Erupt the strike at a portal location, recording who it caught. */
    private static void mirrorStrike(Player player, Location at, int level, Set<UUID> hitMirror) {
        indigoArc(at.clone().add(0, 1, 0));
        at.getWorld().playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        for (Entity e : at.getWorld().getNearbyEntities(at, 3, 3, 3)) {
            if (!isHittable(player, e)) continue;
            hitMirror.add(e.getUniqueId());
        }
    }

    /** Combined candidate set: entities near the player and near either portal node. */
    private static Set<Entity> nearbyUnion(Player player, Portal portal) {
        Set<Entity> set = new HashSet<>(player.getNearbyEntities(3, 3, 3));
        if (portal != null) {
            if (portal.exit != null) set.addAll(portal.exit.getWorld().getNearbyEntities(portal.exit, 3, 3, 3));
            if (portal.entry != null) set.addAll(portal.entry.getWorld().getNearbyEntities(portal.entry, 3, 3, 3));
        }
        return set;
    }

    private static boolean isHittable(Player player, Entity e) {
        if (!(e instanceof LivingEntity le) || e == player) return false;
        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) return false;
        if (PlayerStats.isSummoned(player, le)) return false;
        if (le.hasMetadata("NPC")) return false;
        return true;
    }

    private static void goldArc(Location center, Vector dir) {
        float yaw = dirToYaw(dir);
        for (Vector v : ParticleUtils.getVerticalCirclePoints(1.6, 90f, yaw, 18)) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(GOLD, 1.1f));
        }
    }

    private static void indigoArc(Location center) {
        for (Vector v : ParticleUtils.getVerticalCirclePoints(1.6, 90f, center.getYaw(), 18)) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(INDIGO, 1.1f));
        }
    }

    private static float dirToYaw(Vector dir) {
        return (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Strike in front and mirror it from your Exit portal.";
            case 2 -> ChatColor.GRAY + "The mirror applies Endings.";
            case 3 -> ChatColor.GRAY + "A third strike erupts from the Entry portal.";
            case 4 -> ChatColor.GRAY + "Higher damage.";
            case 5 -> ChatColor.GRAY + "An enemy hit by both is briefly stunned.";
            default -> ChatColor.GRAY + "Strike in front and from your portal.";
        };
    }

    @Override
    public String toString() {
        return "twoFacedStrike";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.GOLDEN_SWORD),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Two-Faced Strike]",
                ChatColor.GRAY + "Strike in front — and the same blow erupts from",
                ChatColor.GRAY + "your " + ChatColor.DARK_PURPLE + "Exit portal" + ChatColor.GRAY + ". An enemy caught in both",
                ChatColor.GRAY + "the strike and the mouth takes " + ChatColor.WHITE + "bonus damage" + ChatColor.GRAY + ".");
    }
}
