package net.minervamc.minerva.skills.greek.iris;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
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
 * Iris RLL — the precise payoff. A dense single-hue beam strikes one target for base damage plus
 * a bonus for each nearby differently-colored enemy (the Contrast rule). The beam then refracts
 * thin DUST lines to each contrasting neighbour, grazing them for partial damage. Higher ranks
 * refract to more enemies, apply a small WITHER DoT, add base damage, and on a kill recolor the
 * survivors for a follow-up.
 */
public class RefractionLance extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 6500;
            case 4, 5 -> 6000;
            default -> 7000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "refractionLance")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 18);
        if (target == null) {
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "No target in sight!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "refractionLance", cooldown);
        cooldownAlarm(player, cooldown, "Refraction Lance");

        int contrast = Prism.contrastCount(target, 5);
        double base = level >= 4 ? 9 : 7;
        double dmg = base + 3 * contrast;

        // dense single-hue beam to the primary target
        int tColor = Prism.getColor(target);
        Color beamHue = tColor >= 0 ? Prism.PALETTE[tColor] : Color.WHITE;
        Location eye = player.getEyeLocation();
        Location tCenter = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
        for (Vector v : ParticleUtils.getLinePoints(eye.toVector(), tCenter.toVector(), 0.35)) {
            eye.getWorld().spawnParticle(Particle.DUST, v.toLocation(eye.getWorld()), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(beamHue, 1.1f));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);

        double primaryHp = target.getHealth();
        damage(target, dmg, player);
        // one-shot prism burst on the primary (single pulse, no strobe)
        for (Color c : Prism.PALETTE) {
            tCenter.getWorld().spawnParticle(Particle.DUST, tCenter, 6, 0.4, 0.5, 0.4, 0,
                    new Particle.DustOptions(c, 1.2f));
        }
        boolean primaryDied = target.isDead() || primaryHp - dmg <= 0;

        // refract thin lines to each contrasting neighbour for partial damage
        int maxRefract = switch (level) {
            case 2, 3 -> 5;
            case 4, 5 -> 7;
            default -> 3;
        };
        double partial = dmg * 0.4;
        int mine = Prism.getColor(target);
        int hit = 0;
        for (Entity e : target.getNearbyEntities(5, 5, 5)) {
            if (hit >= maxRefract) break;
            if (!(e instanceof LivingEntity le) || e == player || e == target) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            int c = Prism.getColor(le);
            if (c == -1 || c == mine) continue;
            Location leCenter = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
            for (Vector v : ParticleUtils.getLinePoints(tCenter.toVector(), leCenter.toVector(), 0.4)) {
                tCenter.getWorld().spawnParticle(Particle.DUST, v.toLocation(tCenter.getWorld()), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(Prism.PALETTE[c], 0.8f));
            }
            damage(le, partial, player);
            if (level >= 3) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            }
            hit++;
        }
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.9f);

        // L5: a kill recolors nearby enemies for an instant follow-up spread
        if (level >= 5 && primaryDied) {
            int idx = 0;
            for (Entity e : tCenter.getNearbyEntities(6, 6, 6)) {
                if (!(e instanceof LivingEntity le) || e == player) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                if (PlayerStats.isSummoned(player, le)) continue;
                Prism.paint(player, le, idx);
                idx++;
            }
            tCenter.getWorld().playSound(tCenter, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.6f);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Beam one target; bonus per nearby clashing color, refracts to them.";
            case 2 -> ChatColor.GRAY + "Refracts to more enemies.";
            case 3 -> ChatColor.GRAY + "Refracted hits apply a small Wither.";
            case 4 -> ChatColor.GRAY + "Higher base damage and more refractions.";
            case 5 -> ChatColor.GRAY + "A kill recolors nearby enemies for a follow-up.";
            default -> ChatColor.GRAY + "Beam one target with contrast bonus.";
        };
    }

    @Override
    public String toString() {
        return "refractionLance";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.AMETHYST_SHARD),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Refraction Lance]",
                ChatColor.GRAY + "Fire a beam at one target, dealing bonus damage",
                ChatColor.GRAY + "per nearby " + ChatColor.LIGHT_PURPLE + "differently-colored" + ChatColor.GRAY + " enemy, then",
                ChatColor.GRAY + "refracting to graze each of them.");
    }
}
