package net.minervamc.minerva.skills.greek.janus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Janus RRR — the engine of the kit. First press plants an Entry gate at your feet; the second
 * press plants an Exit gate where you look. While both stand, any entity that touches one is
 * teleported to the other (a per-entity 0.5s cooldown stops ping-pong), and every crossing runs
 * the passive (GodOfTransitions.applyTransition). At L5 a third linked node forms a triangle.
 * Entry = gold #FFCE5A, Exit/Link = indigo #3B2E8C. Particles only — no real blocks placed.
 */
public class Doorway extends Skill {
    public static final Color GOLD = Color.fromRGB(255, 206, 90);
    public static final Color INDIGO = Color.fromRGB(59, 46, 140);

    /** A live portal pair (or trio) owned by one player. */
    public static class Portal {
        public Location entry;
        public Location exit;
        public Location link; // optional third node (L5)
        public long expire;

        public Portal(Location entry, long expire) {
            this.entry = entry;
            this.expire = expire;
        }
    }

    private static final Map<UUID, Portal> PORTALS = new HashMap<>();

    /** Public accessor for other Janus skills. May be null. */
    public static Portal portalOf(Player player) {
        return PORTALS.get(player.getUniqueId());
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        Portal existing = PORTALS.get(player.getUniqueId());

        // SECOND PRESS: an Entry exists with no Exit yet -> set the Exit and begin the cycle.
        if (existing != null && existing.entry != null && existing.exit == null) {
            int range = level >= 2 ? 26 : 20;
            Location exit = raycast(player, range);
            existing.exit = exit;

            // L5: a third linked node spawns offset from the exit (triangle network).
            if (level >= 5) {
                existing.link = raycastFrom(exit.clone().add(0, 1.2, 0), player.getLocation().getDirection(), 4);
            }

            player.getWorld().playSound(exit, Sound.BLOCK_PORTAL_TRIGGER, 1.2f, 1.2f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.2f, 0.8f);
            runPortals(player, existing, level);
            return;
        }

        // FIRST PRESS (or re-press after expiry): place a fresh Entry, subject to cooldown.
        long cooldown = switch (level) {
            case 2, 3 -> 9500;
            case 4, 5 -> 9000;
            default -> 10000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "doorway")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "doorway", cooldown);
        cooldownAlarm(player, cooldown, "Doorway");

        long durTicks = switch (level) {
            case 3 -> 220L;
            case 4, 5 -> 220L;
            default -> 160L;
        };
        Location entry = player.getLocation().clone();
        Portal portal = new Portal(entry, System.currentTimeMillis() + durTicks * 50L);
        PORTALS.put(player.getUniqueId(), portal);

        player.getWorld().playSound(entry, Sound.BLOCK_PORTAL_TRIGGER, 1.2f, 1.5f);
        player.getWorld().playSound(entry, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);
        player.sendActionBar(ChatColor.GOLD + "Entry placed — press again to set the Exit.");

        // draw the lone Entry gate until the Exit is placed or it expires
        new BukkitRunnable() {
            @Override
            public void run() {
                Portal cur = PORTALS.get(player.getUniqueId());
                if (cur != portal || cur.exit != null || System.currentTimeMillis() > cur.expire) {
                    cancel();
                    if (cur == portal && cur.exit == null && System.currentTimeMillis() > cur.expire) {
                        PORTALS.remove(player.getUniqueId());
                    }
                    return;
                }
                drawGate(portal.entry, GOLD);
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 2L);
    }

