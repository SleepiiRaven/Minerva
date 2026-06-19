package net.minervamc.minerva.skills.greek.bellona;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.bellona.PlantWarBanner.WarBanner;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Bellona RRR — dash forward and bring the battery with you. On landing, relocate your nearest
 * banner(s) to your new position and refresh their lifetime, then pulse a knockback ring. This
 * is the "advance" half of the kit: never stand still, always push the line forward.
 */
public class AdvanceTheLine extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 7500;
            case 4, 5 -> 7000;
            default -> 8000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "advanceTheLine")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "advanceTheLine", cooldown);
        cooldownAlarm(player, cooldown, "Advance The Line");

        final int fLevel = level;
        double power = level >= 4 ? 1.5 : 1.2;
        Vector look = player.getLocation().getDirection().setY(0).normalize();
        player.setVelocity(look.multiply(power).setY(0.3));

        World w = player.getWorld();
        w.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.3f);
        w.playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 1f, 0.9f);
        w.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 0.6f, 1.4f);

        // resolve the landing a few ticks later, then relocate banners + knockback ring
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                // dash trail
                Location feet = player.getLocation();
                feet.getWorld().spawnParticle(Particle.DUST, feet.clone().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0,
                        new Particle.DustOptions(PlantWarBanner.CRIMSON[1], 1f));

                if (t >= 6 || player.isOnGround() && t >= 2) {
                    Location landing = player.getLocation();

                    if (fLevel >= 2) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
                    }

                    // relocate nearest banner(s)
                    int count = fLevel >= 3 ? 2 : 1;
                    List<WarBanner> mine = new ArrayList<>(PlantWarBanner.getBanners(player.getUniqueId()));
                    mine.sort((a, b) -> Double.compare(
                            a.stand.getLocation().distanceSquared(landing),
                            b.stand.getLocation().distanceSquared(landing)));
                    int moved = 0;
                    for (WarBanner b : mine) {
                        if (moved >= count) break;
                        // fan multiple relocated banners slightly apart
                        Vector off = look.clone().multiply(moved == 0 ? 0 : 1.4)
                                .add(new Vector(moved == 0 ? 0 : -1, 0, 0).multiply(0.5));
                        PlantWarBanner.relocate(b, landing.clone().add(off));
                        if (fLevel >= 5) {
                            // relocated banner fires an immediate pulse
                            fireImmediatePulse(player, b);
                        }
                        moved++;
                    }

                    // landing knockback ring
                    Location c = landing.clone();
                    for (Vector v : ParticleUtils.getCirclePoints(2.2, 22)) {
                        c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v).add(0, 0.15, 0), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(PlantWarBanner.CRIMSON[2], 1.1f));
                    }
                    c.getWorld().playSound(c, Sound.BLOCK_ANVIL_LAND, 0.7f, 1.2f);
                    for (Entity e : player.getNearbyEntities(2.5, 2.5, 2.5)) {
                        if (!(e instanceof LivingEntity le) || e == player) continue;
                        if (le.getScoreboardTags().contains("bellonaBanner")) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, le)) continue;
                        Vector kb = le.getLocation().toVector().subtract(c.toVector()).setY(0);
                        if (kb.lengthSquared() < 0.04) kb = look.clone();
                        kb.normalize().multiply(0.6).setY(0.35);
                        knockback(le, kb);
                    }
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 2L, 1L);
    }

    private static void fireImmediatePulse(Player player, WarBanner b) {
        if (b.stand == null || !b.stand.isValid()) return;
        Location top = b.bannerTop();
        World w = top.getWorld();
        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        for (Entity e : b.stand.getNearbyEntities(6, 4, 6)) {
            if (!(e instanceof LivingEntity le) || le == player) continue;
            if (le.getScoreboardTags().contains("bellonaBanner")) continue;
            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
            if (PlayerStats.isSummoned(player, le)) continue;
            double d = le.getLocation().distanceSquared(b.stand.getLocation());
            if (d < best) {
                best = d;
                target = le;
            }
        }
        if (target == null) return;
        Location tc = target.getLocation().clone().add(0, target.getHeight() / 2, 0);
        for (Vector v : ParticleUtils.getLinePoints(top.toVector(), tc.toVector(), 0.5)) {
            w.spawnParticle(Particle.DUST, v.toLocation(w), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(PlantWarBanner.CRIMSON[2], 0.9f));
        }
        w.spawnParticle(Particle.CRIT, tc, 6, 0.2, 0.2, 0.2, 0.05);
        damage(target, 4, player);
        w.playSound(tc, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.2f);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Dash forward and relocate your nearest banner.";
            case 2 -> ChatColor.GRAY + "Gain brief Resistance on arrival.";
            case 3 -> ChatColor.GRAY + "Relocate the 2 nearest banners.";
            case 4 -> ChatColor.GRAY + "Longer dash.";
            case 5 -> ChatColor.GRAY + "The relocated banner fires an immediate pulse.";
            default -> ChatColor.GRAY + "Dash forward and bring your banner.";
        };
    }

    @Override
    public String toString() {
        return "advanceTheLine";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.RED_BANNER),
                ChatColor.RED + "" + ChatColor.BOLD + "[Advance The Line]",
                ChatColor.GRAY + "Dash forward and relocate your nearest",
                ChatColor.RED + "banner" + ChatColor.GRAY + " to your landing, pulsing a",
                ChatColor.GRAY + "knockback ring. Push the line forward.");
    }
}
