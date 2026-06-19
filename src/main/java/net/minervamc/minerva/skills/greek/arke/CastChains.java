package net.minervamc.minerva.skills.greek.arke;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
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
import org.bukkit.util.Vector;

/**
 * Arke RLR (flex) — the setup. Cast a chain at the enemy you aim at, hooking in its 1-2 nearest
 * neighbours, binding them into a Shared Fate link. Desaturated-rainbow dust chains snap taut
 * between them. Levels widen the catch, lengthen the bind, and add a slow.
 */
public class CastChains extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 9500;
            case 4, 5 -> 9000;
            default -> 10000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "castChains")) {
            onCooldown(player);
            return;
        }

        LivingEntity anchor = Frostbite.frontTarget(player, 16);
        if (anchor == null) {
            player.sendActionBar(ChatColor.GRAY + "No target to chain!");
            return;
        }

        // how many extra neighbours to pull into the link (beyond the anchor)
        int extra = switch (level) {
            case 2, 3 -> 3;
            case 4, 5 -> 4;
            default -> 2;
        };

        List<LivingEntity> link = new ArrayList<>();
        link.add(anchor);
        for (Entity e : anchor.getNearbyEntities(7, 5, 7)) {
            if (link.size() >= 1 + extra) break;
            if (!(e instanceof LivingEntity le) || e == player || e == anchor) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            if (le.hasMetadata("NPC")) continue;
            link.add(le);
        }

        if (link.size() < 2) {
            player.sendActionBar(ChatColor.GRAY + "Need another enemy nearby to forge a chain!");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "castChains", cooldown);
        cooldownAlarm(player, cooldown, "Cast Chains");

        // forge the link via the engine
        SharedFate.link(player, link, level);

        // L4: links also slow 15% (extra short slow on top of the engine's bind slow)
        if (level >= 4) {
            for (LivingEntity le : link) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
            }
        }

        // telegraph: a thrown chain from the caster's hand to the anchor
        Location eye = player.getEyeLocation();
        World w = eye.getWorld();
        Vector start = eye.toVector();
        Vector end = anchor.getLocation().clone().add(0, anchor.getHeight() / 2, 0).toVector();
        List<Vector> throwPts = ParticleUtils.getLinePoints(start, end, 0.4);
        int tn = throwPts.size();
        for (int i = 0; i < tn; i++) {
            org.bukkit.Color c = SharedFate.PALETTE[(i * SharedFate.PALETTE.length) / Math.max(1, tn)];
            w.spawnParticle(Particle.DUST, throwPts.get(i).toLocation(w), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, 1f));
        }

        // impact: a single-shot taut-chain burst between every pair of consecutive members
        for (int i = 0; i < link.size() - 1; i++) {
            LivingEntity a = link.get(i);
            LivingEntity b = link.get(i + 1);
            if (a.getWorld() != b.getWorld()) continue;
            Vector av = a.getLocation().clone().add(0, a.getHeight() / 2, 0).toVector();
            Vector bv = b.getLocation().clone().add(0, b.getHeight() / 2, 0).toVector();
            List<Vector> pts = ParticleUtils.getLinePoints(av, bv, 0.4);
            int n = pts.size();
            for (int k = 0; k < n; k++) {
                org.bukkit.Color c = SharedFate.PALETTE[(k * SharedFate.PALETTE.length) / Math.max(1, n)];
                a.getWorld().spawnParticle(Particle.DUST, pts.get(k).toLocation(a.getWorld()), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(c, 1f));
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.2f);
        anchor.getWorld().playSound(anchor.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.9f, 1f);
        anchor.getWorld().playSound(anchor.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 0.6f, 1.3f);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Chain your target with its nearest enemies.";
            case 2 -> ChatColor.GRAY + "Hook in 1-2 more nearby enemies.";
            case 3 -> ChatColor.GRAY + "The chain holds longer.";
            case 4 -> ChatColor.GRAY + "Even wider catch; links also slow 15%.";
            case 5 -> ChatColor.GRAY + "Maximum link size and duration.";
            default -> ChatColor.GRAY + "Bind nearby enemies into a shared-fate chain.";
        };
    }

    @Override
    public String toString() {
        return "castChains";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_CHAIN),
                ChatColor.GRAY + "" + ChatColor.BOLD + "[Cast Chains]",
                ChatColor.GRAY + "Hurl a chain at your target, binding it and its",
                ChatColor.GRAY + "nearest foes into a " + ChatColor.WHITE + "Shared Fate" + ChatColor.GRAY + " link.",
                ChatColor.GRAY + "The link snaps if they spread too far.");
    }
}