    /** The live teleport+draw cycle once both gates exist. */
    private static void runPortals(Player player, Portal portal, int level) {
        final long durTicks = switch (level) {
            case 3, 4, 5 -> 220L;
            default -> 160L;
        };
        portal.expire = System.currentTimeMillis() + durTicks * 50L;
        final Map<UUID, Long> passCd = new HashMap<>();
        final int fLevel = level;

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                Portal cur = PORTALS.get(player.getUniqueId());
                if (cur != portal || System.currentTimeMillis() > portal.expire || !player.isOnline()) {
                    if (cur == portal) PORTALS.remove(player.getUniqueId());
                    if (portal.entry != null) closeBurst(portal.entry, GOLD);
                    if (portal.exit != null) closeBurst(portal.exit, INDIGO);
                    cancel();
                    return;
                }

                drawGate(portal.entry, GOLD);
                drawGate(portal.exit, INDIGO);
                if (portal.link != null) drawGate(portal.link, INDIGO);

                checkPassage(player, portal.entry, portal.exit, passCd, fLevel);
                checkPassage(player, portal.exit, portal.entry, passCd, fLevel);
                if (portal.link != null) {
                    checkPassage(player, portal.link, portal.entry, passCd, fLevel);
                    checkPassage(player, portal.entry, portal.link, passCd, fLevel);
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** Teleport any entity standing in the 'from' mouth across to 'to' (with per-entity cd). */
    private static void checkPassage(Player owner, Location from, Location to, Map<UUID, Long> passCd, int level) {
        if (from == null || to == null) return;
        long now = System.currentTimeMillis();
        for (Entity e : from.getWorld().getNearbyEntities(from, 1.2, 1.6, 1.2)) {
            if (e.hasMetadata("NPC")) continue;
            Long until = passCd.get(e.getUniqueId());
            if (until != null && now < until) continue;
            passCd.put(e.getUniqueId(), now + 500L);

            Location dest = to.clone();
            dest.setDirection(e.getLocation().getDirection());
            from.getWorld().playSound(from, Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.3f);
            e.teleport(dest);
            to.getWorld().playSound(to, Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.1f);
            GodOfTransitions.applyTransition(owner, e, level);
        }
    }

    /** Vertical rune-gate ring (steady spatial gradient — no strobe). */
    private static void drawGate(Location center, Color color) {
        if (center == null) return;
        World w = center.getWorld();
        Location base = center.clone().add(0, 1.0, 0);
        float yaw = center.getYaw();
        for (Vector v : ParticleUtils.getVerticalCirclePoints(1.0, 0f, yaw + 90f, 18)) {
            w.spawnParticle(Particle.DUST, base.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 1.0f));
        }
    }

    private static void closeBurst(Location center, Color color) {
        if (center == null) return;
        World w = center.getWorld();
        Location base = center.clone().add(0, 1.0, 0);
        for (Vector v : ParticleUtils.getSpherePoints(0.8, 6)) {
            w.spawnParticle(Particle.DUST, base.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 1.1f));
        }
        w.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.2f);
    }

    /** Raycast from the eyes up to 'range' blocks, stopping just before the first solid block. */
    private static Location raycast(Player player, int range) {
        return raycastFrom(player.getEyeLocation(), player.getEyeLocation().getDirection(), range);
    }

    private static Location raycastFrom(Location start, Vector dir, int range) {
        Vector step = dir.clone().normalize().multiply(0.5);
        Location loc = start.clone();
        Location last = start.clone();
        for (int i = 0; i < range * 2; i++) {
            Location next = loc.clone().add(step);
            Block block = next.getBlock();
            if (block.getType().isSolid()) {
                break;
            }
            last = next;
            loc = next;
        }
        // settle to the ground beneath the stopping point
        Location grounded = last.clone();
        for (int i = 0; i < 4; i++) {
            Block below = grounded.clone().subtract(0, 1, 0).getBlock();
            if (below.getType().isSolid()) break;
            grounded.subtract(0, 1, 0);
        }
        return grounded;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Place a paired gate; entities crossing teleport.";
            case 2 -> ChatColor.GRAY + "Longer placement range (26).";
            case 3 -> ChatColor.GRAY + "Doors last longer.";
            case 4 -> ChatColor.GRAY + "Allies through the door gain a longer Beginnings.";
            case 5 -> ChatColor.GRAY + "A third linked node forms a portal triangle.";
            default -> ChatColor.GRAY + "Place a paired gate; crossings teleport.";
        };
    }

    @Override
    public String toString() {
        return "doorway";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_DOOR),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Doorway]",
                ChatColor.GRAY + "Plant an " + ChatColor.GOLD + "Entry" + ChatColor.GRAY + " at your feet, then an",
                ChatColor.GRAY + "" + ChatColor.DARK_PURPLE + "Exit" + ChatColor.GRAY + " where you look. Anything crossing one",
                ChatColor.GRAY + "is whisked to the other — your doors are shared.");
    }
}
