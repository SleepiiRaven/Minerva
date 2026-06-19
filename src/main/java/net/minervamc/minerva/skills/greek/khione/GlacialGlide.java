package net.minervamc.minerva.skills.greek.khione;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Khione RRR — a fast low slide along a frost trail (no real ice placed). Enemies passed gain
 * Frost; the glide seeds the kit's combo. Levels add slows, length, a chilling trail and a nova.
 */
public class GlacialGlide extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 7500;
            case 4, 5 -> 7000;
            default -> 8000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "glacialGlide")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "glacialGlide", cooldown);
        cooldownAlarm(player, cooldown, "Glacial Glide");

        int durTicks = level >= 3 ? 22 : 18;
        double frostOnPass = switch (level) {
            case 2 -> 25;
            case 3 -> 28;
            case 4, 5 -> 30;
            default -> 20;
        };
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 0.8f, 1.4f);

        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || player.isDead() || !player.isOnline()) {
                    if (fLevel >= 5) nova(player, fLevel);
                    cancel();
                    return;
                }
                Vector v = dir.clone().multiply(0.92);
                v.setY(player.getVelocity().getY());
                player.setVelocity(v);

                Location feet = player.getLocation();
                feet.getWorld().spawnParticle(Particle.SNOWFLAKE, feet, 6, 0.3, 0.05, 0.3, 0.02);
                feet.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, feet, 3, 0.3, 0.05, 0.3, 0.02);
                feet.getWorld().spawnParticle(Particle.DUST, feet.clone().add(0, 0.1, 0), 4, 0.3, 0.05, 0.3, 0,
                        new Particle.DustOptions(Frostbite.FROST[(int) (Math.random() * Frostbite.FROST.length)], 1f));

                for (Entity e : player.getNearbyEntities(1.4, 1.4, 1.4)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    Frostbite.addFrost(player, le, frostOnPass / 3, fLevel);
                    if (fLevel >= 2) le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 0));
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static void nova(Player player, int level) {
        Location c = player.getLocation();
        c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0), 40, 1.5, 0.4, 1.5, 0.1);
        c.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 1.3f);
        for (Entity e : player.getNearbyEntities(4, 3, 4)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            Frostbite.addFrost(player, le, 30, level);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Slide forward on frost, chilling enemies you pass.";
            case 2 -> ChatColor.GRAY + "Passed enemies are also briefly slowed.";
            case 3 -> ChatColor.GRAY + "Longer glide.";
            case 4 -> ChatColor.GRAY + "More Frost applied per pass.";
            case 5 -> ChatColor.GRAY + "The glide ends with a Frost nova.";
            default -> ChatColor.GRAY + "Slide forward on frost.";
        };
    }

    @Override
    public String toString() {
        return "glacialGlide";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ICE),
                ChatColor.AQUA + "" + ChatColor.BOLD + "[Glacial Glide]",
                ChatColor.GRAY + "Skate forward on a trail of frost, applying",
                ChatColor.AQUA + "Frost" + ChatColor.GRAY + " to every enemy you slip past.");
    }
}
