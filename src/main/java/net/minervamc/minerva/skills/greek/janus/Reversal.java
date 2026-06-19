package net.minervamc.minerva.skills.greek.janus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Janus RLR — undo an enemy's escape. On cast, snapshot ONE aimed enemy's location (a single
 * stored position per owner — no per-enemy history). After 3s (or a re-press) the target is
 * yanked back to where it stood. At L5 the snapshot holds longer and can pin two enemies.
 * gold #FFCE5A breadcrumb at the marked spot; indigo #3B2E8C wisp on the yank.
 */
public class Reversal extends Skill {
    public static final Color GOLD = Color.fromRGB(255, 206, 90);
    public static final Color INDIGO = Color.fromRGB(59, 46, 140);

    /** A single stored snapshot: a target and the location to drag it back to. */
    public static class Snap {
        public LivingEntity target;
        public LivingEntity target2; // optional second pin (L5)
        public Location loc;
        public Location loc2;

        public Snap(LivingEntity target, Location loc) {
            this.target = target;
            this.loc = loc;
        }
    }

    private static final Map<UUID, Object> SNAPS = new HashMap<>();

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 12500;
            case 4, 5 -> 12000;
            default -> 13000;
        };

        // RE-PRESS: a snapshot is already armed -> yank immediately.
        Object held = SNAPS.get(player.getUniqueId());
        if (held instanceof Snap snap) {
            SNAPS.remove(player.getUniqueId());
            yank(player, snap, level);
            return;
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "reversal")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 18);
        if (target == null) {
            player.sendActionBar(ChatColor.GOLD + "No target to mark!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "reversal", cooldown);
        cooldownAlarm(player, cooldown, "Reversal");

        Snap snap = new Snap(target, target.getLocation().clone());
        if (level >= 5) {
            LivingEntity second = nearestOther(player, target, 5);
            if (second != null) {
                snap.target2 = second;
                snap.loc2 = second.getLocation().clone();
            }
        }
        SNAPS.put(player.getUniqueId(), snap);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);
        player.sendActionBar(ChatColor.GOLD + "Snapshot armed — re-press to reverse.");

        long holdTicks = level >= 5 ? 80L : 60L;

        // breadcrumb at the marked spot until the auto-yank fires
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                Object cur = SNAPS.get(player.getUniqueId());
                if (cur != snap) { // re-pressed already
                    cancel();
                    return;
                }
                if (t >= holdTicks || snap.target == null || snap.target.isDead()) {
                    SNAPS.remove(player.getUniqueId());
                    yank(player, snap, level);
                    cancel();
                    return;
                }
                breadcrumb(snap.loc);
                if (snap.loc2 != null) breadcrumb(snap.loc2);
                t += 4;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 4L);
    }

    private static void yank(Player player, Snap snap, int level) {
        pull(player, snap.target, snap.loc, level);
        if (snap.target2 != null && snap.loc2 != null) {
            pull(player, snap.target2, snap.loc2, level);
        }
    }

    private static void pull(Player player, LivingEntity target, Location loc, int level) {
        if (target == null || target.isDead() || loc == null) return;

        // indigo wisp streaking the path back to the snapshot
        for (Vector v : ParticleUtils.getLinePoints(target.getLocation().clone().add(0, target.getHeight() / 2, 0).toVector(),
                loc.clone().add(0, target.getHeight() / 2, 0).toVector(), 0.5)) {
            loc.getWorld().spawnParticle(Particle.DUST, v.toLocation(loc.getWorld()), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(INDIGO, 1.0f));
        }
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation(), 16, 0.3, 0.5, 0.3, 0.05);

        Location dest = loc.clone();
        dest.setDirection(target.getLocation().getDirection());
        target.teleport(dest);

        target.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        target.getWorld().playSound(dest, Sound.BLOCK_BELL_RESONATE, 0.8f, 0.7f);

        if (level >= 2) {
            stun(player, target, level >= 4 ? 25L : 18L);
        }
        if (level >= 3) {
            GodOfTransitions.applyEndings(player, target, level);
        }
    }

    private static void breadcrumb(Location loc) {
        if (loc == null) return;
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.2, 0), 0, 0, 0, 0, 0,
                new Particle.DustOptions(GOLD, 1.0f));
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 0, 0, 0, 0, 0,
                new Particle.DustOptions(GOLD, 0.8f));
    }

    private static LivingEntity nearestOther(Player player, LivingEntity exclude, double range) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : exclude.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player || e == exclude) continue;
            if (le instanceof Player p && net.minervamc.minerva.party.Party.isPlayerInPlayerParty(player, p)) continue;
            if (net.minervamc.minerva.PlayerStats.isSummoned(player, le)) continue;
            if (le.hasMetadata("NPC")) continue;
            double d = le.getLocation().distanceSquared(exclude.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Mark a target; after 3s (or re-press) yank it back.";
            case 2 -> ChatColor.GRAY + "The yank briefly stuns.";
            case 3 -> ChatColor.GRAY + "The yank also applies Endings.";
            case 4 -> ChatColor.GRAY + "Longer stun and lower cooldown.";
            case 5 -> ChatColor.GRAY + "Hold the mark longer; pins up to two enemies.";
            default -> ChatColor.GRAY + "Mark a target, then yank it back.";
        };
    }

    @Override
    public String toString() {
        return "reversal";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.CLOCK),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Reversal]",
                ChatColor.GRAY + "Snapshot a foe's position. Moments later they are",
                ChatColor.GRAY + "dragged " + ChatColor.DARK_PURPLE + "back" + ChatColor.GRAY + " to where they stood —",
                ChatColor.GRAY + "undo an escape or haul a diver into your team.");
    }
}
