package net.minervamc.minerva.skills.greek.bellona;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.bellona.PlantWarBanner.WarBanner;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Bellona RLL — every active banner of yours launches an arcing shell that converges on the
 * area you aim at. Damage scales with banner count: the more emplacements you've placed, the
 * harder the volley. The payoff for building and advancing your battery.
 */
public class Cannonade extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 9500;
            case 4, 5 -> 9000;
            default -> 10000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "cannonade")) {
            onCooldown(player);
            return;
        }

        List<WarBanner> banners = PlantWarBanner.getBanners(player.getUniqueId());
        if (banners.isEmpty()) {
            player.sendActionBar(ChatColor.RED + "You have no banners to fire!");
            return;
        }

        // aim point: the targeted block, falling back to a point ahead of the player
        Location aim = player.getTargetBlockExact(24) != null
                ? player.getTargetBlockExact(24).getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(12));

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "cannonade", cooldown);
        cooldownAlarm(player, cooldown, "Cannonade");

        final int fLevel = level;
        final int count = banners.size();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 0.9f);
        for (WarBanner b : banners) {
            launchShell(player, b.bannerTop(), aim.clone());
        }

        // converge the impact after the shells have arced over (~14 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                impact(player, aim.clone(), fLevel, count);
            }
        }.runTaskLater(Minerva.getInstance(), 14L);

        // L5: a second smaller volley 1s later
        if (fLevel >= 5) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<WarBanner> nowBanners = PlantWarBanner.getBanners(player.getUniqueId());
                    for (WarBanner b : nowBanners) {
                        launchShell(player, b.bannerTop(), aim.clone());
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            impact(player, aim.clone(), fLevel, Math.max(1, nowBanners.size() - 1));
                        }
                    }.runTaskLater(Minerva.getInstance(), 14L);
                }
            }.runTaskLater(Minerva.getInstance(), 20L);
        }
    }

    private static void launchShell(Player player, Location from, Location to) {
        World w = from.getWorld();
        // arc control point lifted between start and end
        Vector mid = from.toVector().add(to.toVector()).multiply(0.5).add(new Vector(0, 5, 0));
        List<Vector> arc = ParticleUtils.getNthBezierPoints(20, from.toVector(), mid, to.toVector());
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= arc.size()) {
                    cancel();
                    return;
                }
                int steps = Math.min(arc.size(), i + 2);
                for (; i < steps; i++) {
                    w.spawnParticle(Particle.DUST, arc.get(i).toLocation(w), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(PlantWarBanner.CRIMSON[2], 1f));
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        w.playSound(from, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.4f);
    }

    private static void impact(Player player, Location aim, int level, int count) {
        World w = aim.getWorld();
        // one shared impact burst
        w.spawnParticle(Particle.EXPLOSION, aim, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.DUST, aim, 24, 1.2, 0.6, 1.2, 0,
                new Particle.DustOptions(PlantWarBanner.CRIMSON[0], 1.3f));
        for (Vector v : ParticleUtils.getCirclePoints(3.0, 26)) {
            w.spawnParticle(Particle.DUST, aim.clone().add(v).add(0, 0.15, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(PlantWarBanner.CRIMSON[1], 1f));
        }
        w.playSound(aim, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.1f);
        w.playSound(aim, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.8f);

        double dmg = 5 + 3 * count;
        boolean primaryStunned = false;
        for (Entity e : w.getNearbyEntities(aim, 3, 3, 3)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le.getScoreboardTags().contains("bellonaBanner")) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            damage(le, dmg, player);
            if (level >= 2) {
                Vector kb = le.getLocation().toVector().subtract(aim.toVector()).setY(0);
                if (kb.lengthSquared() < 0.04) kb = new Vector(0, 0, 1);
                kb.normalize().multiply(0.5).setY(0.3);
                knockback(le, kb);
            }
            if (level >= 4 && !primaryStunned) {
                stun(player, le, 10);
                primaryStunned = true;
            }
        }

        // L3: leave a 2s slow scorch field
        if (level >= 3) {
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (t >= 40) {
                        cancel();
                        return;
                    }
                    if (t % 4 == 0) {
                        for (Vector v : ParticleUtils.getCirclePoints(2.6, 18)) {
                            w.spawnParticle(Particle.DUST, aim.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(PlantWarBanner.CRIMSON[3], 1f));
                        }
                    }
                    if (t % 10 == 0) {
                        for (Entity e : w.getNearbyEntities(aim, 2.6, 2, 2.6)) {
                            if (!(e instanceof LivingEntity le) || e == player) continue;
                            if (le.getScoreboardTags().contains("bellonaBanner")) continue;
                            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                            if (PlayerStats.isSummoned(player, le)) continue;
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1));
                        }
                    }
                    t++;
                }
            }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Every banner fires a shell; damage scales with banner count.";
            case 2 -> ChatColor.GRAY + "Impact knocks enemies back.";
            case 3 -> ChatColor.GRAY + "Leaves a 2s slowing scorch field.";
            case 4 -> ChatColor.GRAY + "Briefly stuns the primary target.";
            case 5 -> ChatColor.GRAY + "A second smaller volley follows.";
            default -> ChatColor.GRAY + "Every banner fires a converging shell.";
        };
    }

    @Override
    public String toString() {
        return "cannonade";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.FIRE_CHARGE),
                ChatColor.RED + "" + ChatColor.BOLD + "[Cannonade]",
                ChatColor.GRAY + "Every active " + ChatColor.RED + "banner" + ChatColor.GRAY + " launches an arcing",
                ChatColor.GRAY + "shell that converges on the area you aim at.",
                ChatColor.GRAY + "Damage scales with your banner count.");
    }
}
