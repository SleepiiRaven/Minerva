package net.minervamc.minerva.skills.greek.hypnos;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hypnos RRR — a gentle drift: slow-falling forward float for ~2s on a soft-blue mist trail.
 * Enemies passed or directly below gain Drowsiness. Higher ranks float allies and trail an aura.
 */
public class Dreamdrift extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 7500;
            case 4, 5 -> 7000;
            default -> 8000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "dreamdrift")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "dreamdrift", cooldown);
        cooldownAlarm(player, cooldown, "Dreamdrift");

        int driftTicks = level >= 2 ? 50 : 40;
        int slowFallTicks = level >= 2 ? 80 : 60;
        final int fLevel = level;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, slowFallTicks, 0));
        // party allies near the caster also slow-fall (L4+)
        if (level >= 4 && Party.partyList(player) != null) {
            for (Player ally : Party.partyList(player)) {
                if (ally == null || !ally.isOnline()) continue;
                if (ally.getLocation().distanceSquared(player.getLocation()) <= 36) {
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, slowFallTicks, 0));
                }
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, 0.9f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.5f, 1.4f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= driftTicks || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                // gentle forward momentum with a small upward float
                Vector look = player.getLocation().getDirection().normalize().multiply(0.5);
                look.setY(0.06);
                player.setVelocity(look);

                // soft-blue mist trail
                Location feet = player.getLocation();
                feet.getWorld().spawnParticle(Particle.DUST, feet.clone().add(0, 0.3, 0), 6, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(Sandman.DREAM[(int) (Math.random() * Sandman.DREAM.length)], 1.1f));
                feet.getWorld().spawnParticle(Particle.NOTE, feet.clone().add(0, 0.6, 0), 1, 0.3, 0.2, 0.3, 0.4);

                // enemies passed (within 1.6) or directly below gain +25 Drowsiness
                for (Entity e : player.getNearbyEntities(1.6, 2.5, 1.6)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    Sandman.addDrowsiness(player, le, 25.0 / 3, fLevel); // spread the +25 across passing ticks
                    // L3: enemies below are also slowed
                    if (fLevel >= 3 && le.getLocation().getY() < player.getLocation().getY() - 0.2) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1));
                    }
                }

                // L5: a steady Drowsiness aura while drifting
                if (fLevel >= 5 && t % 10 == 0) {
                    for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                        if (!(e instanceof LivingEntity le) || e == player) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, le)) continue;
                        Sandman.addDrowsiness(player, le, 8, fLevel);
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Drift forward, chilling enemies you pass with Drowsiness.";
            case 2 -> ChatColor.GRAY + "Longer slow-fall and drift.";
            case 3 -> ChatColor.GRAY + "Enemies below you are slowed.";
            case 4 -> ChatColor.GRAY + "Nearby party members also slow-fall.";
            case 5 -> ChatColor.GRAY + "A steady Drowsiness aura trails you as you drift.";
            default -> ChatColor.GRAY + "Drift forward on a dream-mist.";
        };
    }

    @Override
    public String toString() {
        return "dreamdrift";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PHANTOM_MEMBRANE),
                ChatColor.BLUE + "" + ChatColor.BOLD + "[Dreamdrift]",
                ChatColor.GRAY + "Float forward on a soft-blue mist, lulling every",
                ChatColor.GRAY + "enemy you pass with " + ChatColor.BLUE + "Drowsiness" + ChatColor.GRAY + ".");
    }
}
