package net.minervamc.minerva.skills.greek.hestia;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
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
 * Hestia RRR — a short forward surge that leaves a lingering warm haze behind. Enemies who enter
 * the haze are slowed (and, later, lightly burned); party allies who pass through it are cleansed
 * of one debuff. A mobility + screen tool that protects the bank.
 */
public class EmberVeil extends Skill {
    private static final PotionEffectType[] CLEANSEABLE = {
        PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.MINING_FATIGUE,
        PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.BLINDNESS,
        PotionEffectType.NAUSEA, PotionEffectType.DARKNESS,
    };

    private static int cleanseOne(LivingEntity le) {
        for (PotionEffectType type : CLEANSEABLE) {
            if (le.hasPotionEffect(type)) {
                le.removePotionEffect(type);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 10500;
            case 4, 5 -> 10000;
            default -> 11000;
        };
        String key = "emberVeil";

        // L5: allow a single recast within 3s of the first use (free window, no normal cd check)
        boolean recastWindow = level >= 5 && player.getScoreboardTags().contains("hestiaVeilRecast");
        if (!recastWindow && !cooldownManager.isCooldownDone(player.getUniqueId(), key)) {
            onCooldown(player);
            return;
        }

        if (recastWindow) {
            player.removeScoreboardTag("hestiaVeilRecast");
        } else {
            cooldownManager.setCooldownFromNow(player.getUniqueId(), key, cooldown);
            cooldownAlarm(player, cooldown, "Ember Veil");
            if (level >= 5) {
                player.addScoreboardTag("hestiaVeilRecast");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.removeScoreboardTag("hestiaVeilRecast");
                    }
                }.runTaskLater(Minerva.getInstance(), 60L);
            }
        }

        // forward surge
        Vector dir = player.getLocation().getDirection().setY(0).normalize().multiply(1.1);
        dir.setY(0.25);
        player.setVelocity(dir);

        if (level >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 0));
        }

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.3f);
        world.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 0.6f, 1.4f);
        world.playSound(player.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.8f, 1.1f);

        final int fLevel = level;
        final int hazeTicks = level >= 4 ? 80 : 60; // ~3s base, ~4s at L4
        final boolean burns = level >= 3;

        // lay the lingering haze along the surge path; each puff is its own short-lived field
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 14 || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location puff = player.getLocation().clone().add(0, 0.6, 0);
                world.spawnParticle(Particle.WHITE_ASH, puff, 6, 0.4, 0.5, 0.4, 0.0);
                List<Vector> ring = ParticleUtils.getCirclePoints(0.8, 8);
                for (int i = 0; i < ring.size(); i++) {
                    Color c = BankedEmbers.HEARTH[i % BankedEmbers.HEARTH.length];
                    world.spawnParticle(Particle.DUST, puff.clone().add(ring.get(i)), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(c, 1f));
                }
                spawnHaze(player, puff.clone(), fLevel, hazeTicks, burns);
                t += 2;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 2L);
    }

    /** A stationary warm field at {@code center} that slows enemies, cleanses allies, may burn. */
    private static void spawnHaze(Player owner, Location center, int level, int lifeTicks, boolean burns) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= lifeTicks || world == null) {
                    cancel();
                    return;
                }
                // steady haze visual (no strobe)
                if (t % 4 == 0) {
                    world.spawnParticle(Particle.WHITE_ASH, center.clone().add(0, 0.4, 0), 3, 0.5, 0.4, 0.5, 0.0);
                    Color c = BankedEmbers.HEARTH[(t / 4) % BankedEmbers.HEARTH.length];
                    world.spawnParticle(Particle.DUST, center.clone().add(0, 0.3, 0), 2, 0.5, 0.3, 0.5, 0,
                            new Particle.DustOptions(c, 1f));
                }
                // effect tick every 10
                if (t % 10 == 0) {
                    for (Entity e : world.getNearbyEntities(center, 1.6, 1.6, 1.6)) {
                        if (!(e instanceof LivingEntity le)) continue;
                        if (le instanceof Player p && (p == owner || Party.isPlayerInPlayerParty(owner, p))) {
                            cleanseOne(le);
                            continue;
                        }
                        if (le == owner) continue;
                        if (PlayerStats.isSummoned(owner, le)) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, level >= 4 ? 1 : 0));
                        if (burns && t % 20 == 0) {
                            damage(le, 1, owner, true, true);
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Surge forward, leaving a haze that slows foes and cleanses allies.";
            case 2 -> ChatColor.GRAY + "Gain Absorption on use.";
            case 3 -> ChatColor.GRAY + "The haze lightly burns enemies inside it.";
            case 4 -> ChatColor.GRAY + "Longer-lasting, stronger haze.";
            case 5 -> ChatColor.GRAY + "You may surge a second time within 3 seconds.";
            default -> ChatColor.GRAY + "Surge forward and leave a protective haze.";
        };
    }

    @Override
    public String toString() {
        return "emberVeil";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BLAZE_POWDER),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Ember Veil]",
                ChatColor.GRAY + "Surge forward and leave a lingering " + ChatColor.GOLD + "warm haze" + ChatColor.GRAY + ".",
                ChatColor.GRAY + "Enemies who enter are " + ChatColor.GOLD + "slowed" + ChatColor.GRAY + "; allies who pass",
                ChatColor.GRAY + "are " + ChatColor.GOLD + "cleansed" + ChatColor.GRAY + " of a debuff.");
    }
}
