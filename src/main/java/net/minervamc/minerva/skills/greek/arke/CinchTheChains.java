package net.minervamc.minerva.skills.greek.arke;

import java.util.List;
import net.minervamc.minerva.Minerva;
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
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Arke RRL (utility) — the setup payoff. Cinch every enemy chained to you together at their average
 * point and root them, retracting the chains to a central knot — a clump for your team's AoE.
 * Levels lengthen the root, add collision damage, widen the pull range, and fear on release.
 */
public class CinchTheChains extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 15000;
            case 4, 5 -> 14000;
            default -> 16000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "cinchTheChains")) {
            onCooldown(player);
            return;
        }

        List<LivingEntity> chained = SharedFate.getChainOf(player);
        // friendly-safe pruning
        chained.removeIf(le -> le == player
                || (le instanceof Player p && Party.isPlayerInPlayerParty(player, p))
                || PlayerStats.isSummoned(player, le)
                || le.getWorld() != player.getWorld());

        if (chained.size() < 2) {
            player.sendActionBar(ChatColor.GRAY + "No chains to cinch!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "cinchTheChains", cooldown);
        cooldownAlarm(player, cooldown, "Cinch the Chains");

        long rootTicks = switch (level) {
            case 2, 3 -> 30L;
            case 4, 5 -> 35L;
            default -> 20L;
        };
        final int fLevel = level;

        // average point of all chained enemies = the knot.
        Vector sum = new Vector(0, 0, 0);
        for (LivingEntity le : chained) sum.add(le.getLocation().toVector());
        final Location knot = sum.multiply(1.0 / chained.size()).toLocation(player.getWorld());

        // yank everyone to the knot (strong velocity), then root.
        for (LivingEntity le : chained) {
            Vector toKnot = knot.toVector().subtract(le.getLocation().toVector());
            double dist = toKnot.length();
            if (dist > 0.01) {
                Vector pull = toKnot.normalize().multiply(Math.min(2.2, 0.5 + dist * 0.35));
                pull.setY(Math.max(0.25, pull.getY()));
                le.setVelocity(pull);
            }
            // retracting chain line to the knot (single-shot, spatial gradient)
            World w = le.getWorld();
            Vector a = le.getLocation().clone().add(0, le.getHeight() / 2, 0).toVector();
            List<Vector> pts = ParticleUtils.getLinePoints(a, knot.clone().add(0, 1, 0).toVector(), 0.4);
            int n = pts.size();
            for (int k = 0; k < n; k++) {
                Color c = SharedFate.PALETTE[(k * SharedFate.PALETTE.length) / Math.max(1, n)];
                w.spawnParticle(Particle.DUST, pts.get(k).toLocation(w), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(c, 1f));
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.9f, 0.8f);
        knot.getWorld().playSound(knot, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);

        // root after the yank lands (so they don't slide out of it), then handle release effects.
        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity le : chained) {
                    if (le.isDead()) continue;
                    stun(player, le, rootTicks);
                    // L3: collision damage on the clump.
                    if (fLevel >= 3) {
                        damage(le, fLevel >= 5 ? 7 : 6, player);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().clone().add(0, le.getHeight() / 2, 0),
                                10, 0.2, 0.3, 0.2, 0.05);
                    }
                }
                // central knot burst (single-shot, steady)
                knot.getWorld().spawnParticle(Particle.CRIT, knot.clone().add(0, 1, 0), 24, 0.6, 0.6, 0.6, 0.1);
                for (Vector v : ParticleUtils.getSpherePoints(1.0, 7)) {
                    Color c = SharedFate.PALETTE[(int) (Math.abs(v.getX() + v.getZ()) * 3) % SharedFate.PALETTE.length];
                    knot.getWorld().spawnParticle(Particle.DUST, knot.clone().add(0, 1, 0).add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(c, 1.1f));
                }
                knot.getWorld().playSound(knot, Sound.BLOCK_CONDUIT_ACTIVATE, 0.8f, 1.1f);

                // L5: when the root ends, the clustered enemies are feared.
                if (fLevel >= 5) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (LivingEntity le : chained) {
                                if (!le.isDead()) fear(player, le, 40L);
                            }
                        }
                    }.runTaskLater(Minerva.getInstance(), rootTicks);
                }
            }
        }.runTaskLater(Minerva.getInstance(), 4L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Yank chained enemies together and root them briefly.";
            case 2 -> ChatColor.GRAY + "Longer root.";
            case 3 -> ChatColor.GRAY + "The clump takes collision damage.";
            case 4 -> ChatColor.GRAY + "Wider pull and longer root.";
            case 5 -> ChatColor.GRAY + "When the root ends, the clustered enemies flee in fear.";
            default -> ChatColor.GRAY + "Cinch your chains into one rooted clump.";
        };
    }

    @Override
    public String toString() {
        return "cinchTheChains";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_BARS),
                ChatColor.GRAY + "" + ChatColor.BOLD + "[Cinch the Chains]",
                ChatColor.GRAY + "Retract every chain to a single " + ChatColor.WHITE + "knot" + ChatColor.GRAY + ",",
                ChatColor.GRAY + "yanking your bound foes together and " + ChatColor.WHITE + "rooting" + ChatColor.GRAY + " them —",
                ChatColor.GRAY + "a clump primed for your team's AoE.");
    }
}
