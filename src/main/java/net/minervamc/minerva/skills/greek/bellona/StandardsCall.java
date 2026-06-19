package net.minervamc.minerva.skills.greek.bellona;

import java.util.ArrayList;
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
 * Bellona RRL — sound the standards. Every banner surges for 4s: allies near any banner are
 * empowered, enemies near any banner are crippled. The team-fight button; rewards a dense,
 * forward battery rather than scattered emplacements.
 */
public class StandardsCall extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 19000;
            case 4, 5 -> 18000;
            default -> 20000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "standardsCall")) {
            onCooldown(player);
            return;
        }

        List<WarBanner> banners = PlantWarBanner.getBanners(player.getUniqueId());
        if (banners.isEmpty()) {
            player.sendActionBar(ChatColor.RED + "You have no banners to rally!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "standardsCall", cooldown);
        cooldownAlarm(player, cooldown, "Standards Call");

        final int fLevel = level;
        final int durTicks = 80; // 4 seconds

        player.getWorld().playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 0.9f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1.1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);

        // L4: extend all banner lifetimes
        if (fLevel >= 4) {
            for (WarBanner b : banners) {
                b.lifetime += 120;
            }
        }
        // L5: double pulse rate for the duration
        if (fLevel >= 5) {
            for (WarBanner b : banners) {
                b.stand.addScoreboardTag("bellonaSurgeFast");
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (WarBanner b : PlantWarBanner.getBanners(player.getUniqueId())) {
                        b.stand.removeScoreboardTag("bellonaSurgeFast");
                    }
                }
            }.runTaskLater(Minerva.getInstance(), durTicks);
        }

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                List<WarBanner> live = PlantWarBanner.getBanners(player.getUniqueId());
                if (t >= durTicks || live.isEmpty()) {
                    cancel();
                    return;
                }

                for (WarBanner b : live) {
                    if (b.stand == null || !b.stand.isValid()) continue;
                    Location base = b.stand.getLocation();
                    World w = base.getWorld();

                    // vertical DUST helix + ground ring per banner (steady, no strobe)
                    if (t % 2 == 0) {
                        double y = ((t % 20) / 20.0) * 2.4;
                        double ang1 = (t / 20.0) * Math.PI * 2;
                        for (int k = 0; k < 2; k++) {
                            double ang = ang1 + k * Math.PI;
                            Vector off = new Vector(Math.cos(ang) * 0.6, y, Math.sin(ang) * 0.6);
                            w.spawnParticle(Particle.DUST, base.clone().add(off), 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(PlantWarBanner.CRIMSON[2], 1f));
                        }
                    }
                    if (t % 6 == 0) {
                        for (Vector v : ParticleUtils.getCirclePoints(4.0, 24)) {
                            w.spawnParticle(Particle.DUST, base.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(PlantWarBanner.CRIMSON[1], 0.9f));
                        }
                    }

                    // apply effects every 20 ticks
                    if (t % 20 == 0) {
                        // allies near this banner
                        List<Player> allies = new ArrayList<>();
                        List<Player> party = Party.partyList(player);
                        if (party != null) allies.addAll(party);
                        if (!allies.contains(player)) allies.add(player);
                        for (Player ally : allies) {
                            if (!ally.isOnline() || ally.getWorld() != w) continue;
                            if (ally.getLocation().distanceSquared(base) <= 16) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 30, 1));
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0));
                                if (fLevel >= 2) ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 0));
                            }
                        }
                        // enemies near this banner
                        for (Entity e : b.stand.getNearbyEntities(4, 3, 4)) {
                            if (!(e instanceof LivingEntity le) || le == player) continue;
                            if (le.getScoreboardTags().contains("bellonaBanner")) continue;
                            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                            if (PlayerStats.isSummoned(player, le)) continue;
                            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                            if (fLevel >= 3 && t == 0) fear(player, le, 20);
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
            case 1 -> ChatColor.GRAY + "Banners surge 4s: empower allies, cripple enemies.";
            case 2 -> ChatColor.GRAY + "Allies also gain Regeneration.";
            case 3 -> ChatColor.GRAY + "Enemies near a banner are briefly feared.";
            case 4 -> ChatColor.GRAY + "Extends all banner lifetimes.";
            case 5 -> ChatColor.GRAY + "Doubles banner pulse rate for the duration.";
            default -> ChatColor.GRAY + "Banners surge: empower allies, cripple enemies.";
        };
    }

    @Override
    public String toString() {
        return "standardsCall";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.GOAT_HORN),
                ChatColor.RED + "" + ChatColor.BOLD + "[Standards Call]",
                ChatColor.GRAY + "Sound the standards: every " + ChatColor.RED + "banner" + ChatColor.GRAY + " surges,",
                ChatColor.GRAY + "empowering nearby allies and crippling",
                ChatColor.GRAY + "enemies caught in their shadow.");
    }
}
