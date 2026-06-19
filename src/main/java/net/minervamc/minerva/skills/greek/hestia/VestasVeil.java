package net.minervamc.minerva.skills.greek.hestia;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hestia RRL — spend 4 Embers to raise a protective dome at the Hearth (or at the player if none).
 * Party allies inside gain Resistance for the duration; the dome shrugs off pressure. The defensive
 * spend that turns a banked lead into a teamfight wall.
 */
public class VestasVeil extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 15500;
            case 4, 5 -> 15000;
            default -> 16000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "vestasVeil")) {
            onCooldown(player);
            return;
        }

        // require & spend 4 embers BEFORE setting cooldown
        if (!BankedEmbers.spendEmbers(player, 4)) {
            skillLocked(player, "you need 4 banked Embers");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "vestasVeil", cooldown);
        cooldownAlarm(player, cooldown, "Vesta's Veil");

        Location center = TendTheHearth.getHearth(player);
        if (center == null || center.getWorld() == null || !center.getWorld().equals(player.getWorld())) {
            center = player.getLocation().clone();
        } else {
            center = center.clone();
        }

        World world = center.getWorld();
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.8f, 1f);
        world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.7f, 0.9f);

        final Location dome = center;
        final int fLevel = level;
        final int durTicks = level >= 4 ? 100 : 80; // ~4s base, ~5s at L4
        final double radius = level >= 4 ? 4.5 : 3.5;
        final int resistAmp = level >= 2 ? 1 : 0; // L2 stronger resist
        final boolean reflects = level >= 3;
        final boolean keepsShield = level >= 5;
        final Set<UUID> inside = new HashSet<>();

        if (reflects) {
            player.setMetadata("hestiaVeilReflect", new org.bukkit.metadata.FixedMetadataValue(Minerva.getInstance(), true));
        }

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= durTicks || world == null) {
                    // allies leaving keep a brief Absorption shield (L5)
                    if (keepsShield) {
                        for (Player ally : alliesIncludingCaster(player)) {
                            if (inside.contains(ally.getUniqueId()) && ally.isOnline() && !ally.isDead()) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 30, 0));
                            }
                        }
                    }
                    player.removeMetadata("hestiaVeilReflect", Minerva.getInstance());
                    world.playSound(dome, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1f);
                    cancel();
                    return;
                }

                // amber DUST sphere lattice that breathes (steady, no strobe)
                if (t % 3 == 0) {
                    double breathe = radius + 0.2 * Math.sin(t * 0.1);
                    List<Vector> lattice = ParticleUtils.getSpherePoints(breathe, 7);
                    int idx = 0;
                    for (Vector v : lattice) {
                        Color col = BankedEmbers.HEARTH[idx % BankedEmbers.HEARTH.length];
                        world.spawnParticle(Particle.DUST, dome.clone().add(v), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(col, 1f));
                        idx++;
                    }
                }

                // apply resistance to party allies (and caster) inside the dome
                if (t % 10 == 0) {
                    for (Player ally : alliesIncludingCaster(player)) {
                        if (!ally.isOnline() || ally.isDead()) continue;
                        if (ally.getWorld() != world) continue;
                        if (ally.getLocation().distanceSquared(dome) <= radius * radius) {
                            ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, resistAmp));
                            inside.add(ally.getUniqueId());
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private static List<Player> alliesIncludingCaster(Player player) {
        List<Player> allies = Party.partyList(player);
        if (allies == null) {
            allies = new java.util.ArrayList<>();
            allies.add(player);
        } else if (!allies.contains(player)) {
            allies = new java.util.ArrayList<>(allies);
            allies.add(player);
        }
        return allies;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Spend 4 Embers to raise a dome granting allies Resistance.";
            case 2 -> ChatColor.GRAY + "Stronger Resistance inside the dome.";
            case 3 -> ChatColor.GRAY + "While the dome holds, you reflect part of blocked damage.";
            case 4 -> ChatColor.GRAY + "Bigger, longer-lasting dome.";
            case 5 -> ChatColor.GRAY + "Allies leaving the dome keep a brief Absorption shield.";
            default -> ChatColor.GRAY + "Spend 4 Embers to raise a protective dome.";
        };
    }

    @Override
    public String toString() {
        return "vestasVeil";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.TORCH),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Vesta's Veil]",
                ChatColor.GRAY + "Spend " + ChatColor.GOLD + "4 Embers" + ChatColor.GRAY + " to raise a warding dome at",
                ChatColor.GRAY + "your " + ChatColor.GOLD + "Hearth" + ChatColor.GRAY + ". Allies inside gain " + ChatColor.GOLD + "Resistance",
                ChatColor.GRAY + "for the duration.");
    }
}
