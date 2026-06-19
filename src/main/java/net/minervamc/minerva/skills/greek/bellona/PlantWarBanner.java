package net.minervamc.minerva.skills.greek.bellona;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * Bellona RLR — plant firing banner emplacements. Each banner is an invisible invulnerable
 * ArmorStand wearing a banner on its head that periodically fires a damaging pulse at the
 * nearest enemy and buffs nearby allies. The banners are the core of the kit: other skills
 * relocate them, surge them, and converge their fire. 3 charges.
 */
public class PlantWarBanner extends Skill {
    public static final Color[] CRIMSON = {
        Color.fromRGB(139, 0, 0), Color.fromRGB(200, 30, 30),
        Color.fromRGB(255, 60, 60), Color.fromRGB(120, 10, 10),
    };

    /** Live registry of every active banner across all owners. */
    public static final List<WarBanner> BANNERS = new CopyOnWriteArrayList<>();

    public static List<WarBanner> getBanners(UUID owner) {
        List<WarBanner> out = new ArrayList<>();
        for (WarBanner b : BANNERS) {
            if (b.owner.equals(owner) && b.stand != null && b.stand.isValid()) out.add(b);
        }
        return out;
    }

    /** The owner's banner nearest to the player, or null. */
    public static WarBanner nearestBanner(Player p) {
        WarBanner best = null;
        double bestDist = Double.MAX_VALUE;
        for (WarBanner b : getBanners(p.getUniqueId())) {
            double d = b.stand.getLocation().distanceSquared(p.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = b;
            }
        }
        return best;
    }

