package net.minervamc.minerva.skills.greek.thanatos;

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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Thanatos RRR — NON-movement utility. Cloak in a death shroud for ~4s: gain RESISTANCE (feels
 * like ~35% damage reduction) and HEAL whenever a nearby marked enemy's Death Timer ticks — you
 * feed on impending death. This is the survivability that lets a mobility-less god outlast the wait
 * for the sand to run out. A faint thread links you to each marked enemy in range.
 */
public class ShroudOfLetus extends Skill {
    private static final double HEAL_RANGE = 8.0;

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 11500;
            case 4, 5 -> 11000;
            default -> 12000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "shroudOfLetus")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "shroudOfLetus", cooldown);
        cooldownAlarm(player, cooldown, "Shroud of Letus");

        int durTicks = 80;
        int drAmp = switch (level) {
            case 2, 3 -> 1;       // RESISTANCE II — stronger DR
            case 4, 5 -> 2;       // RESISTANCE III
            default -> 0;         // RESISTANCE I (~35% feel)
        };
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durTicks, drAmp));

        // L4: the shroud also wraps a single ally standing beside you.
        if (level >= 4) {
            LivingEntity ally = nearestAlly(player, 3.5);
            if (ally instanceof Player ap) {
                ap.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durTicks, Math.max(0, drAmp - 1)));
                ap.getWorld().spawnParticle(Particle.SOUL, ap.getLocation().clone().add(0, 1, 0), 6, 0.3, 0.6, 0.3, 0.01);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.7f);

        // mark the shroud active (and its level) so timer ticks know to feed the owner.
        player.addScoreboardTag("thanatosShroud");
        player.getScoreboardTags().removeIf(tag -> tag.startsWith("thanatosShroudL"));
        player.addScoreboardTag("thanatosShroudL" + level);

        final int fLevel = level;
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || player.isDead() || !player.isOnline()) {
                    player.removeScoreboardTag("thanatosShroud");
                    player.getScoreboardTags().removeIf(tag -> tag.startsWith("thanatosShroudL"));
                    if (fLevel >= 3) {
                        // L3 — flicker out of reach for half a second.
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.1f);
                    }
                    cancel();
                    return;
                }

                // steady wrapping shroud — a slow vertical ring of soul + dark dust (no strobe).
                Location c = player.getLocation().clone().add(0, 1.0, 0);
                double phase = (t % 40) / 40.0 * Math.PI * 2;
                for (Vector v : ParticleUtils.getVerticalCirclePoints(0.7, (float) Math.toDegrees(phase), player.getLocation().getYaw(), 6)) {
                    player.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TheInevitable.BONE[3], 1.0f));
                }
                if (t % 10 == 0) {
                    player.getWorld().spawnParticle(Particle.SOUL, c, 3, 0.4, 0.5, 0.4, 0.01);
                    // faint threads to each marked enemy in range
                    for (LivingEntity le : TheInevitable.markedNear(player, HEAL_RANGE)) {
                        Vector to = le.getLocation().clone().add(0, le.getHeight() / 2, 0).toVector();
                        for (Vector p : ParticleUtils.getLinePoints(c.toVector(), to, 0.6)) {
                            player.getWorld().spawnParticle(Particle.DUST, p.toLocation(player.getWorld()), 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(TheInevitable.BONE[2], 0.7f));
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /**
     * Called by TheInevitable every time a marked enemy's timer ticks. If the shroud is active and
     * that enemy is near the owner, heal a sliver of HP. L5 scales the trickle with how many marked
     * enemies are clustered nearby (feast on the dying). Throttled to ~every 10 ticks per enemy.
     */
    public static void onTimerTick(Player owner, LivingEntity marked) {
        if (owner == null || !owner.isOnline() || owner.isDead()) return;
        if (!owner.getScoreboardTags().contains("thanatosShroud")) return;
        if (marked == null || marked.isDead()) return;
        if (!marked.getWorld().equals(owner.getWorld())) return;
        if (marked.getLocation().distance(owner.getLocation()) > HEAL_RANGE) return;

        // throttle: only feed roughly twice a second off any one timer
        if ((marked.getTicksLived() % 10) != 0) return;

        int level = shroudLevel(owner);
        double heal;
        if (level >= 5) {
            int n = TheInevitable.markedNear(owner, HEAL_RANGE).size();
            heal = 0.5 + 0.25 * Math.max(0, n - 1); // scales with the crowd of dying
        } else {
            heal = 1.0;
        }
        double newHp = Math.min(owner.getMaxHealth(), owner.getHealth() + heal);
        owner.setHealth(newHp);
        owner.getWorld().spawnParticle(Particle.SOUL, owner.getLocation().clone().add(0, 1, 0), 1, 0.2, 0.3, 0.2, 0.005);
    }

    // Track whether the shroud is active and at what level via scoreboard tags set in cast().
    private static int shroudLevel(Player owner) {
        for (int l = 5; l >= 1; l--) {
            if (owner.getScoreboardTags().contains("thanatosShroudL" + l)) return l;
        }
        return 1;
    }

    private static LivingEntity nearestAlly(Player owner, double range) {
        List<Player> party = Party.partyList(owner);
        if (party == null) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : party) {
            if (p == owner || p.isDead() || !p.isOnline()) continue;
            if (!p.getWorld().equals(owner.getWorld())) continue;
            double d = p.getLocation().distanceSquared(owner.getLocation());
            if (d <= range * range && d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Cloak ~4s: take less damage and heal off nearby ticking timers.";
            case 2 -> ChatColor.GRAY + "Stronger damage reduction.";
            case 3 -> ChatColor.GRAY + "The shroud ends with a brief flicker of intangibility.";
            case 4 -> ChatColor.GRAY + "The damage reduction also shields a party ally beside you.";
            case 5 -> ChatColor.GRAY + "Healing scales with the number of marked enemies near you.";
            default -> ChatColor.GRAY + "Cloak in a death shroud, feeding off the dying.";
        };
    }

    @Override
    public String toString() {
        return "shroudOfLetus";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.WITHER_SKELETON_SKULL),
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Shroud of Letus]",
                ChatColor.GRAY + "Wrap yourself in a death-shroud: take reduced damage",
                ChatColor.GRAY + "and " + ChatColor.DARK_GREEN + "heal" + ChatColor.GRAY + " every time a nearby marked enemy's",
                ChatColor.GRAY + "Death Timer ticks. You outlast the inevitable.");
    }
}
