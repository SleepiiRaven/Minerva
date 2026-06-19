package net.minervamc.minerva.skills.greek.hecate;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.hecate.TripleGoddess.Face;
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
 * Hecate RLL — a hex bolt that morphs by current Face. MAIDEN is a fast low-damage poke (and pierces
 * at higher ranks); MOTHER snares with Slowness + minor damage to set up allies; CRONE is a heavy
 * Wither curse + DoT. The trail and impact are recoloured to the active Face.
 */
public class SpectralHex extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 5500;
            case 4, 5 -> 5000;
            default -> 6000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "spectralHex")) {
            onCooldown(player);
            return;
        }

        Face face = TripleGoddess.getFace(player);
        double range = 25;
        LivingEntity target = Frostbite.frontTarget(player, range);
        if (target == null) {
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "No target in sight!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "spectralHex", cooldown);
        cooldownAlarm(player, cooldown, "Spectral Hex");

        final int fLevel = level;
        Color trail = TripleGoddess.faceColor(face);

        // Face-coloured DUST bolt trail from eye to target
        Location eye = player.getEyeLocation();
        Location hit = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
        for (Vector v : ParticleUtils.getLinePoints(eye.toVector(), hit.toVector(), 0.4)) {
            eye.getWorld().spawnParticle(Particle.DUST, v.toLocation(eye.getWorld()), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(trail, 1.0f));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);

        switch (face) {
            case MAIDEN -> {
                double dmg = switch (fLevel) {
                    case 2 -> 6;
                    case 3 -> 6;
                    case 4 -> 7;
                    case 5 -> 8;
                    default -> 5;
                };
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.6f);
                damage(target, dmg, player);
                faceImpact(target, face);
                // L3+: pierce — also hit one enemy just behind the target
                if (fLevel >= 3) {
                    LivingEntity behind = pierceBehind(player, target, 3);
                    if (behind != null) {
                        damage(behind, dmg * 0.7, player);
                        faceImpact(behind, face);
                    }
                }
                maybeTriggerTorch(player, target, fLevel);
            }
            case MOTHER -> {
                double dmg = fLevel >= 4 ? 4 : 3;
                int snareTicks = switch (fLevel) {
                    case 3 -> 50;
                    case 4 -> 60;
                    case 5 -> 70;
                    default -> 40;
                };
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
                damage(target, dmg, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, snareTicks, fLevel >= 4 ? 2 : 1));
                faceImpact(target, face);
                maybeTriggerTorch(player, target, fLevel);
            }
            case CRONE -> {
                double dmg = switch (fLevel) {
                    case 2 -> 11;
                    case 3 -> 12;
                    case 4 -> 13;
                    case 5 -> 15;
                    default -> 10;
                };
                int witherTicks = switch (fLevel) {
                    case 3 -> 80;
                    case 4 -> 100;
                    case 5 -> 120;
                    default -> 60;
                };
                int witherAmp = fLevel >= 4 ? 1 : 0;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 0.8f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.4f);
                damage(target, dmg, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, witherAmp));
                faceImpact(target, face);
                maybeTriggerTorch(player, target, fLevel);
            }
        }
    }

    /** L5: if the struck enemy is near one of your torches, fire that torch's effect on the spot. */
    private static void maybeTriggerTorch(Player player, LivingEntity target, int level) {
        if (level < 5) return;
        CrossroadsTorches.triggerNearest(player, target.getLocation(), 4.5);
    }

    private static LivingEntity pierceBehind(Player player, LivingEntity primary, double extra) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        LivingEntity best = null;
        double bestDot = 0.7;
        for (Entity e : primary.getNearbyEntities(extra, extra, extra)) {
            if (!(e instanceof LivingEntity le) || e == player || e == primary) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            Vector to = le.getLocation().toVector().subtract(primary.getLocation().toVector());
            if (to.lengthSquared() < 0.01) continue;
            double dot = to.normalize().dot(dir);
            if (dot > bestDot) {
                bestDot = dot;
                best = le;
            }
        }
        return best;
    }

    private static void faceImpact(LivingEntity target, Face face) {
        Location c = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
        switch (face) {
            case MAIDEN -> {
                c.getWorld().spawnParticle(Particle.ENCHANTED_HIT, c, 12, 0.3, 0.3, 0.3, 0.1);
                for (Vector v : ParticleUtils.getSpherePoints(0.6, 5)) {
                    c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TripleGoddess.MAIDEN_SILVER, 1.0f));
                }
                c.getWorld().playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 1.6f);
            }
            case MOTHER -> {
                c.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, c, 14, 0.4, 0.4, 0.4, 0.02);
                for (Vector v : ParticleUtils.getSpherePoints(0.6, 5)) {
                    c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TripleGoddess.MOTHER_GREEN, 1.0f));
                }
                c.getWorld().playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.0f);
            }
            case CRONE -> {
                c.getWorld().spawnParticle(Particle.WITCH, c, 16, 0.4, 0.4, 0.4, 0.05);
                for (Vector v : ParticleUtils.getSpherePoints(0.6, 5)) {
                    c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TripleGoddess.CRONE_VIOLET, 1.1f));
                }
                c.getWorld().playSound(c, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.8f);
            }
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "A hex bolt that morphs by Face (poke / snare / curse).";
            case 2 -> ChatColor.GRAY + "More damage and effect on every Face.";
            case 3 -> ChatColor.GRAY + "Maiden pierces, Mother snares longer, Crone curses longer.";
            case 4 -> ChatColor.GRAY + "Stronger effects and lower cooldown.";
            case 5 -> ChatColor.GRAY + "Hitting an enemy near your torch triggers that torch on them.";
            default -> ChatColor.GRAY + "A Face-morphing hex bolt.";
        };
    }

    @Override
    public String toString() {
        return "spectralHex";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PURPLE_DYE),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Spectral Hex]",
                ChatColor.GRAY + "Fire a hex bolt shaped by your " + ChatColor.LIGHT_PURPLE + "Face" + ChatColor.GRAY + ":",
                ChatColor.WHITE + "Maiden" + ChatColor.GRAY + " pokes, " + ChatColor.GREEN + "Mother" + ChatColor.GRAY + " snares,",
                ChatColor.DARK_PURPLE + "Crone" + ChatColor.GRAY + " lays a heavy Wither curse.");
    }
}
