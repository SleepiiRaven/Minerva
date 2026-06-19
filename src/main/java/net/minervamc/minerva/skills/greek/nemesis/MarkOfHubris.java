package net.minervamc.minerva.skills.greek.nemesis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
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
 * Nemesis RRL — brand an aggressor with Hubris. While marked, a fraction of the damage the
 * MARKED enemy deals to anyone copies into the brander's Ledger: pick the enemy carry and let
 * their aggression arm your retribution. A steady dark-gold brand glyph rotates over the target.
 * Higher ranks feed a larger fraction, make the marked target take extra damage, last longer,
 * and bank a lump on the marked target's death.
 */
public class MarkOfHubris extends Skill {
    /** Stored marks: marked-enemy UUID -> the brand details. */
    public static final Map<UUID, Mark> MARKS = new HashMap<>();

    /** Inner record of a single brand. */
    public static class Mark {
        public final UUID nemesis;
        public final int level;
        public final long expire;

        public Mark(UUID nemesis, int level, long expire) {
            this.nemesis = nemesis;
            this.level = level;
            this.expire = expire;
        }
    }

    // ===================================================================================
    // REQUIRED HOOK (verbatim signature — external code calls this on every damage event)
    // ===================================================================================

    /** If the damager is a marked enemy and not expired, bank a fraction of dmg into the nemesis' Ledger. */
    public static void feedIfMarked(org.bukkit.entity.Entity damager, double dmg) {
        if (damager == null || dmg <= 0) return;
        Mark mark = MARKS.get(damager.getUniqueId());
        if (mark == null) return;
        if (System.currentTimeMillis() > mark.expire) {
            MARKS.remove(damager.getUniqueId());
            return;
        }
        Player nemesis = Bukkit.getPlayer(mark.nemesis);
        if (nemesis == null || !nemesis.isOnline()) {
            MARKS.remove(damager.getUniqueId());
            return;
        }
        double feedPct = 0.3 + 0.05 * mark.level; // 30% base scaling toward 45% by L5 territory
        LedgerOfWrongs.addToLedger(nemesis, dmg * feedPct);

        // thin gold thread pulses to the nemesis (single shot per feed)
        Location from = damager.getLocation().clone().add(0, 1, 0);
        Location to = nemesis.getLocation().clone().add(0, 1, 0);
        for (Vector v : ParticleUtils.getLinePoints(from.toVector(), to.toVector(), 0.6)) {
            from.getWorld().spawnParticle(Particle.DUST, v.toLocation(from.getWorld()), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[3], 0.7f));
        }
        nemesis.getWorld().playSound(nemesis.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.4f);
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 11500;
            case 4, 5 -> 11000;
            default -> 12000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "markOfHubris")) {
            onCooldown(player);
            return;
        }

        LivingEntity target = Frostbite.frontTarget(player, 20);
        if (target == null) {
            player.sendActionBar(ChatColor.GOLD + "No target to brand!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "markOfHubris", cooldown);
        cooldownAlarm(player, cooldown, "Mark of Hubris");

        long durationMs = switch (level) {
            case 2, 3 -> 7000L;
            case 4, 5 -> 8000L;
            default -> 6000L;
        };
        final UUID targetId = target.getUniqueId();
        MARKS.put(targetId, new Mark(player.getUniqueId(), level, System.currentTimeMillis() + durationMs));

        // L3+: the marked target takes +5% damage (apply a light vulnerability via weakness-removal? use GLOWING + tag)
        if (level >= 3) {
            target.addScoreboardTag("nemesisMarked");
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 0.7f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 0.6f);

        final int fLevel = level;
        final int durationTicks = (int) (durationMs / 50);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                Mark m = MARKS.get(targetId);
                if (m == null || t >= durationTicks || target.isDead() || System.currentTimeMillis() > m.expire) {
                    // L5: bank a lump if the marked target died while marked
                    if (fLevel >= 5 && target.isDead() && MARKS.containsKey(targetId)) {
                        LedgerOfWrongs.addToLedger(player, 12);
                        if (player.isOnline()) {
                            player.sendActionBar(ChatColor.GOLD + "Hubris collected on death!");
                        }
                    }
                    MARKS.remove(targetId);
                    target.removeScoreboardTag("nemesisMarked");
                    cancel();
                    return;
                }

                // rotating dark-gold brand glyph above the target (steady, spatially fixed gradient)
                Location glyph = target.getLocation().clone().add(0, target.getHeight() + 0.6, 0);
                double baseAngle = (t % 40) / 40.0 * Math.PI * 2;
                int idx = 0;
                for (Vector base : ParticleUtils.getStarPoints(4, 0.45, 0.45, 1)) {
                    Vector v = ParticleUtils.rotateYAxis(base.clone(), Math.toDegrees(baseAngle));
                    glyph.getWorld().spawnParticle(Particle.DUST, glyph.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(LedgerOfWrongs.BLACKGOLD[2 + (idx % 2)], 0.9f));
                    idx++;
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Mark a foe: 30% of damage they deal banks for you.";
            case 2 -> ChatColor.GRAY + "Longer mark duration.";
            case 3 -> ChatColor.GRAY + "Feeds more; marked foe is exposed.";
            case 4 -> ChatColor.GRAY + "Even longer mark.";
            case 5 -> ChatColor.GRAY + "Banks a lump if the marked foe dies.";
            default -> ChatColor.GRAY + "Mark a foe so their damage feeds your Ledger.";
        };
    }

    @Override
    public String toString() {
        return "markOfHubris";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.GOLD_NUGGET),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Mark of Hubris]",
                ChatColor.GRAY + "Brand an enemy. While marked, a share of the",
                ChatColor.GRAY + "damage " + ChatColor.GOLD + "they" + ChatColor.GRAY + " deal copies into your Ledger.",
                ChatColor.GRAY + "Mark the carry; let their aggression arm you.");
    }
}
