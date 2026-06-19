package net.minervamc.minerva.skills.greek.nemesis;

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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Nemesis RRR — a NON-movement guard stance. For a brief window she plants her feet, takes
 * reduced direct damage (RESISTANCE), and tags herself 'nemesisBracing' so the Ledger banks
 * DOUBLE the absorbed damage. A steady dark-gold shield-lattice forms in front of her. A timing
 * decision: turn a predictable big hit into a fat deposit. Higher ranks reflect a slice of
 * blocked damage, shield party allies behind her, and grant CC-immunity for the window.
 */
public class Brace extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "brace")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "brace", cooldown);
        cooldownAlarm(player, cooldown, "Brace");

        final int braceTicks = level >= 2 ? 40 : 30;
        final int fLevel = level;
        final int resAmp = level >= 4 ? 1 : 0; // 40%/60% reduced direct damage

        player.addScoreboardTag("nemesisBracing");
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, braceTicks, resAmp));

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);

        new BukkitRunnable() {
            int t = 0;
            double lastHealth = player.getHealth();

            @Override
            public void run() {
                if (t >= braceTicks || player.isDead() || !player.isOnline()) {
                    player.removeScoreboardTag("nemesisBracing");
                    cancel();
                    return;
                }

                // L5: CC-immunity — clear slowness and any stun-style tag each tick
                if (fLevel >= 5) {
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.removeScoreboardTag("stunned");
                }

                // L3: reflect a slice of damage absorbed this tick to nearby attackers
                if (fLevel >= 3) {
                    double now = player.getHealth();
                    double lost = lastHealth - now;
                    if (lost > 0.1) {
                        double reflect = lost * 0.4;
                        for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                            if (!(e instanceof LivingEntity le) || e == player) continue;
                            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                            if (PlayerStats.isSummoned(player, le)) continue;
                            damage(le, reflect, player);
                            break; // reflect to the nearest aggressor only
                        }
                    }
                    lastHealth = player.getHealth();
                } else {
                    lastHealth = player.getHealth();
                }

                // L4: extend the guard to party allies directly behind the caster
                if (fLevel >= 4 && t % 5 == 0) {
                    Vector back = player.getLocation().getDirection().setY(0).normalize().multiply(-1);
                    List<Player> party = Party.partyList(player);
                    if (party != null) {
                        for (Player ally : party) {
                            if (ally == null || ally == player || !ally.isOnline()) continue;
                            if (ally.getWorld() != player.getWorld()) continue;
                            Vector to = ally.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
                            if (to.lengthSquared() < 0.01 || to.lengthSquared() > 16) continue;
                            if (to.normalize().dot(back) > 0.4) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 12, 0));
                            }
                        }
                    }
                }

                drawLattice(player, fLevel);
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** Steady dark-gold shield lattice arcing in front of the caster (no strobe). */
    private static void drawLattice(Player player, int level) {
        Color[] palette = LedgerOfWrongs.BLACKGOLD;
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        float yaw = player.getLocation().getYaw();
        Location front = player.getLocation().clone().add(0, 1.0, 0).add(dir.clone().multiply(1.1));
        // a vertical arc of lattice points facing the aim direction
        int idx = 0;
        for (Vector v : ParticleUtils.getVerticalCirclePoints(0.9, 0f, yaw + 90f, 14)) {
            Color col = palette[idx % palette.length]; // spatially fixed gradient, not per-frame random
            front.getWorld().spawnParticle(Particle.DUST, front.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(col, 1.1f));
            idx++;
        }
        // crossbars to read as a lattice
        for (double h = -0.6; h <= 0.6; h += 0.4) {
            Vector across = ParticleUtils.rotateYAxis(new Vector(0, 0, 0.6), yaw + 90f);
            for (double s = -1; s <= 1; s += 0.5) {
                Location p = front.clone().add(0, h, 0).add(across.clone().multiply(s));
                front.getWorld().spawnParticle(Particle.DUST, p, 0, 0, 0, 0, 0,
                        new Particle.DustOptions(palette[1], 0.9f));
            }
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Brace: reduce direct damage and bank DOUBLE the hits.";
            case 2 -> ChatColor.GRAY + "Longer brace window.";
            case 3 -> ChatColor.GRAY + "Reflect a slice of blocked damage.";
            case 4 -> ChatColor.GRAY + "Stronger guard; shields allies behind you.";
            case 5 -> ChatColor.GRAY + "Immune to crowd control while bracing.";
            default -> ChatColor.GRAY + "Brace and bank double the damage you absorb.";
        };
    }

    @Override
    public String toString() {
        return "brace";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SHIELD),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Brace]",
                ChatColor.GRAY + "Plant your feet. Take reduced direct damage, but",
                ChatColor.GRAY + "bank " + ChatColor.GOLD + "DOUBLE" + ChatColor.GRAY + " the absorbed damage into the Ledger.",
                ChatColor.GRAY + "Turn a big incoming hit into a fat deposit.");
    }
}
