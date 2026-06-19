package net.minervamc.minerva.skills.greek.persephone;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Persephone RRL — the big-spend seasonal swing. Costs 4 seeds. Over a large area for 5s, the
 * queen of two worlds blesses her party (Spring: regen + speed) while cursing enemies caught in
 * the same field (Winter: slow + a small Wither decay). Steady petals for allies, steady violet
 * ash for enemies — spatial gradient, no strobe.
 */
public class QueensDecree extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 17000;
            case 4, 5 -> 16000;
            default -> 18000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "queensDecree")) {
            onCooldown(player);
            return;
        }

        if (SeedsOfTheUnderworld.getSeeds(player) < 4) {
            skillLocked(player, "you need 4 Pomegranate Seeds");
            return;
        }
        if (!SeedsOfTheUnderworld.spendSeeds(player, 4)) {
            skillLocked(player, "you need 4 Pomegranate Seeds");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "queensDecree", cooldown);
        cooldownAlarm(player, cooldown, "Queen's Decree");

        double radius = switch (level) {
            case 2, 3 -> 9.0;
            case 4, 5 -> 10.0;
            default -> 8.0;
        };
        int durTicks = switch (level) {
            case 3, 4 -> 120;
            case 5 -> 120;
            default -> 100;
        };
        final int lingerTicks = level >= 5 ? 40 : 0;
        final int fLevel = level;
        final Location center = player.getLocation().clone();

        player.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.9f);
        player.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                int total = durTicks + lingerTicks;
                if (t >= total || !player.isOnline()) {
                    cancel();
                    return;
                }
                boolean active = t < durTicks;

                // steady seasonal field: spring petals + winter violet ash (every 5 ticks)
                if (t % 5 == 0) {
                    for (Vector v : ParticleUtils.getCirclePoints(radius, 28)) {
                        Location loc = center.clone().add(v).add(0, 0.1, 0);
                        // spatial split: spring on +X half, winter on -X half (fixed, not random per frame)
                        if (v.getX() >= 0) {
                            center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc.clone().add(0, 0.6, 0), 0, 0, 0, 0, 0);
                            center.getWorld().spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[1], 1.1f));
                        } else {
                            center.getWorld().spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[3], 1.1f));
                            center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc.clone().add(0, 0.5, 0), 0, 0, 0, 0, 0);
                        }
                    }
                }

                if (active && t % 20 == 0) {
                    // bless party allies inside (Spring)
                    for (Player ally : alliesOf(player)) {
                        if (!ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(center) > radius * radius) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0));
                        ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().clone().add(0, 1.2, 0), 4, 0.3, 0.4, 0.3, 0);
                    }
                    // curse enemies inside (Winter)
                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, 4, radius)) {
                        if (!(e instanceof LivingEntity le) || e == player) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, le)) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static List<Player> alliesOf(Player player) {
        List<Player> allies = new ArrayList<>();
        allies.add(player);
        List<Player> party = Party.partyList(player);
        if (party != null) {
            for (Player p : party) {
                if (p != null && !allies.contains(p)) allies.add(p);
            }
        }
        return allies;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Decree the seasons: bless allies, curse enemies (5s).";
            case 2 -> ChatColor.GRAY + "Larger area.";
            case 3 -> ChatColor.GRAY + "Longer duration.";
            case 4 -> ChatColor.GRAY + "Even larger area.";
            case 5 -> ChatColor.GRAY + "The seasonal field lingers 2s after it ends.";
            default -> ChatColor.GRAY + "Bless allies and curse enemies in a great area.";
        };
    }

    @Override
    public String toString() {
        return "queensDecree";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.GOLDEN_APPLE),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Queen's Decree]",
                ChatColor.GRAY + "Spend " + ChatColor.LIGHT_PURPLE + "4 seeds" + ChatColor.GRAY + " to turn the seasons over a",
                ChatColor.GRAY + "great area: " + ChatColor.GREEN + "Spring" + ChatColor.GRAY + " heals and hastens your party while",
                ChatColor.GRAY + "" + ChatColor.DARK_PURPLE + "Winter" + ChatColor.GRAY + " slows and withers your enemies.");
    }
}
