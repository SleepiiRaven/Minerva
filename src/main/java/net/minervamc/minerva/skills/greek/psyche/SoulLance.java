package net.minervamc.minerva.skills.greek.psyche;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Psyche RLL (damage) — a piercing soul-bolt fired along the eye direction. It strikes every enemy
 * in a thin line, scaling with {@link IridescentSoul#bonusMultiplier} (wounded Psyche hits harder).
 * The FIRST enemy hit becomes Anima-marked: ReleaseAnima will recall onto it. No strobe — a single
 * comet head travels the path with a steady fixed-gradient trail.
 */
public class SoulLance extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 6500;
            case 4, 5 -> 6000;
            default -> 7000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "soulLance")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "soulLance", cooldown);
        cooldownAlarm(player, cooldown, "Soul Lance");

        final int fLevel = level;
        double range = switch (level) {
            case 3 -> 22;
            case 4, 5 -> 24;
            default -> 18;
        };
        double baseDmg = 8 * IridescentSoul.bonusMultiplier(player, level);
        final double dmg = baseDmg;
        final double markedBonus = level >= 2 ? baseDmg * 0.5 : 0;

        World w = player.getWorld();
        final Location start = player.getEyeLocation().clone();
        final Vector dir = start.getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.4f);

        final Set<UUID> alreadyHit = new HashSet<>();
        final boolean[] markedSet = {false};

        new BukkitRunnable() {
            double traveled = 0;
            final Location head = start.clone();
            @Override
            public void run() {
                if (traveled >= range || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                for (int step = 0; step < 2 && traveled < range; step++) {
                    head.add(dir.clone().multiply(0.6));
                    traveled += 0.6;
                    if (head.getBlock().getType().isSolid()) {
                        cancel();
                        return;
                    }
                    // comet head + soul motes (placed on the path, not spammed)
                    w.spawnParticle(Particle.DUST, head, 0, 0, 0, 0, 0,
                            new Particle.DustOptions(IridescentSoul.IRIDESCENT[0], 1.1f));
                    w.spawnParticle(Particle.DUST, head, 0, 0, 0, 0, 0,
                            new Particle.DustOptions(IridescentSoul.IRIDESCENT[(int) (traveled) % IridescentSoul.IRIDESCENT.length], 0.6f));

                    for (Entity e : w.getNearbyEntities(head, 1.0, 1.0, 1.0)) {
                        if (!(e instanceof LivingEntity le) || e == player) continue;
                        if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                        if (PlayerStats.isSummoned(player, le)) continue;
                        if (le.hasMetadata("NPC")) continue;
                        if (!alreadyHit.add(le.getUniqueId())) continue;

                        double total = dmg;
                        if (!markedSet[0]) {
                            // first enemy hit becomes the Anima mark
                            markedSet[0] = true;
                            ReleaseAnima.setRecallTarget(player, le);
                            total += markedBonus;
                            le.getWorld().spawnParticle(Particle.DUST, le.getLocation().clone().add(0, le.getHeight() / 2, 0), 18, 0.3, 0.4, 0.3, 0,
                                    new Particle.DustOptions(IridescentSoul.IRIDESCENT[3], 1.2f));
                            le.getWorld().playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.3f);
                        }
                        damage(le, total, player);
                        le.getWorld().spawnParticle(Particle.DUST, le.getLocation().clone().add(0, le.getHeight() / 2, 0), 8, 0.25, 0.3, 0.25, 0,
                                new Particle.DustOptions(IridescentSoul.IRIDESCENT[1], 0.9f));
                    }
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // L4: recall-to-mark also tugs threaded enemies (recall handles the tug; here we ensure
        // the mark is fresh so the recall will home onto it).  L5 death-wisp handled below.
        if (level >= 5) {
            // watch the marked target: if it dies soon, drop a healing soul-wisp for the caster
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    LivingEntity mark = ReleaseAnima.getRecallTarget(player);
                    if (mark != null && mark.isDead()) {
                        Location wisp = mark.getLocation().clone().add(0, 0.5, 0);
                        spawnHealingWisp(player, wisp, fLevel);
                        cancel();
                        return;
                    }
                    if (t >= 120 || player.isDead() || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    t += 5;
                }
            }.runTaskTimer(Minerva.getInstance(), 5L, 5L);
        }
    }

    private static void spawnHealingWisp(Player owner, Location loc, int level) {
        World w = loc.getWorld();
        w.spawnParticle(Particle.DUST, loc, 16, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(IridescentSoul.IRIDESCENT[2], 1f));
        w.playSound(loc, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.5f);
        // a slow wisp that heals the caster on pickup window
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 100 || owner.isDead() || !owner.isOnline()) {
                    cancel();
                    return;
                }
                w.spawnParticle(Particle.DUST, loc.clone().add(0, 0.4 + 0.1 * Math.sin(t / 6.0), 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(IridescentSoul.IRIDESCENT[2], 0.8f));
                if (owner.getLocation().distanceSquared(loc) <= 4) {
                    double heal = Math.min(owner.getMaxHealth(), owner.getHealth() + 6);
                    owner.setHealth(heal);
                    w.spawnParticle(Particle.HEART, owner.getLocation().clone().add(0, 1.6, 0), 3, 0.3, 0.3, 0.3, 0);
                    w.playSound(owner.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.6f);
                    cancel();
                    return;
                }
                t += 2;
            }
        }.runTaskTimer(Minerva.getInstance(), 2L, 2L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Fire a piercing soul-bolt; first enemy hit is Anima-marked.";
            case 2 -> ChatColor.GRAY + "Bonus damage to the marked enemy.";
            case 3 -> ChatColor.GRAY + "The bolt pierces further.";
            case 4 -> ChatColor.GRAY + "Recalling to the mark also reels in your threads.";
            case 5 -> ChatColor.GRAY + "If the mark dies, a healing soul-wisp is left for you.";
            default -> ChatColor.GRAY + "Fire a piercing soul-bolt that marks your prey.";
        };
    }

    @Override
    public String toString() {
        return "soulLance";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SPECTRAL_ARROW),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Soul Lance]",
                ChatColor.GRAY + "Loose a piercing bolt of soul-light. It hits all in a",
                ChatColor.GRAY + "line and " + ChatColor.LIGHT_PURPLE + "marks" + ChatColor.GRAY + " the first foe — recall onto it.",
                ChatColor.GRAY + "Hits harder the lower your health.");
    }
}
