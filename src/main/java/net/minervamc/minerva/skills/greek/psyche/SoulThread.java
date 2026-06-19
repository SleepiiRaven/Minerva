package net.minervamc.minerva.skills.greek.psyche;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Psyche RLR (flex) — dash forward, threading every enemy passed. Threaded enemies stay tethered
 * by pastel soul-lines for a window; calling {@link #tugThreaded} (e.g. when Psyche recalls) yanks
 * them all toward a point. No strobe: the thread lines are a steady fixed-gradient DUST runnable.
 */
public class SoulThread extends Skill {
    /** owner UUID -> set of currently-threaded enemy UUIDs. */
    private static final Map<UUID, Set<UUID>> THREADS = new HashMap<>();

    public static Set<UUID> threadsOf(Player owner) {
        return THREADS.getOrDefault(owner.getUniqueId(), new HashSet<>());
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 8500;
            case 4, 5 -> 8000;
            default -> 9000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "soulThread")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "soulThread", cooldown);
        cooldownAlarm(player, cooldown, "Soul Thread");

        final int fLevel = level;
        final Set<UUID> threaded = new HashSet<>();
        THREADS.put(player.getUniqueId(), threaded);

        int threadTicks = switch (level) {
            case 2 -> 120;
            case 3 -> 130;
            case 4, 5 -> 140;
            default -> 100;
        };

        // dash
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Vector dash = dir.clone().multiply(1.15);
        dash.setY(Math.max(0.32, dash.getY()));
        player.setVelocity(dash);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.8f, 1.3f);

        // collect threaded enemies along the dash (a short runnable)
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 14 || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location feet = player.getLocation();
                feet.getWorld().spawnParticle(Particle.DUST, feet.clone().add(0, 1, 0), 3, 0.3, 0.4, 0.3, 0,
                        new Particle.DustOptions(IridescentSoul.IRIDESCENT[t % IridescentSoul.IRIDESCENT.length], 0.9f));
                for (Entity e : player.getNearbyEntities(1.6, 1.6, 1.6)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    if (le.hasMetadata("NPC")) continue;
                    if (threaded.add(le.getUniqueId()) && fLevel >= 3) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, threadTicks, fLevel >= 4 ? 1 : 0));
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        // steady thread-line visuals for the thread window
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= threadTicks || player.isDead() || !player.isOnline() || threaded.isEmpty()) {
                    if (THREADS.get(player.getUniqueId()) == threaded) {
                        THREADS.remove(player.getUniqueId());
                    }
                    cancel();
                    return;
                }
                if (t % 4 == 0) {
                    World w = player.getWorld();
                    Location from = player.getLocation().clone().add(0, 1, 0);
                    int colorIdx = 0;
                    for (UUID id : new HashSet<>(threaded)) {
                        Entity e = Minerva.getInstance().getServer().getEntity(id);
                        if (!(e instanceof LivingEntity le) || le.isDead() || !le.getWorld().equals(w)) {
                            threaded.remove(id);
                            continue;
                        }
                        Location to = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
                        if (from.distanceSquared(to) > 900) continue;
                        for (Vector v : ParticleUtils.getLinePoints(from.toVector(), to.toVector(), 0.6)) {
                            w.spawnParticle(Particle.DUST, v.toLocation(w), 0, 0, 0, 0, 0,
                                    new Particle.DustOptions(IridescentSoul.IRIDESCENT[colorIdx % IridescentSoul.IRIDESCENT.length], 0.5f));
                        }
                        colorIdx++;
                    }
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 2L, 1L);
    }

    /**
     * Pull all of {@code owner}'s threaded enemies toward {@code to}. Called by ReleaseAnima.recall.
     * Tug strength and a rank-5 group-burst scale with the owner's Soul Thread level.
     */
    public static void tugThreaded(Player owner, Location to) {
        Set<UUID> threaded = THREADS.get(owner.getUniqueId());
        if (threaded == null || threaded.isEmpty()) return;

        // Soul Thread occupies the RLR slot for Psyche.
        PlayerStats stats = PlayerStats.getStats(owner.getUniqueId());
        int level = stats == null ? 1 : stats.getRLRLevel();
        double pull = level >= 4 ? 1.0 : 0.7;
        boolean burst = level >= 5;
        World w = to.getWorld();

        int pulled = 0;
        for (UUID id : new HashSet<>(threaded)) {
            Entity e = Minerva.getInstance().getServer().getEntity(id);
            if (!(e instanceof LivingEntity le) || le.isDead()) {
                threaded.remove(id);
                continue;
            }
            if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
            if (PlayerStats.isSummoned(owner, le)) continue;

            Vector toward = to.clone().toVector().subtract(le.getLocation().toVector());
            if (toward.lengthSquared() > 0.04) {
                toward.normalize().multiply(pull);
                toward.setY(Math.min(0.6, Math.max(0.15, toward.getY() + 0.25)));
                knockback(le, toward);
            }
            Location lc = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
            le.getWorld().spawnParticle(Particle.DUST, lc, 6, 0.2, 0.3, 0.2, 0,
                    new Particle.DustOptions(IridescentSoul.IRIDESCENT[1], 0.7f));
            pulled++;
        }

        if (pulled > 0) {
            w.playSound(to, Sound.BLOCK_CHAIN_PLACE, 1f, 1.4f);
            w.playSound(to, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 1.5f);
        }

        if (burst && pulled >= 2) {
            // when threaded enemies are tugged together, they take a soul-burst
            double dmg = 6 * IridescentSoul.bonusMultiplier(owner, level);
            for (UUID id : new HashSet<>(threaded)) {
                Entity e = Minerva.getInstance().getServer().getEntity(id);
                if (!(e instanceof LivingEntity le) || le.isDead()) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                if (PlayerStats.isSummoned(owner, le)) continue;
                if (to.distanceSquared(le.getLocation()) <= 16) {
                    damage(le, dmg, owner);
                }
            }
            w.spawnParticle(Particle.DUST, to.clone().add(0, 1, 0), 24, 1.2, 0.6, 1.2, 0,
                    new Particle.DustOptions(IridescentSoul.IRIDESCENT[3], 1.1f));
            w.playSound(to, Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1.2f);
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Dash through enemies, threading each with a soul-line.";
            case 2 -> ChatColor.GRAY + "Threads last longer.";
            case 3 -> ChatColor.GRAY + "Threaded enemies are slowed.";
            case 4 -> ChatColor.GRAY + "Stronger tug when you reel threads in.";
            case 5 -> ChatColor.GRAY + "Tugged-together enemies take a soul-burst.";
            default -> ChatColor.GRAY + "Dash through enemies, threading each.";
        };
    }

    @Override
    public String toString() {
        return "soulThread";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PHANTOM_MEMBRANE),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Soul Thread]",
                ChatColor.GRAY + "Dash through your foes, " + ChatColor.LIGHT_PURPLE + "threading" + ChatColor.GRAY + " each one",
                ChatColor.GRAY + "to your soul. Recall to reel them all in.");
    }
}
