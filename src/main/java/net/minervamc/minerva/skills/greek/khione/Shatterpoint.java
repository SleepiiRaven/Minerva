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
import org.bukkit.util.Vector;

/**
 * Khione RLL — the builder/finisher. Against a Brittle/Encased target it SHATTERS for a burst
 * and sprays shards that seed Frost on nearby enemies (the chain). Against a normal target it
 * deals modest damage and a chunk of Frost.
 */
public class Shatterpoint extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 5500;
            case 4, 5 -> 5000;
            default -> 6000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "shatterpoint")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 4.5);
        if (target == null) {
            player.sendActionBar(ChatColor.AQUA + "No target in range!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "shatterpoint", cooldown);
        cooldownAlarm(player, cooldown, "Shatterpoint");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);

        if (Frostbite.isBrittle(target)) {
            double dmg = switch (level) {
                case 2 -> 12;
                case 3 -> 13;
                case 4 -> 15;
                case 5 -> 17;
                default -> 10;
            };
            shatter(player, target, dmg, level);
        } else {
            double dmg = level >= 4 ? 5 : 3;
            damage(target, dmg, player);
            Frostbite.addFrost(player, target, 40, level);
            Location loc = target.getLocation().clone().add(0, 1, 0);
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 12, 0.3, 0.4, 0.3, 0.05);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK, 1f, 1.2f);
        }
    }

    /** Detonate a Brittle target: burst damage + shards seeding Frost on up to N neighbours. */
    public static void shatter(Player owner, LivingEntity target, double dmg, int level) {
        Location c = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
        damage(target, dmg, owner);
        target.removeScoreboardTag("khioneBrittle");
        target.removeScoreboardTag("khioneEncased");

        // one-shot shard burst
        c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c, 40, 0.4, 0.5, 0.4, 0.25);
        for (Vector v : ParticleUtils.getSpherePoints(1.0, 7)) {
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(Frostbite.FROST[(int) (Math.random() * Frostbite.FROST.length)], 1.3f));
        }
        c.getWorld().playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1.1f);
        c.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1f);

        int maxShards = level >= 2 ? 6 : 4;
        double shardDmg = dmg * 0.4;
        int hit = 0;
        for (Entity e : target.getNearbyEntities(4, 4, 4)) {
            if (hit >= maxShards) break;
            if (!(e instanceof LivingEntity le) || e == owner || e == target) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
            if (PlayerStats.isSummoned(owner, le)) continue;
            // visible shard line to each neighbour
            for (Vector v : ParticleUtils.getLinePoints(c.toVector(), le.getLocation().clone().add(0, le.getHeight() / 2, 0).toVector(), 0.4)) {
                c.getWorld().spawnParticle(Particle.DUST, v.toLocation(c.getWorld()), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(Frostbite.FROST[1], 0.8f));
            }
            damage(le, shardDmg, owner);
            Frostbite.addFrost(owner, le, 30, level);
            hit++;
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Shatter Brittle targets for burst + shard spread.";
            case 2 -> ChatColor.GRAY + "Shards hit up to 6 enemies. Lower cooldown.";
            case 3 -> ChatColor.GRAY + "Higher shatter damage.";
            case 4 -> ChatColor.GRAY + "Even higher damage; normal hits add more Frost.";
            case 5 -> ChatColor.GRAY + "Maximum shatter damage.";
            default -> ChatColor.GRAY + "Shatter Brittle targets.";
        };
    }

    @Override
    public String toString() {
        return "shatterpoint";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PRISMARINE_SHARD),
                ChatColor.AQUA + "" + ChatColor.BOLD + "[Shatterpoint]",
                ChatColor.GRAY + "Strike your target. If it is " + ChatColor.AQUA + "Brittle" + ChatColor.GRAY + ", it",
                ChatColor.GRAY + "SHATTERS — bursting and spraying " + ChatColor.WHITE + "ice shards" + ChatColor.GRAY,
                ChatColor.GRAY + "that seed Frost on nearby enemies. Otherwise, adds Frost.");
    }
}
