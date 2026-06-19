package net.minervamc.minerva.skills.greek.iris;

import java.util.ArrayList;
import java.util.List;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Iris RLR — the spreader. Paint every enemy in a forward cone, each a DIFFERENT color (cycling
 * the palette per target to maximize future contrast). A fan of spatial-rainbow DUST sweeps the
 * cone. Higher ranks widen the cone, briefly slow recolored enemies, guarantee distinct colors,
 * and paint a friendly buff color onto party allies caught in the fan.
 */
public class SpectrumShift extends Skill {
    private static final Color[] RAINBOW = {
        Color.fromRGB(235, 64, 52), Color.fromRGB(245, 140, 40),
        Color.fromRGB(240, 214, 50), Color.fromRGB(64, 200, 84),
        Color.fromRGB(58, 150, 240), Color.fromRGB(170, 70, 220),
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "spectrumShift")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "spectrumShift", cooldown);
        cooldownAlarm(player, cooldown, "Spectrum Shift");

        double range = 8;
        double minDot = switch (level) {
            case 2, 3 -> 0.55;
            case 4, 5 -> 0.45;
            default -> 0.65;
        };
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);

        // fan of spatial-rainbow DUST sweeping the cone (single sweep, no strobe)
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        for (int i = 0; i < RAINBOW.length; i++) {
            double yawOff = (i - (RAINBOW.length - 1) / 2.0) * 14;
            Vector fan = ParticleUtils.rotateYAxis(dir.clone(), yawOff);
            for (Vector v : ParticleUtils.getLinePoints(fan.clone().normalize(), range, 0.6)) {
                Location p = eye.clone().add(v);
                p.getWorld().spawnParticle(Particle.DUST, p, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(RAINBOW[i], 1.1f));
            }
        }

        // collect cone targets, paint each a distinct color
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            Vector to = le.getLocation().toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            if (to.normalize().dot(dir) >= minDot) targets.add(le);
        }

        int idx = 0;
        for (LivingEntity le : targets) {
            int colorIdx = idx;
            if (fLevel < 4) {
                // without the L4 guarantee, base on existing color so distinctness isn't perfect
                int cur = Prism.getColor(le);
                if (cur >= 0) colorIdx = cur + 1 + idx;
            }
            Prism.paint(player, le, colorIdx);
            if (fLevel >= 3) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, fLevel >= 4 ? 40 : 30, 0));
            }
            Location swirl = le.getLocation().clone().add(0, le.getHeight() + 0.45, 0);
            le.getWorld().spawnParticle(Particle.DUST, swirl, 6, 0.25, 0.2, 0.25, 0,
                    new Particle.DustOptions(Prism.PALETTE[((colorIdx % Prism.PALETTE.length) + Prism.PALETTE.length) % Prism.PALETTE.length], 1.1f));
            idx++;
        }

        // L5: paint a friendly buff color onto party allies in the cone (a brief RESISTANCE)
        if (fLevel >= 5) {
            List<Player> party = Party.partyList(player);
            if (party != null) {
                for (Player ally : party) {
                    if (!ally.isOnline()) continue;
                    Vector to = ally.getLocation().toVector().subtract(eye.toVector());
                    double dist = to.length();
                    if (ally != player && (dist > range || dist < 0.01 || to.normalize().dot(dir) < minDot)) continue;
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0));
                    Location buff = ally.getLocation().clone().add(0, ally.getHeight() + 0.45, 0);
                    ally.getWorld().spawnParticle(Particle.DUST, buff, 8, 0.25, 0.25, 0.25, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.2f));
                }
            }
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Paint a cone of enemies each a different Color.";
            case 2 -> ChatColor.GRAY + "Wider cone.";
            case 3 -> ChatColor.GRAY + "Recolored enemies are briefly slowed.";
            case 4 -> ChatColor.GRAY + "Guarantees every target a distinct Color.";
            case 5 -> ChatColor.GRAY + "Also shields party allies caught in the fan.";
            default -> ChatColor.GRAY + "Paint a cone of enemies different Colors.";
        };
    }

    @Override
    public String toString() {
        return "spectrumShift";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PRISMARINE_CRYSTALS),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Spectrum Shift]",
                ChatColor.GRAY + "Sweep a fan of light across a cone, painting each",
                ChatColor.GRAY + "enemy a " + ChatColor.LIGHT_PURPLE + "different Color" + ChatColor.GRAY + " — maximize the contrast",
                ChatColor.GRAY + "before your payoff hit.");
    }
}
