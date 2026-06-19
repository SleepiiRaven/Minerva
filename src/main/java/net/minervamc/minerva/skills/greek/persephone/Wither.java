package net.minervamc.minerva.skills.greek.persephone;

import java.util.Iterator;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.persephone.Bloom.BloomPatch;
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
 * Persephone RLL — the death half. Spend 1-3 seeds to drain enemies in an area in front, dealing
 * damage scaled by seeds and healing Persephone for 25% of damage dealt. If cast on or near a
 * Bloom patch, the blossoms wilt and burst — a bonus death AoE (the signature combo). Grey-violet
 * decay motes and steady drain tethers; no strobe.
 */
public class Wither extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 6500;
            case 4, 5 -> 6000;
            default -> 7000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "wither")) {
            onCooldown(player);
            return;
        }

        int available = SeedsOfTheUnderworld.getSeeds(player);
        if (available <= 0) {
            skillLocked(player, "you have no Pomegranate Seeds");
            return;
        }
        int seeds = Math.min(3, available);
        if (!SeedsOfTheUnderworld.spendSeeds(player, seeds)) {
            skillLocked(player, "you have no Pomegranate Seeds");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "wither", cooldown);
        cooldownAlarm(player, cooldown, "Wither");

        double radius = 3.0 + seeds * 0.6;
        double dmg = (switch (level) {
            case 2 -> 4.5;
            case 3 -> 5.0;
            case 4, 5 -> 5.5;
            default -> 4.0;
        }) * seeds;
        final int fLevel = level;

        Location origin = player.getLocation().clone();
        Vector face = player.getEyeLocation().getDirection().setY(0).normalize();
        Location center = origin.clone().add(face.clone().multiply(radius * 0.5)).add(0, 1, 0);

        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.7f);

        // grey-violet falling decay motes (steady, spatial gradient)
        for (Vector v : ParticleUtils.getFilledCirclePoints(radius, 24)) {
            Location loc = center.clone().add(v).add(0, 1.2 + Math.random() * 0.8, 0);
            player.getWorld().spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[v.getZ() > 0 ? 2 : 3], 1.1f));
            player.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 0, 0, 0, 0, 0);
        }

        double totalDealt = 0;
        int killed = 0;
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, 3, radius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;

            double before = le.getHealth();
            damage(le, dmg, player);
            double dealt = Math.max(0, before - le.getHealth());
            totalDealt += dealt;

            // steady drain tether from enemy to Persephone
            Location from = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
            Location to = player.getLocation().clone().add(0, 1, 0);
            for (Vector v : ParticleUtils.getLinePoints(from.toVector(), to.toVector(), 0.5)) {
                player.getWorld().spawnParticle(Particle.DUST, v.toLocation(player.getWorld()), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[2], 0.8f));
            }

            if (fLevel >= 4) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            }
            if (fLevel >= 5 && le.isDead()) {
                killed++;
            }
        }

        // self-heal for 25% of damage dealt
        if (totalDealt > 0) {
            double max = player.getMaxHealth();
            player.setHealth(Math.min(max, player.getHealth() + totalDealt * 0.25));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().clone().add(0, 1.4, 0), 2, 0.2, 0.2, 0.2, 0);
        }

        // L5: a kill regrows a seed
        if (fLevel >= 5) {
            for (int i = 0; i < killed; i++) {
                SeedsOfTheUnderworld.addSeed(player);
            }
        }

        // conversion: detonate a Bloom patch overlapping this cast
        BloomPatch hit = findOverlappingPatch(center, radius);
        if (hit != null) {
            detonatePatch(player, hit, seeds, fLevel);
        }
    }

    private static BloomPatch findOverlappingPatch(Location center, double radius) {
        long now = System.currentTimeMillis();
        Iterator<BloomPatch> it = Bloom.PATCHES.iterator();
        while (it.hasNext()) {
            BloomPatch patch = it.next();
            if (now > patch.expire) {
                it.remove();
                continue;
            }
            if (!patch.center.getWorld().equals(center.getWorld())) continue;
            double reach = radius + patch.radius;
            if (patch.center.distanceSquared(new Location(center.getWorld(), center.getX(), patch.center.getY(), center.getZ())) <= reach * reach) {
                return patch;
            }
        }
        return null;
    }

    private static void detonatePatch(Player player, BloomPatch patch, int seeds, int level) {
        Bloom.PATCHES.remove(patch);
        Location c = patch.center.clone().add(0, 1, 0);
        double burstRadius = patch.radius + (level >= 3 ? 1.5 : 0.5);
        double burstDmg = (level >= 3 ? 8.0 : 6.0) + seeds * 2.0;

        player.getWorld().playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.6f);
        player.getWorld().playSound(c, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.5f);

        // single-shot wilt burst: pink blossoms wilting to violet (steady spatial gradient ring)
        for (Vector v : ParticleUtils.getCirclePoints(burstRadius, Math.max(16, (int) (burstRadius * 10)))) {
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(SeedsOfTheUnderworld.PALETTE[2], 1.3f));
        }
        c.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, c, 20, burstRadius * 0.5, 0.6, burstRadius * 0.5, 0);

        for (Entity e : c.getWorld().getNearbyEntities(c, burstRadius, 3, burstRadius)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            damage(le, burstDmg, player);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Drain enemies ahead; heal for 25% of damage dealt.";
            case 2 -> ChatColor.GRAY + "More damage per seed spent.";
            case 3 -> ChatColor.GRAY + "Bloom-patch conversion bursts are bigger.";
            case 4 -> ChatColor.GRAY + "The drain also slows enemies.";
            case 5 -> ChatColor.GRAY + "A Wither that kills regrows a seed.";
            default -> ChatColor.GRAY + "Drain enemies and heal yourself.";
        };
    }

    @Override
    public String toString() {
        return "wither";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.WITHER_ROSE),
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[Wither]",
                ChatColor.GRAY + "Spend up to " + ChatColor.LIGHT_PURPLE + "3 seeds" + ChatColor.GRAY + " to drain enemies ahead,",
                ChatColor.GRAY + "healing yourself. Cast it on a " + ChatColor.GREEN + "Bloom" + ChatColor.GRAY + " patch to",
                ChatColor.GRAY + "wilt the blossoms into a " + ChatColor.DARK_PURPLE + "death-burst" + ChatColor.GRAY + ".");
    }
}
