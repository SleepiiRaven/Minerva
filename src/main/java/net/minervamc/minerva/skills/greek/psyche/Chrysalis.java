package net.minervamc.minerva.skills.greek.psyche;

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
 * Psyche RRL (utility) — cocoon for about a second: heavy RESISTANCE + SLOWNESS lock you in a
 * steady DUST lattice, then burst into heal + cleanse + a small outward knockback ring. The
 * defensive beat of the kit. No strobe — the cocoon shell is a fixed-gradient DUST lattice that
 * is redrawn smoothly, and the burst is a single one-shot fan.
 */
public class Chrysalis extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 15000;
            case 4, 5 -> 14000;
            default -> 16000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "chrysalis")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "chrysalis", cooldown);
        cooldownAlarm(player, cooldown, "Chrysalis");

        final int fLevel = level;
        final int cocoonTicks = level >= 3 ? 16 : 20;

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, cocoonTicks, 250));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, cocoonTicks, 250));
        player.addScoreboardTag("psycheCocoon");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.4f);

        // steady cocoon shell lattice
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= cocoonTicks || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                if (t % 2 == 0) {
                    World w = player.getWorld();
                    Location c = player.getLocation().clone().add(0, 1, 0);
                    int idx = 0;
                    for (Vector v : ParticleUtils.getSpherePoints(0.8, 7)) {
                        w.spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(IridescentSoul.IRIDESCENT[idx++ % IridescentSoul.IRIDESCENT.length], 0.8f));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // burst on emergence
        new BukkitRunnable() {
            @Override
            public void run() {
                burst(player, fLevel);
            }
        }.runTaskLater(Minerva.getInstance(), cocoonTicks);
    }

    private static void burst(Player player, int level) {
        player.removeScoreboardTag("psycheCocoon");
        if (player.isDead() || !player.isOnline()) return;
        World w = player.getWorld();
        Location c = player.getLocation();

        // heal + cleanse self
        double heal = switch (level) {
            case 2, 3 -> 8;
            case 4, 5 -> 10;
            default -> 6;
        };
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
        cleanse(player);

        // L4: burst also heals party nearby (include caster, already healed)
        if (level >= 4) {
            List<Player> party = Party.partyList(player);
            if (party != null) {
                for (Player ally : party) {
                    if (ally == null || ally == player || !ally.isOnline()) continue;
                    if (ally.getWorld().equals(w) && ally.getLocation().distanceSquared(c) <= 64) {
                        ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + heal));
                        cleanse(ally);
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().clone().add(0, 1.6, 0), 2, 0.3, 0.3, 0.3, 0);
                    }
                }
            }
        }

        // L5: emerging grants the wings buff (SPEED) for 3s
        if (level >= 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
        }

        // one-shot knockback ring + petal fan (CHERRY_LEAVES / SPORE_BLOSSOM_AIR)
        w.spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 1, 0), 30, 1.0, 0.5, 1.0, 0.1);
        w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, c.clone().add(0, 1, 0), 20, 1.2, 0.6, 1.2, 0.02);
        int idx = 0;
        for (Vector v : ParticleUtils.getCirclePoints(1.4, 24)) {
            w.spawnParticle(Particle.DUST, c.clone().add(v).add(0, 0.6, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(IridescentSoul.IRIDESCENT[idx++ % IridescentSoul.IRIDESCENT.length], 1f));
        }
        w.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.3f);
        w.playSound(c, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.5f);
        w.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.5f);

        double ringRadius = 4.0;
        for (Entity e : player.getNearbyEntities(ringRadius, 3, ringRadius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            Vector away = le.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (away.lengthSquared() < 0.04) away = new Vector(0, 0, 1);
            away.normalize().multiply(0.55);
            away.setY(0.32);
            knockback(le, away);
        }
    }

    private static void cleanse(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        unfear(player);
        wake(player);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Cocoon briefly, then burst to heal, cleanse, and knock back.";
            case 2 -> ChatColor.GRAY + "Greater heal on emergence.";
            case 3 -> ChatColor.GRAY + "Shorter cocoon before the burst.";
            case 4 -> ChatColor.GRAY + "The burst also heals nearby party members.";
            case 5 -> ChatColor.GRAY + "Emerging grants wings (Speed) for 3 seconds.";
            default -> ChatColor.GRAY + "Cocoon, then burst to heal and cleanse.";
        };
    }

    @Override
    public String toString() {
        return "chrysalis";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.HONEYCOMB),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Chrysalis]",
                ChatColor.GRAY + "Wrap yourself in a cocoon — nearly invulnerable but",
                ChatColor.GRAY + "rooted — then " + ChatColor.LIGHT_PURPLE + "burst" + ChatColor.GRAY + " free, healed and cleansed,",
                ChatColor.GRAY + "scattering foes around you.");
    }
}
