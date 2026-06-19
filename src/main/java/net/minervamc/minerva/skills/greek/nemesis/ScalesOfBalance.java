package net.minervamc.minerva.skills.greek.nemesis;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
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
 * Nemesis RLR — the scales tip. Aim an enemy; both move toward the average of your HP%: she
 * heals if below it, the enemy drops if above. A hard floor (never below 20% of the enemy's
 * max health, never lethal) means it's brutal vs. a full-HP bruiser and weak vs. a low one —
 * built-in counterplay and a real target-choice call. Two DUST tether beams from a floating
 * scale icon. Higher ranks close more of the gap, bank the difference, and hit two targets.
 */
public class ScalesOfBalance extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 3 -> 12500;
            case 4, 5 -> 12000;
            default -> 14000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "scalesOfBalance")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 20);
        if (target == null) {
            player.sendActionBar(ChatColor.GOLD + "No target to weigh!");
            return;
        }
        LivingEntity second = (level >= 5) ? nearestOther(player, target, 8) : null;

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "scalesOfBalance", cooldown);
        cooldownAlarm(player, cooldown, "Scales of Balance");

        // how much of the gap to close
        double closeFrac = switch (level) {
            case 2 -> 0.65;
            case 3 -> 0.7;
            case 4 -> 0.8;
            case 5 -> 0.85;
            default -> 0.5;
        };

        double bankedDiff = equalize(player, target, closeFrac, level);
        if (second != null) {
            bankedDiff += equalize(player, second, closeFrac, level);
        }

        if (level >= 4 && bankedDiff > 0) {
            LedgerOfWrongs.addToLedger(player, bankedDiff * 0.5);
        }

        // floating scale icon above the caster + tether beams
        Location scale = player.getLocation().clone().add(0, 2.4, 0);
        scale.getWorld().spawnParticle(Particle.DUST, scale, 0, 0, 0, 0, 0,
                new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[3], 1.6f));
        for (Vector v : ParticleUtils.getCirclePoints(0.5, 10)) {
            scale.getWorld().spawnParticle(Particle.DUST, scale.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[2], 1f));
        }
        tether(scale, player.getLocation().clone().add(0, 1, 0));
        tether(scale, target.getLocation().clone().add(0, target.getHeight() / 2, 0));
        if (second != null) tether(scale, second.getLocation().clone().add(0, second.getHeight() / 2, 0));

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1.1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.7f);
    }

    /** Drag self and target toward the average HP%; returns the magnitude of HP shifted on the enemy. */
    private static double equalize(Player player, LivingEntity target, double closeFrac, int level) {
        double selfMax = player.getMaxHealth();
        double tgtMax = target.getMaxHealth();
        if (selfMax <= 0 || tgtMax <= 0) return 0;

        double selfPct = player.getHealth() / selfMax;
        double tgtPct = target.getHealth() / tgtMax;
        double avg = (selfPct + tgtPct) / 2;

        // self moves toward avg
        double selfTargetPct = selfPct + (avg - selfPct) * closeFrac;
        double newSelf = Math.min(selfMax, Math.max(player.getHealth(), selfTargetPct * selfMax));
        if (newSelf > player.getHealth()) {
            player.setHealth(Math.min(selfMax, newSelf));
        }

        // enemy moves toward avg, but never below 20% of its max and never lethal
        double tgtTargetPct = tgtPct + (avg - tgtPct) * closeFrac;
        double floorHp = tgtMax * 0.2;
        double desiredHp = tgtTargetPct * tgtMax;
        double shifted = 0;
        if (desiredHp < target.getHealth()) {
            double newTgt = Math.max(floorHp, desiredHp);
            double dmg = target.getHealth() - newTgt;
            if (dmg > 0.1 && newTgt < target.getHealth()) {
                shifted = dmg;
                // route through Skill.damage so resist/indicators apply, but cap so it never goes lethal
                double safeDmg = Math.min(dmg, Math.max(0, target.getHealth() - floorHp));
                if (safeDmg > 0) damage(target, safeDmg, player, true, false);
            }
        }
        return shifted;
    }

    private static void tether(Location from, Location to) {
        for (Vector v : ParticleUtils.getLinePoints(from.toVector(), to.toVector(), 0.4)) {
            from.getWorld().spawnParticle(Particle.DUST, v.toLocation(from.getWorld()), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[2], 0.9f));
        }
    }

    private static LivingEntity nearestOther(Player player, LivingEntity exclude, double range) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : exclude.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player || e == exclude) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
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
            case 1 -> ChatColor.GRAY + "Drag a target toward your shared average HP%.";
            case 2 -> ChatColor.GRAY + "Closes more of the HP gap.";
            case 3 -> ChatColor.GRAY + "Lower cooldown.";
            case 4 -> ChatColor.GRAY + "Banks the HP difference into the Ledger.";
            case 5 -> ChatColor.GRAY + "Weighs two targets toward the shared average.";
            default -> ChatColor.GRAY + "Equalize HP% with a target. Never an execute.";
        };
    }

    @Override
    public String toString() {
        return "scalesOfBalance";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.GOLD_INGOT),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Scales of Balance]",
                ChatColor.GRAY + "Tip the scales: you and your target both move toward",
                ChatColor.GRAY + "your " + ChatColor.GOLD + "average HP%" + ChatColor.GRAY + ". Brutal on the healthy, weak",
                ChatColor.GRAY + "on the wounded — and never an execute.");
    }
}
