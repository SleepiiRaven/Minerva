package net.minervamc.minerva.skills.greek.hypnos;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
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
 * Hypnos RLL — the finisher. Strike the aimed target: if it is Asleep, the dream sours into a
 * Nightmare — heavy damage, a dark burst, the sleeper wakes, and nearby enemies are Feared.
 * Against a waking target it deals modest damage and a big chunk of Drowsiness instead.
 */
public class Nightmare extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 6500;
            case 4, 5 -> 6000;
            default -> 7000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "nightmare")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = frontTarget(player, 5);
        if (target == null) {
            player.sendActionBar(ChatColor.BLUE + "No target in range!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "nightmare", cooldown);
        cooldownAlarm(player, cooldown, "Nightmare");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f);

        if (isAsleep(target)) {
            double dmg = switch (level) {
                case 4 -> 15;
                case 5 -> 17;
                default -> 12;
            };
            damage(target, dmg, player);
            wake(target);

            // one-shot dark burst
            Location c = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
            c.getWorld().spawnParticle(Particle.SCULK_SOUL, c, 30, 0.4, 0.5, 0.4, 0.05);
            for (Vector v : ParticleUtils.getSpherePoints(1.0, 7)) {
                c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(Sandman.DREAM[3], 1.2f));
            }
            c.getWorld().playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
            c.getWorld().playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1f, 0.8f);

            long fearTicks = level >= 2 ? 40 : 30; // L2 longer Fear
            for (Entity e : target.getNearbyEntities(4, 3, 4)) {
                if (!(e instanceof LivingEntity le) || e == player || e == target) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                if (PlayerStats.isSummoned(player, le)) continue;
                fear(player, le, fearTicks);
                // L3: Fear spreads to enemies near the freshly-feared
                if (level >= 3) {
                    for (Entity e2 : le.getNearbyEntities(3, 3, 3)) {
                        if (!(e2 instanceof LivingEntity le2) || e2 == player || e2 == target || e2 == le) continue;
                        if (le2 instanceof Player p2 && Party.isPlayerInPlayerParty(player, p2)) continue;
                        if (PlayerStats.isSummoned(player, le2)) continue;
                        fear(player, le2, fearTicks);
                    }
                }
            }

            // L5: re-apply heavy Drowsiness to all nearby
            if (level >= 5) {
                for (Entity e : target.getNearbyEntities(5, 3, 5)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    Sandman.addDrowsiness(player, le, 50, level);
                }
            }
        } else {
            damage(target, 4, player);
            Sandman.addDrowsiness(player, target, 40, level);
            Location loc = target.getLocation().clone().add(0, 1, 0);
            target.getWorld().spawnParticle(Particle.NOTE, loc, 8, 0.3, 0.4, 0.3, 0.4);
            target.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.3, 0.4, 0.3, 0,
                    new Particle.DustOptions(Sandman.DREAM[(int) (Math.random() * Sandman.DREAM.length)], 1f));
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
        }
    }

    /** Locate the living entity the player is aiming at within range (friendly-fire safe). */
    private static LivingEntity frontTarget(Player p, double range) {
        Vector dir = p.getEyeLocation().getDirection();
        LivingEntity best = null;
        double bestDot = 0.55;
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == p) continue;
            if (le instanceof Player pl && Party.isPlayerInPlayerParty(p, pl)) continue;
            if (PlayerStats.isSummoned(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getEyeLocation().toVector());
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            double dot = to.normalize().dot(dir);
            if (dot > bestDot) {
                bestDot = dot;
                best = le;
            }
        }
        return best;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Strike a target. Sleepers take heavy damage and Fear nearby foes.";
            case 2 -> ChatColor.GRAY + "Longer Fear.";
            case 3 -> ChatColor.GRAY + "Fear spreads to enemies near the feared.";
            case 4 -> ChatColor.GRAY + "More damage to sleepers.";
            case 5 -> ChatColor.GRAY + "The nightmare floods all nearby with Drowsiness.";
            default -> ChatColor.GRAY + "Strike a target; sleepers take heavy damage.";
        };
    }

    @Override
    public String toString() {
        return "nightmare";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SCULK),
                ChatColor.BLUE + "" + ChatColor.BOLD + "[Nightmare]",
                ChatColor.GRAY + "Strike your target. If it is " + ChatColor.BLUE + "Asleep" + ChatColor.GRAY + ", the dream",
                ChatColor.GRAY + "sours — heavy damage and " + ChatColor.DARK_PURPLE + "Fear" + ChatColor.GRAY + " on nearby enemies.",
                ChatColor.GRAY + "Otherwise, modest damage and a flood of Drowsiness.");
    }
}
