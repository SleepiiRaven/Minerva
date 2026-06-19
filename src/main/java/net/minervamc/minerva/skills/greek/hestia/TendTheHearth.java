package net.minervamc.minerva.skills.greek.hestia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hestia RLR — place (or move) the Hearth: a stationary warm font. Party allies who stand near it
 * are regenerated and cleansed periodically, and the player banks Embers twice as fast nearby.
 * Anchors the whole kit. No real fire is ever placed (a hidden marker ArmorStand + particles).
 */
public class TendTheHearth extends Skill {
    private static final Map<UUID, Location> HEARTH = new HashMap<>();
    private static final Map<UUID, ArmorStand> MARKERS = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> TASKS = new HashMap<>();

    private static final PotionEffectType[] CLEANSEABLE = {
        PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.MINING_FATIGUE,
        PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.BLINDNESS,
        PotionEffectType.NAUSEA, PotionEffectType.DARKNESS,
    };

    public static Location getHearth(Player player) {
        return HEARTH.get(player.getUniqueId());
    }

    public static boolean nearHearth(Player player, double r) {
        Location h = HEARTH.get(player.getUniqueId());
        if (h == null) return false;
        if (h.getWorld() == null || !h.getWorld().equals(player.getWorld())) return false;
        return h.distanceSquared(player.getLocation()) <= r * r;
    }

    /** Cleanse up to {@code count} negative effects from an entity; returns how many were removed. */
    private static int cleanse(LivingEntity le, int count) {
        int removed = 0;
        for (PotionEffectType type : CLEANSEABLE) {
            if (removed >= count) break;
            if (le.hasPotionEffect(type)) {
                le.removePotionEffect(type);
                removed++;
            }
        }
        return removed;
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 9500;
            case 4, 5 -> 9000;
            default -> 10000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tendTheHearth")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tendTheHearth", cooldown);
        cooldownAlarm(player, cooldown, "Tend the Hearth");

        UUID id = player.getUniqueId();
        World world = player.getWorld();
        Location loc = player.getLocation().clone();

        // tear down any prior hearth
        ArmorStand oldMarker = MARKERS.remove(id);
        if (oldMarker != null && !oldMarker.isDead()) oldMarker.remove();
        BukkitRunnable oldTask = TASKS.remove(id);
        if (oldTask != null) {
            try { oldTask.cancel(); } catch (IllegalStateException ignored) {}
        }

        HEARTH.put(id, loc);
        ArmorStand marker = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setMarker(true);
        marker.setSmall(true);
        marker.setGravity(false);
        MARKERS.put(id, marker);

        world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f);
        world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 1f);
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.2f);

        final int fLevel = level;
        final double auraRadius = level >= 2 ? 5.0 : 4.0;
        final int cleanseCount = level >= 4 ? 2 : 1;

        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline() || marker.isDead() || !loc.equals(HEARTH.get(id))) {
                    marker.remove();
                    MARKERS.remove(id);
                    TASKS.remove(id);
                    HEARTH.remove(id);
                    cancel();
                    return;
                }

                drawHearth(loc, t);

                // pulse buff every 40 ticks
                if (t % 40 == 0) {
                    List<Player> allies = Party.partyList(player);
                    // party allies within radius
                    if (allies != null) {
                        for (Player ally : allies) {
                            if (!ally.isOnline() || ally.isDead()) continue;
                            if (ally.getWorld() != loc.getWorld()) continue;
                            if (ally.getLocation().distanceSquared(loc) <= auraRadius * auraRadius) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
                                cleanse(ally, cleanseCount);
                            }
                        }
                    }
                    // caster within radius
                    if (player.getWorld() == loc.getWorld()
                            && player.getLocation().distanceSquared(loc) <= auraRadius * auraRadius) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
                        cleanse(player, cleanseCount);
                    }
                    // L3: regen Hestia anywhere
                    if (fLevel >= 3 && player.isOnline() && !player.isDead()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0));
                    }
                    // L5: knock back enemies standing right on the hearth
                    if (fLevel >= 5) {
                        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.6, 1.6, 1.6)) {
                            if (!(e instanceof LivingEntity le) || e == player) continue;
                            if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                            if (net.minervamc.minerva.PlayerStats.isSummoned(player, le)) continue;
                            Vector away = le.getLocation().toVector().subtract(loc.toVector()).setY(0);
                            if (away.lengthSquared() < 0.04) away = player.getLocation().getDirection().setY(0);
                            away.normalize().multiply(0.8).setY(0.4);
                            knockback(le, away);
                        }
                        loc.getWorld().playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.6f, 1.4f);
                    }
                }
                t++;
            }
        };
        task.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        TASKS.put(id, task);
    }

    /** Steady warm glow: fixed flames, rising motes, and an amber ground ring that breathes. */
    private static void drawHearth(Location loc, int t) {
        World world = loc.getWorld();
        if (world == null) return;

        // a few FLAME at fixed offsets (NOT flickering — same offsets every frame)
        Location core = loc.clone().add(0, 0.3, 0);
        world.spawnParticle(Particle.FLAME, core, 2, 0.12, 0.18, 0.12, 0.0);
        world.spawnParticle(Particle.SMALL_FLAME, core, 2, 0.1, 0.2, 0.1, 0.0);

        // rising warm motes
        if (t % 2 == 0) {
            Color c = BankedEmbers.HEARTH[(t / 2) % BankedEmbers.HEARTH.length];
            world.spawnParticle(Particle.DUST, loc.clone().add(0, 0.5 + (t % 10) * 0.06, 0), 1, 0.15, 0.1, 0.15, 0,
                    new Particle.DustOptions(c, 1.1f));
        }

        // amber ground ring that breathes via sin
        double radius = 1.4 + 0.25 * Math.sin(t * 0.12);
        List<Vector> ring = ParticleUtils.getCirclePoints(radius, 18);
        for (int i = 0; i < ring.size(); i++) {
            Color c = BankedEmbers.HEARTH[i % BankedEmbers.HEARTH.length];
            world.spawnParticle(Particle.DUST, loc.clone().add(ring.get(i)).add(0, 0.06, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, 1f));
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Place a Hearth: nearby allies regen + cleanse; you bank faster.";
            case 2 -> ChatColor.GRAY + "Wider aura (5 blocks).";
            case 3 -> ChatColor.GRAY + "You regenerate anywhere while the Hearth burns.";
            case 4 -> ChatColor.GRAY + "The Hearth cleanses two debuffs per pulse.";
            case 5 -> ChatColor.GRAY + "Enemies standing on the Hearth are knocked away.";
            default -> ChatColor.GRAY + "Place a Hearth that heals and cleanses nearby allies.";
        };
    }

    @Override
    public String toString() {
        return "tendTheHearth";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.CAMPFIRE),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Tend the Hearth]",
                ChatColor.GRAY + "Place or move your " + ChatColor.GOLD + "Hearth" + ChatColor.GRAY + ". Allies nearby are",
                ChatColor.GRAY + "steadily " + ChatColor.GOLD + "healed" + ChatColor.GRAY + " and " + ChatColor.GOLD + "cleansed" + ChatColor.GRAY + ", and you",
                ChatColor.GRAY + "bank Embers twice as fast while close to it.");
    }
}
