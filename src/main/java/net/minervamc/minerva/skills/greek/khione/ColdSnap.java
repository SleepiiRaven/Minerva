package net.minervamc.minerva.skills.greek.khione;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Khione RRL — instantly max one target's Frost, forcing an Encase. The guaranteed lock on a
 * key threat; long cooldown so it's a decision, not stun-spam.
 */
public class ColdSnap extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 13000;
            case 4, 5 -> 12000;
            default -> 14000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "coldSnap")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 16);
        if (target == null) {
            player.sendActionBar(ChatColor.AQUA + "No target in sight!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "coldSnap", cooldown);
        cooldownAlarm(player, cooldown, "Cold Snap");

        // line of frost darts to the target
        Location eye = player.getEyeLocation();
        for (Vector v : ParticleUtils.getLinePoints(eye.toVector(), target.getLocation().clone().add(0, target.getHeight() / 2, 0).toVector(), 0.5)) {
            eye.getWorld().spawnParticle(Particle.SNOWFLAKE, v.toLocation(eye.getWorld()), 1, 0, 0, 0, 0);
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.8f);

        Frostbite.addFrost(player, target, 200, level); // overfill → forces encase
        Frostbite.encase(player, target, level);

        if (level >= 5) {
            LivingEntity second = nearestOther(player, target, 6);
            if (second != null) {
                Frostbite.addFrost(player, second, 200, level);
                Frostbite.encase(player, second, level);
            }
        }

        if (level >= 2) {
            for (Entity e : target.getNearbyEntities(4, 3, 4)) {
                if (!(e instanceof LivingEntity le) || e == player || e == target) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                if (PlayerStats.isSummoned(player, le)) continue;
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, level >= 4 ? 40 : 30, 1));
            }
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
            case 1 -> ChatColor.GRAY + "Instantly Encase the target you aim at.";
            case 2 -> ChatColor.GRAY + "Also slows nearby enemies.";
            case 3 -> ChatColor.GRAY + "Lower cooldown.";
            case 4 -> ChatColor.GRAY + "Stronger surrounding slow.";
            case 5 -> ChatColor.GRAY + "Encases a second nearby enemy too.";
            default -> ChatColor.GRAY + "Instantly Encase a target.";
        };
    }

    @Override
    public String toString() {
        return "coldSnap";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BLUE_ICE),
                ChatColor.AQUA + "" + ChatColor.BOLD + "[Cold Snap]",
                ChatColor.GRAY + "Instantly fill a target's Frost, forcing an",
                ChatColor.AQUA + "Encase" + ChatColor.GRAY + " — your guaranteed lock on a key threat.");
    }
}
