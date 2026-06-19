package net.minervamc.minerva.skills.greek.arke;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Arke RRR (movement) — the puppeteer's carry. Dash a short distance, and every enemy chained to
 * you is hauled along behind. Levels lengthen the dash, add collision damage on arrival, slow the
 * dragged, and allow a short blink through one block.
 */
public class Reel extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "reel")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "reel", cooldown);
        cooldownAlarm(player, cooldown, "Reel");

        double power = switch (level) {
            case 2, 3 -> 1.5;
            case 4 -> 1.6;
            case 5 -> 1.7;
            default -> 1.3;
        };
        final int fLevel = level;

        Vector dir = player.getEyeLocation().getDirection().normalize();
        Vector dash = dir.clone().multiply(power);
        dash.setY(Math.max(0.35, dash.getY() + 0.25));

        // L5: a short blink — nudge forward so a single thin wall is passed.
        if (level >= 5) {
            Location ahead = player.getLocation().clone().add(dir.clone().multiply(2.5));
            ahead.setY(player.getLocation().getY());
            if (ahead.getBlock().isPassable() && ahead.clone().add(0, 1, 0).getBlock().isPassable()) {
                player.teleport(ahead);
                player.getWorld().playSound(ahead, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.3f);
            }
        }

        player.setVelocity(dash);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.8f);

        // drag every chained enemy toward the caster, over a few ticks, with a desaturated streak
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 8 || player.isDead() || !player.isOnline()) {
                    if (fLevel >= 3) collide(player, fLevel);
                    cancel();
                    return;
                }
                // caster's own washed-out streak
                Location feet = player.getLocation();
                World w = feet.getWorld();
                Color c = SharedFate.PALETTE[t % SharedFate.PALETTE.length];
                w.spawnParticle(Particle.DUST, feet.clone().add(0, 0.4, 0), 4, 0.2, 0.2, 0.2, 0,
                        new Particle.DustOptions(c, 1f));

                List<LivingEntity> dragged = SharedFate.getChainOf(player);
                for (LivingEntity le : dragged) {
                    if (le == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    if (le.getWorld() != player.getWorld()) continue;
                    Vector toPlayer = player.getLocation().toVector()
                            .subtract(le.getLocation().toVector());
                    double dist = toPlayer.length();
                    if (dist < 2.0 || dist < 0.01) continue;
                    Vector pull = toPlayer.normalize().multiply(0.55);
                    pull.setY(Math.max(0.15, pull.getY()));
                    le.setVelocity(pull);
                    // dragged-chain dust trail
                    le.getWorld().spawnParticle(Particle.DUST, le.getLocation().clone().add(0, le.getHeight() / 2, 0),
                            3, 0.2, 0.2, 0.2, 0,
                            new Particle.DustOptions(SharedFate.PALETTE[(t + 2) % SharedFate.PALETTE.length], 0.9f));
                    if (fLevel >= 4) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** L3+: enemies dragged close take a small collision hit on arrival. */
    private static void collide(Player player, int level) {
        double dmg = level >= 5 ? 6 : level >= 4 ? 5 : 4;
        for (LivingEntity le : SharedFate.getChainOf(player)) {
            if (le == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (le.getWorld() != player.getWorld()) continue;
            if (le.getLocation().distance(player.getLocation()) <= 3.0) {
                damage(le, dmg, player);
                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().clone().add(0, le.getHeight() / 2, 0),
                        8, 0.2, 0.3, 0.2, 0.05);
                le.getWorld().playSound(le.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
            }
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Dash forward, dragging chained enemies behind you.";
            case 2 -> ChatColor.GRAY + "Longer dash.";
            case 3 -> ChatColor.GRAY + "Dragged enemies take collision damage on arrival.";
            case 4 -> ChatColor.GRAY + "Dragged enemies are also slowed.";
            case 5 -> ChatColor.GRAY + "May blink through one thin wall while reeling.";
            default -> ChatColor.GRAY + "Dash forward, hauling your chained foes along.";
        };
    }

    @Override
    public String toString() {
        return "reel";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.LEAD),
                ChatColor.GRAY + "" + ChatColor.BOLD + "[Reel]",
                ChatColor.GRAY + "Dash a short distance and " + ChatColor.WHITE + "haul" + ChatColor.GRAY + " every enemy",
                ChatColor.GRAY + "chained to you along behind — keep them close,",
                ChatColor.GRAY + "keep them bound, keep them sharing your fate.");
    }
}
