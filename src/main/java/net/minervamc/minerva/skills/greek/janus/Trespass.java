package net.minervamc.minerva.skills.greek.janus;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.janus.Doorway.Portal;
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
 * Janus RRL — clean displacement. Find the enemy nearest your Exit portal (L4: either portal)
 * and swap places: you take their spot, they're thrown to yours, through the door. indigo->gold
 * swirls erupt at both endpoints. Without a portal or a target, nothing happens (no cooldown).
 */
public class Trespass extends Skill {
    public static final Color GOLD = Color.fromRGB(255, 206, 90);
    public static final Color INDIGO = Color.fromRGB(59, 46, 140);

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 14500;
            case 4, 5 -> 14000;
            default -> 15000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "trespass")) {
            onCooldown(player);
            return;
        }

        Portal portal = Doorway.portalOf(player);
        if (portal == null || portal.exit == null) {
            player.sendActionBar(ChatColor.GOLD + "No Exit portal to trespass through!");
            return;
        }

        LivingEntity target = nearestEnemyTo(player, portal.exit, 6);
        if (target == null && level >= 4 && portal.entry != null) {
            target = nearestEnemyTo(player, portal.entry, 6);
        }
        if (target == null) {
            player.sendActionBar(ChatColor.GOLD + "No enemy near your portal!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "trespass", cooldown);
        cooldownAlarm(player, cooldown, "Trespass");

        Location myOld = player.getLocation().clone();
        Location theirOld = target.getLocation().clone();

        // swirls at both endpoints before the swap resolves
        swirl(myOld);
        swirl(theirOld);
        myOld.getWorld().spawnParticle(Particle.REVERSE_PORTAL, myOld.clone().add(0, 1, 0), 14, 0.3, 0.5, 0.3, 0.05);
        theirOld.getWorld().spawnParticle(Particle.REVERSE_PORTAL, theirOld.clone().add(0, 1, 0), 14, 0.3, 0.5, 0.3, 0.05);

        Location toTarget = theirOld.clone();
        toTarget.setDirection(player.getLocation().getDirection());
        Location toMe = myOld.clone();
        toMe.setDirection(target.getLocation().getDirection());

        player.teleport(toTarget);
        target.teleport(toMe);

        player.getWorld().playSound(toTarget, Sound.BLOCK_PORTAL_TRAVEL, 1f, 1.1f);
        target.getWorld().playSound(toMe, Sound.BLOCK_PORTAL_TRAVEL, 1f, 0.9f);
        player.getWorld().playSound(toTarget, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.2f);

        if (level >= 2) {
            stun(player, target, level >= 4 ? 24L : 18L);
        }
        if (level >= 3) {
            GodOfTransitions.applyEndings(player, target, level);
        }
        if (level >= 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 30, 1));
            player.getWorld().spawnParticle(Particle.WAX_ON, player.getLocation().clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private static LivingEntity nearestEnemyTo(Player player, Location anchor, double range) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : anchor.getWorld().getNearbyEntities(anchor, range, range, range)) {
            if (!(e instanceof LivingEntity le) || e == player) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (le.hasMetadata("NPC")) continue;
            double d = le.getLocation().distanceSquared(anchor);
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    /** A spatial indigo->gold swirl ring (steady gradient by height — no strobe). */
    private static void swirl(Location at) {
        Location base = at.clone().add(0, 0.3, 0);
        int rings = 5;
        for (int r = 0; r < rings; r++) {
            double radius = 0.4 + r * 0.18;
            Color color = blend(INDIGO, GOLD, (double) r / (rings - 1));
            for (Vector v : ParticleUtils.getCirclePoints(radius, 10)) {
                base.getWorld().spawnParticle(Particle.DUST, base.clone().add(v).add(0, r * 0.25, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(color, 1.0f));
            }
        }
    }

    private static Color blend(Color a, Color b, double t) {
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return Color.fromRGB(clamp(r), clamp(g), clamp(bl));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Swap places with the enemy nearest your Exit portal.";
            case 2 -> ChatColor.GRAY + "The swapped enemy is briefly stunned.";
            case 3 -> ChatColor.GRAY + "The swap applies Endings.";
            case 4 -> ChatColor.GRAY + "Targets the nearest enemy to either portal.";
            case 5 -> ChatColor.GRAY + "You gain a brief shield after swapping.";
            default -> ChatColor.GRAY + "Swap places with an enemy at your portal.";
        };
    }

    @Override
    public String toString() {
        return "trespass";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ENDER_PEARL),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Trespass]",
                ChatColor.GRAY + "Swap places with the enemy at your " + ChatColor.DARK_PURPLE + "Exit",
                ChatColor.GRAY + "portal — you take their spot, they take yours.",
                ChatColor.GRAY + "Peel a diver, or trade places to escape.");
    }
}
