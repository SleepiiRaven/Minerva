package net.minervamc.minerva.skills.greek.persephone;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Persephone RRR — movement. Costs 1 seed. Persephone sinks into the ground (briefly untargetable
 * via INVISIBILITY + a tag), then re-emerges at an aimed nearby ground location. DEATH-SAFE: only
 * solid ground with 2 air blocks above is chosen; fall distance is reset and SLOW_FALLING applied
 * on arrival; never teleports into the void or inside blocks. Ground cracks + petals sink with her.
 */
public class Descent extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 5500;
            case 4, 5 -> 5000;
            default -> 6000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "descent")) {
            onCooldown(player);
            return;
        }

        // require a seed, but don't spend it until a death-safe destination is confirmed
        if (SeedsOfTheUnderworld.getSeeds(player) < 1) {
            skillLocked(player, "you have no Pomegranate Seeds");
            return;
        }

        double range = switch (level) {
            case 2 -> 9.0;
            case 3 -> 10.0;
            case 4, 5 -> 11.0;
            default -> 8.0;
        };

        // find a death-safe destination along the aim line before committing
        Location dest = findSafeDestination(player, range);
        if (dest == null) {
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "No safe ground to emerge!");
            return;
        }

        // commit: spend the seed now (guard against a race where it vanished)
        if (!SeedsOfTheUnderworld.spendSeeds(player, 1)) {
            skillLocked(player, "you have no Pomegranate Seeds");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "descent", cooldown);
        cooldownAlarm(player, cooldown, "Descent");

        final int fLevel = level;
        final Location entry = player.getLocation().clone();

        // L3: leave a free Bloom patch at the entry point
        if (fLevel >= 3) {
            Bloom.registerPatch(entry, 2.4, 12000L);
            for (Vector v : ParticleUtils.getCirclePoints(2.4, 18)) {
                entry.getWorld().spawnParticle(Particle.DUST, entry.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[1], 1.0f));
            }
        }

        // sink particles + sound (single-shot)
        sinkBurst(entry);
        player.getWorld().playSound(entry, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.7f);
        player.getWorld().playSound(entry, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);

        // briefly untargetable: invisibility + tag
        int untargetTicks = fLevel >= 4 ? 16 : 12;
        player.addScoreboardTag("persephoneDescent");
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, untargetTicks + 4, 0));

        // L5: carry one nearby party ally
        final Player carried = (fLevel >= 5) ? nearestAlly(player, 5) : null;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    player.removeScoreboardTag("persephoneDescent");
                    return;
                }
                player.setFallDistance(0f);
                player.teleport(dest);
                player.setFallDistance(0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
                player.removeScoreboardTag("persephoneDescent");

                if (carried != null && carried.isOnline() && !carried.isDead()) {
                    Location allyDest = findSafeNear(dest, 2);
                    if (allyDest != null) {
                        carried.setFallDistance(0f);
                        carried.teleport(allyDest);
                        carried.setFallDistance(0f);
                        carried.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
                    }
                }

                // emerge burst: single petal ring
                emergeBurst(dest);
                dest.getWorld().playSound(dest, Sound.BLOCK_MOSS_PLACE, 1f, 1.1f);
                dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
            }
        }.runTaskLater(Minerva.getInstance(), untargetTicks);
    }

    /**
     * Pick a death-safe destination: walk outward from the aim direction, find the highest solid
     * block with 2 air blocks above near the aim point, within range. Never the void / inside blocks.
     */
    private static Location findSafeDestination(Player player, double range) {
        Vector dir = player.getEyeLocation().getDirection().clone();
        dir.setY(0);
        if (dir.lengthSquared() < 0.001) dir = new Vector(0, 0, 1);
        dir.normalize();

        Location origin = player.getLocation();
        // try the furthest reachable point first, stepping back toward the player
        for (double d = range; d >= 1.0; d -= 1.0) {
            Location aim = origin.clone().add(dir.clone().multiply(d));
            Location safe = findSafeNear(aim, 1);
            if (safe != null) return safe;
        }
        return null;
    }

    /** Search a small column/area around a target XZ for a standable spot (2 air over solid). */
    private static Location findSafeNear(Location aim, int horizSearch) {
        for (int dx = 0; dx <= horizSearch; dx++) {
            for (int dz = 0; dz <= horizSearch; dz++) {
                for (int sx : (dx == 0 ? new int[]{0} : new int[]{dx, -dx})) {
                    for (int sz : (dz == 0 ? new int[]{0} : new int[]{dz, -dz})) {
                        Location candidate = standableInColumn(aim.clone().add(sx, 0, sz));
                        if (candidate != null) return candidate;
                    }
                }
            }
        }
        return null;
    }

    /** Scan a vertical column near aim.y for ground = solid block with 2 air above. */
    private static Location standableInColumn(Location aim) {
        int baseY = aim.getBlockY();
        int minY = aim.getWorld().getMinHeight();
        int maxY = aim.getWorld().getMaxHeight();
        // search downward then a little upward around the aim height
        for (int y = Math.min(baseY + 2, maxY - 1); y >= Math.max(baseY - 6, minY + 1); y--) {
            Block ground = aim.getWorld().getBlockAt(aim.getBlockX(), y - 1, aim.getBlockZ());
            Block feet = aim.getWorld().getBlockAt(aim.getBlockX(), y, aim.getBlockZ());
            Block head = aim.getWorld().getBlockAt(aim.getBlockX(), y + 1, aim.getBlockZ());
            if (isSolidGround(ground) && isClear(feet) && isClear(head)) {
                return new Location(aim.getWorld(), aim.getBlockX() + 0.5, y, aim.getBlockZ() + 0.5,
                        aim.getYaw(), aim.getPitch());
            }
        }
        return null;
    }

    private static boolean isSolidGround(Block b) {
        Material m = b.getType();
        if (m == Material.LAVA || m == Material.MAGMA_BLOCK || m == Material.CACTUS || m == Material.CAMPFIRE) return false;
        return m.isSolid() && b.isCollidable();
    }

    private static boolean isClear(Block b) {
        Material m = b.getType();
        if (m == Material.LAVA) return false;
        return b.isPassable() && !b.isLiquid();
    }

    private static Player nearestAlly(Player player, double range) {
        List<Player> party = Party.partyList(player);
        if (party == null) return null;
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : party) {
            if (p == null || p == player || !p.isOnline()) continue;
            if (!p.getWorld().equals(player.getWorld())) continue;
            double d = p.getLocation().distanceSquared(player.getLocation());
            if (d <= range * range && d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private static final Color[] SINK = {
        Color.fromRGB(120, 70, 130), Color.fromRGB(90, 40, 80), Color.fromRGB(220, 90, 150),
    };

    private static void sinkBurst(Location loc) {
        for (Vector v : ParticleUtils.getCirclePoints(1.0, 16)) {
            Location l = loc.clone().add(v).add(0, 0.1, 0);
            loc.getWorld().spawnParticle(Particle.DUST, l, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(SINK[(int) (Math.abs(v.getX()) * 2) % SINK.length], 1.1f));
            loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, l.clone().add(0, 0.2, 0), 0, 0, 0, 0, 0);
        }
        loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc.clone().add(0, 0.3, 0), 18, 0.5, 0.2, 0.5, 0,
                Material.ROOTED_DIRT.createBlockData());
    }

    private static void emergeBurst(Location loc) {
        for (Vector v : ParticleUtils.getCirclePoints(1.2, 18)) {
            Location l = loc.clone().add(v).add(0, 0.1, 0);
            loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, l.clone().add(0, 0.3, 0), 0, 0, 0, 0, 0);
            loc.getWorld().spawnParticle(Particle.DUST, l, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[0], 1.0f));
        }
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc.clone().add(0, 0.6, 0), 8, 0.6, 0.3, 0.6, 0);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Sink and re-emerge at safe ground you aim at.";
            case 2 -> ChatColor.GRAY + "Longer range.";
            case 3 -> ChatColor.GRAY + "Leaves a free Bloom patch where you sank.";
            case 4 -> ChatColor.GRAY + "Longer untargetable window; further range.";
            case 5 -> ChatColor.GRAY + "May carry one nearby party ally with you.";
            default -> ChatColor.GRAY + "Sink and re-emerge at safe ground.";
        };
    }

    @Override
    public String toString() {
        return "descent";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ROOTED_DIRT),
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[Descent]",
                ChatColor.GRAY + "Spend " + ChatColor.LIGHT_PURPLE + "1 seed" + ChatColor.GRAY + " to sink into the earth,",
                ChatColor.GRAY + "briefly untargetable, then re-emerge at safe ground",
                ChatColor.GRAY + "you aim at — never into a fall or a wall.");
    }
}
