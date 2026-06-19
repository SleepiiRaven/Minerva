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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Nemesis RLL — the payoff. Unleash the Ledger as retribution: 6 + min(Ledger, cap) damage,
 * then empty the bank. The orbiting scale-sword raises overhead, then a single downward sword
 * of judgment falls with one impact burst. Higher ranks raise the cap, splash to neighbours,
 * keep a portion of the Ledger, and refund half the cooldown on a kill.
 */
public class CollectTheDebt extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "collectTheDebt")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 18);
        if (target == null) {
            player.sendActionBar(ChatColor.GOLD + "No debtor in sight!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "collectTheDebt", cooldown);
        cooldownAlarm(player, cooldown, "Collect the Debt");

        double cap = 25 + 5 * level;
        double ledger = LedgerOfWrongs.getLedger(player);
        final double dmg = 6 + Math.min(ledger, cap);
        final int fLevel = level;
        final long fCooldown = cooldown;

        // L4: keep 30% of the ledger instead of fully emptying it
        if (level >= 4) {
            LedgerOfWrongs.LEDGER.put(player.getUniqueId(), ledger * 0.3);
        } else {
            LedgerOfWrongs.spend(player);
        }

        // telegraph: gather the gold beam overhead
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 1f, 0.8f);
        final Location overhead = target.getLocation().clone().add(0, 3.5, 0);
        for (double y = 0; y <= 1.4; y += 0.2) {
            overhead.getWorld().spawnParticle(Particle.DUST, overhead.clone().add(0, y, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[3], 1.2f));
        }

        // after a short windup, the sword of judgment falls
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isDead()) return;
                Location impact = target.getLocation().clone().add(0, target.getHeight() / 2, 0);

                // downward sword-of-judgment column
                Location top = target.getLocation().clone().add(0, 4.0, 0);
                for (Vector v : ParticleUtils.getLinePoints(top.toVector(), impact.toVector(), 0.3)) {
                    impact.getWorld().spawnParticle(Particle.DUST, v.toLocation(impact.getWorld()), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[3], 1.3f));
                }
                // crossguard near the top of the blade
                Location guard = target.getLocation().clone().add(0, 2.2, 0);
                for (double s = -0.7; s <= 0.7; s += 0.2) {
                    impact.getWorld().spawnParticle(Particle.DUST, guard.clone().add(s, 0, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[2], 1.1f));
                    impact.getWorld().spawnParticle(Particle.DUST, guard.clone().add(0, 0, s), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[2], 1.1f));
                }

                double targetHpBefore = target.getHealth();
                damage(target, dmg, player, true, false);

                // L3: splash partial damage to enemies near the target
                if (fLevel >= 3) {
                    double splash = dmg * 0.4;
                    for (Entity e : target.getNearbyEntities(3.5, 3, 3.5)) {
                        if (!(e instanceof LivingEntity le) || e == player || e == target) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, le)) continue;
                        damage(le, splash, player, true, false);
                    }
                }

                // one-shot impact burst (no strobe)
                impact.getWorld().spawnParticle(Particle.DUST, impact, 30, 0.5, 0.4, 0.5, 0,
                        new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[3], 1.4f));
                impact.getWorld().spawnParticle(Particle.DUST, impact, 20, 0.6, 0.3, 0.6, 0,
                        new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[0], 1.2f));
                impact.getWorld().playSound(impact, Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.8f);
                impact.getWorld().playSound(impact, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 1.2f);

                // L5: a kill refunds half the cooldown
                if (fLevel >= 5 && (target.isDead() || target.getHealth() <= 0.1) && targetHpBefore > 0) {
                    cooldownManager.setCooldownFromNow(player.getUniqueId(), "collectTheDebt", fCooldown / 2);
                    cooldownAlarm(player, fCooldown / 2, "Collect the Debt");
                    if (player.isOnline()) {
                        player.sendActionBar(ChatColor.GOLD + "Debt collected — cooldown refunded!");
                    }
                }
            }
        }.runTaskLater(Minerva.getInstance(), 8L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Strike for 6 + your Ledger, then empty the bank.";
            case 2 -> ChatColor.GRAY + "Higher damage cap.";
            case 3 -> ChatColor.GRAY + "Splashes partial damage to nearby enemies.";
            case 4 -> ChatColor.GRAY + "Keep 30% of the Ledger after collecting.";
            case 5 -> ChatColor.GRAY + "A kill refunds half the cooldown.";
            default -> ChatColor.GRAY + "Unleash the Ledger as a single retribution strike.";
        };
    }

    @Override
    public String toString() {
        return "collectTheDebt";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.NETHERITE_SWORD),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Collect the Debt]",
                ChatColor.GRAY + "Call down a " + ChatColor.GOLD + "sword of judgment" + ChatColor.GRAY + ", dealing",
                ChatColor.GRAY + "" + ChatColor.GOLD + "6 + your Ledger" + ChatColor.GRAY + " damage and emptying the bank.",
                ChatColor.GRAY + "Is it fat enough — and are you alive to spend it?");
    }
}
