package net.minervamc.minerva.skills.greek.bellona;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.bellona.PlantWarBanner.WarBanner;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Bellona PASSIVE engine. Standing in your own banner crossfire amplifies your damage, and
 * fighting near a banner heals you. This is what makes the battery worth advancing — you want
 * enemies pinned between your emplacements. Wired into SkillListener via onOwnerDealDamage.
 */
public class WarFooting extends Skill {

    /**
     * Passive hook: called when the banner owner deals damage to a victim.
     * Crossfire (2+ banners near the victim) amplifies damage; fighting near a banner heals.
     */
    public static void onOwnerDealDamage(Player attacker, org.bukkit.entity.LivingEntity victim,
                                         org.bukkit.event.entity.EntityDamageByEntityEvent event, int level) {
        if (attacker == null || victim == null || event == null) return;

        // count attacker's banners within 8 blocks of the victim
        int near = 0;
        for (WarBanner b : PlantWarBanner.getBanners(attacker.getUniqueId())) {
            if (b.stand == null || !b.stand.isValid()) continue;
            if (b.stand.getWorld() != victim.getWorld()) continue;
            if (b.stand.getLocation().distanceSquared(victim.getLocation()) <= 64) near++;
        }

        // crossfire: 2 banners (or 1 at L4) amplifies damage
        int crossfireThreshold = level >= 4 ? 1 : 2;
        if (near >= crossfireThreshold) {
            double amp = level >= 2 ? 1.30 : 1.20;
            event.setDamage(event.getDamage() * amp);
            Location vc = victim.getLocation().clone().add(0, victim.getHeight() / 2, 0);
            World w = victim.getWorld();
            w.spawnParticle(Particle.DUST, vc, 8, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(PlantWarBanner.CRIMSON[2], 1.2f));
            w.playSound(vc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.1f);
        }

        // self-heal if the attacker is within 6 of any of their banners
        boolean nearOwn = false;
        for (WarBanner b : PlantWarBanner.getBanners(attacker.getUniqueId())) {
            if (b.stand == null || !b.stand.isValid()) continue;
            if (b.stand.getWorld() != attacker.getWorld()) continue;
            if (b.stand.getLocation().distanceSquared(attacker.getLocation()) <= 36) {
                nearOwn = true;
                break;
            }
        }
        if (nearOwn) {
            double heal = event.getFinalDamage() * 0.15;
            double max = attacker.getMaxHealth();
            attacker.setHealth(Math.min(max, attacker.getHealth() + heal));
            attacker.getWorld().spawnParticle(Particle.HEART,
                    attacker.getLocation().clone().add(0, 1.8, 0), 1, 0.2, 0.2, 0.2, 0);
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Crossfire (2+ banners) amps damage; banners heal you.";
            case 2 -> ChatColor.GRAY + "Crossfire amplification increased to 30%.";
            case 3 -> ChatColor.GRAY + "Crossfire amplification increased to 30%.";
            case 4 -> ChatColor.GRAY + "A single banner now counts as crossfire.";
            case 5 -> ChatColor.GRAY + "A single banner now counts as crossfire.";
            default -> ChatColor.GRAY + "Crossfire amps damage; banners heal you.";
        };
    }

    @Override
    public String toString() {
        return "warFooting";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_SWORD),
                ChatColor.RED + "" + ChatColor.BOLD + "[War Footing]",
                ChatColor.GRAY + "Enemies caught in your " + ChatColor.RED + "crossfire" + ChatColor.GRAY + " take",
                ChatColor.GRAY + "amplified damage. Fighting near a banner",
                ChatColor.GRAY + "heals you for part of the damage you deal.");
    }
}