    /** Move a banner to a new location and refresh its lifetime. */
    public static void relocate(WarBanner b, Location loc) {
        if (b == null || b.stand == null || !b.stand.isValid()) return;
        Location dest = loc.clone().add(0, -1.4, 0);
        b.stand.teleport(dest);
        b.ticksLived = 0;
        World w = b.stand.getWorld();
        w.playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1f, 0.9f);
        w.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.4f);
        for (Vector v : ParticleUtils.getCirclePoints(0.8, 14)) {
            w.spawnParticle(Particle.DUST, loc.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(CRIMSON[1], 1.1f));
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 5500;
            case 4, 5 -> 5000;
            default -> 6000;
        };

        // 3 charges: find the first available key
        String chosen = null;
        for (int i = 0; i < 3; i++) {
            if (cooldownManager.isCooldownDone(player.getUniqueId(), "plantWarBanner" + i)) {
                chosen = "plantWarBanner" + i;
                break;
            }
        }
        if (chosen == null) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), chosen, cooldown);
        cooldownAlarm(player, cooldown, "War Banner");

        // place at the aimed block, falling back to the player's feet
        Location place = player.getTargetBlockExact(6) != null
                ? player.getTargetBlockExact(6).getLocation().add(0.5, 1, 0.5)
                : player.getLocation();

        int maxBanners = level >= 3 ? 4 : 3;
        List<WarBanner> mine = getBanners(player.getUniqueId());
        while (mine.size() >= maxBanners) {
            WarBanner oldest = mine.get(0);
            for (WarBanner b : mine) {
                if (b.ticksLived > oldest.ticksLived) oldest = b;
            }
            oldest.remove();
            mine = getBanners(player.getUniqueId());
        }

        WarBanner banner = new WarBanner(player.getUniqueId(), place, level);
        banner.start();

        World w = player.getWorld();
        w.playSound(place, Sound.BLOCK_CHAIN_PLACE, 1.1f, 0.8f);
        w.playSound(place, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9f, 1.2f);
        w.playSound(place, Sound.ITEM_GOAT_HORN_SOUND_1, 0.6f, 1.3f);
        for (Vector v : ParticleUtils.getCirclePoints(1.0, 18)) {
            w.spawnParticle(Particle.DUST, place.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(CRIMSON[2], 1.1f));
        }
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Plant a firing banner that pulses nearby enemies.";
            case 2 -> ChatColor.GRAY + "Banners buff allies harder; lower charge cooldown.";
            case 3 -> ChatColor.GRAY + "You may field one more banner (4).";
            case 4 -> ChatColor.GRAY + "Each pulse strikes up to 2 targets.";
            case 5 -> ChatColor.GRAY + "Banners knock back attackers when struck.";
            default -> ChatColor.GRAY + "Plant a firing banner emplacement.";
        };
    }

    @Override
    public String toString() {
        return "plantWarBanner";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.WHITE_BANNER),
                ChatColor.RED + "" + ChatColor.BOLD + "[Plant War Banner]",
                ChatColor.GRAY + "Plant a firing " + ChatColor.RED + "banner emplacement" + ChatColor.GRAY + " that",
                ChatColor.GRAY + "pulses nearby enemies and rallies allies.",
                ChatColor.GRAY + "Holds " + ChatColor.RED + "3 charges" + ChatColor.GRAY + " — build your battery.");
    }

    /**
     * A single firing banner emplacement: an invisible invulnerable ArmorStand wearing a banner.
     */
    public static class WarBanner {
        public final UUID owner;
        public final int level;
        public ArmorStand stand;
        public int ticksLived = 0;
        public int lifetime = 320;
        private BukkitRunnable task;

        public WarBanner(UUID owner, Location loc, int level) {
            this.owner = owner;
            this.level = level;
            World w = loc.getWorld();
            Location spawn = loc.clone().add(0, -1.4, 0);
            this.stand = (ArmorStand) w.spawnEntity(spawn, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.RED_BANNER));
            stand.addScoreboardTag("bellonaBanner");
            BANNERS.add(this);
        }

        public Location bannerTop() {
            return stand.getLocation().clone().add(0, 1.8, 0);
        }

        public void start() {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (stand == null || !stand.isValid() || ticksLived >= lifetime) {
                        remove();
                        cancel();
                        return;
                    }

                    Player ownerPlayer = org.bukkit.Bukkit.getPlayer(owner);
                    Location top = bannerTop();
                    World w = top.getWorld();

                    // steady idle ambience (no strobe)
                    if (ticksLived % 8 == 0) {
                        w.spawnParticle(Particle.DUST, top, 0, 0, 0, 0, 0,
                                new Particle.DustOptions(CRIMSON[1], 1f));
                    }

                    // firing pulse every 40 ticks (20 if surged via StandardsCall L5)
                    int rate = stand.getScoreboardTags().contains("bellonaSurgeFast") ? 20 : 40;
                    if (ticksLived > 0 && ticksLived % rate == 0 && ownerPlayer != null) {
                        firePulse(ownerPlayer, top, w);
                        buffAllies(ownerPlayer);
                    }

                    ticksLived++;
                }
            };
            task.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        }

        private void firePulse(Player ownerPlayer, Location top, World w) {
            int maxTargets = level >= 4 ? 2 : 1;
            int hit = 0;
            // sort-ish: just iterate, fire at nearest available
            List<LivingEntity> targets = new ArrayList<>();
            for (Entity e : stand.getNearbyEntities(6, 4, 6)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (le == ownerPlayer) continue;
                if (le.getScoreboardTags().contains("bellonaBanner")) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(ownerPlayer, p)) continue;
                if (PlayerStats.isSummoned(ownerPlayer, le)) continue;
                if (le.hasMetadata("NPC")) continue;
                targets.add(le);
            }
            targets.sort((a, b) -> Double.compare(
                    a.getLocation().distanceSquared(stand.getLocation()),
                    b.getLocation().distanceSquared(stand.getLocation())));

            for (LivingEntity le : targets) {
                if (hit >= maxTargets) break;
                Location tc = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
                for (Vector v : ParticleUtils.getLinePoints(top.toVector(), tc.toVector(), 0.5)) {
                    w.spawnParticle(Particle.DUST, v.toLocation(w), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(CRIMSON[2], 0.9f));
                }
                w.spawnParticle(Particle.DUST, tc, 8, 0.25, 0.25, 0.25, 0,
                        new Particle.DustOptions(CRIMSON[0], 1.1f));
                w.spawnParticle(Particle.CRIT, tc, 6, 0.2, 0.2, 0.2, 0.05);
                damage(le, 4, ownerPlayer);
                w.playSound(tc, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.2f);
                hit++;
            }
        }

        private void buffAllies(Player ownerPlayer) {
            List<Player> allies = new ArrayList<>();
            List<Player> party = Party.partyList(ownerPlayer);
            if (party != null) allies.addAll(party);
            if (!allies.contains(ownerPlayer)) allies.add(ownerPlayer);
            for (Player ally : allies) {
                if (!ally.isOnline()) continue;
                if (ally.getWorld() != stand.getWorld()) continue;
                if (ally.getLocation().distanceSquared(stand.getLocation()) <= 16) {
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0));
                }
            }
        }

        public void remove() {
            if (stand != null && stand.isValid()) {
                Location top = bannerTop();
                top.getWorld().spawnParticle(Particle.DUST, top, 10, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(CRIMSON[3], 1f));
                top.getWorld().playSound(top, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.9f);
                stand.remove();
            }
            BANNERS.remove(this);
            if (task != null) {
                try { task.cancel(); } catch (IllegalStateException ignored) {}
            }
        }
    }
}
