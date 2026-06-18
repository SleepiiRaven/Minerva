package net.minervamc.minerva.skills.greek.demeter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HarvestBlessing extends Skill {

    static final List<ThornPatch> ACTIVE_THORNS = new CopyOnWriteArrayList<>();

    private static final Color[] THORN_RING = {
        Color.fromRGB(40, 130, 30), Color.fromRGB(65, 155, 45),
        Color.fromRGB(85, 175, 60), Color.fromRGB(50, 110, 35),
    };
    private static final Color[] BLIGHT_RING = {
        Color.fromRGB(15, 55, 10), Color.fromRGB(30, 75, 20),
        Color.fromRGB(45, 95, 30), Color.fromRGB(10, 40, 8),
    };
    static final Color[] DETONATE_BURST = {
        Color.fromRGB(130, 230, 65), Color.fromRGB(205, 225, 85),
        Color.fromRGB(255, 235, 105), Color.fromRGB(70, 190, 45),
    };

    public static class ThornPatch {
        public final Location center;
        public final double radius;
        public final UUID ownerUUID;
        public final boolean isBlightZone;
        public final boolean nearWater;
        private final double tickDmg;
        private final double detonateDmg;
        private final int durationTicks;
        boolean detonated = false;
        private BukkitRunnable runnable;

        ThornPatch(Location center, double radius, UUID ownerUUID, boolean isBlightZone,
                   boolean nearWater, double tickDmg, double detonateDmg, int durationTicks) {
            this.center = center.clone();
            this.radius = radius;
            this.ownerUUID = ownerUUID;
            this.isBlightZone = isBlightZone;
            this.nearWater = nearWater;
            this.tickDmg = tickDmg;
            this.detonateDmg = detonateDmg;
            this.durationTicks = durationTicks;
        }

        void start() {
            Color[] ring = isBlightZone ? BLIGHT_RING : THORN_RING;
            runnable = new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (detonated || t >= durationTicks) {
                        ACTIVE_THORNS.remove(ThornPatch.this);
                        cancel();
                        return;
                    }
                    Player owner = Bukkit.getPlayer(ownerUUID);
                    if (owner == null) { ACTIVE_THORNS.remove(ThornPatch.this); cancel(); return; }

                    int pts = Math.max(12, (int)(radius * 12));
                    for (Vector v : ParticleUtils.getCirclePoints(radius, pts)) {
                        center.getWorld().spawnParticle(Particle.DUST,
                            center.clone().add(v).add(0, 0.05, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(ring[(int)(Math.random() * ring.length)],
                                isBlightZone ? 1.3f : 1.0f));
                    }
                    // Cherry blossom petals drifting inside the patch — organic, alive feel
                    if (t % 3 == 0) {
                        double ox = (Math.random() - 0.5) * radius * 1.8;
                        double oz = (Math.random() - 0.5) * radius * 1.8;
                        center.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
                            center.clone().add(ox, 0.8, oz), 2, 0.2, 0.3, 0.2, 0.02);
                    }
                    if (t % 4 == 0) {
                        double ox = (Math.random() - 0.5) * radius * 1.6;
                        double oz = (Math.random() - 0.5) * radius * 1.6;
                        center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                            center.clone().add(ox, 0.1, oz), 3, 0.05, 0.5, 0.05, 0.01);
                    }

                    if (t % 10 == 0) {
                        for (Entity e : center.getWorld().getNearbyEntities(center, radius, 2.5, radius)) {
                            if (!(e instanceof LivingEntity living) || e == owner) continue;
                            if (e instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                            if (PlayerStats.isSummoned(owner, e)) continue;
                            double dmg = tickDmg * (e.getScoreboardTags().contains("demeterRooted") ? 2.0 : 1.0);
                            Skill.damage(living, dmg, owner, true, true);
                            if (isBlightZone) {
                                int slow = nearWater ? 2 : 1;
                                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, slow));
                                if (nearWater) living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0));
                            }
                        }
                    }
                    t++;
                }
            };
            runnable.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        }

        public void detonate(Player owner) {
            if (detonated) return;
            detonated = true;
            if (runnable != null) runnable.cancel();
            ACTIVE_THORNS.remove(this);

            double det = isBlightZone ? radius + 2.0 : radius;

            // Expanding ring
            new BukkitRunnable() {
                double r = 0.3;
                @Override public void run() {
                    if (r > det + 1.5) { cancel(); return; }
                    for (Vector v : ParticleUtils.getCirclePoints(r, Math.max(10, (int)(r * 14)))) {
                        center.getWorld().spawnParticle(Particle.DUST,
                            center.clone().add(v).add(0, 0.1, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(DETONATE_BURST[(int)(Math.random() * DETONATE_BURST.length)], 1.7f));
                    }
                    r += 0.55;
                }
            }.runTaskTimer(Minerva.getInstance(), 0L, 1L);

            center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.3, 0), 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 0.5, 0),
                35 + (isBlightZone ? 25 : 0), det / 3, 0.5, det / 3, 0.25);
            center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, center.clone().add(0, 1.5, 0),
                40, det / 2, 0.8, det / 2, 0.15);
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 1, 0),
                25, det / 2.5, 0.6, det / 2.5, 0.18);
            center.getWorld().spawnParticle(Particle.COMPOSTER, center.clone().add(0, 0.3, 0), 20, 0.4, 0.2, 0.4, 0.18);

            if (owner.getWorld().isThundering()) {
                center.getWorld().strikeLightningEffect(center);
            }
            center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 1f, 0.55f);
            center.getWorld().playSound(center, Sound.ITEM_BONE_MEAL_USE, 0.7f, 0.5f);

            // Detonation damage
            for (Entity e : center.getWorld().getNearbyEntities(center, det, 3, det)) {
                if (!(e instanceof LivingEntity living) || e == owner) continue;
                if (e instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                if (PlayerStats.isSummoned(owner, e)) continue;
                double dmg = detonateDmg;
                if (e.getScoreboardTags().contains("demeterRooted")) dmg *= 1.5;
                if (e.getScoreboardTags().contains("demeterSeeded")) dmg += 10;
                Skill.damage(living, dmg, owner, true, true);
            }
        }
    }

    // Called by each skill when it hits an enemy — passive effect only
    public static void spawnThorn(Player owner, Location loc) {
        Skill passive = PlayerStats.getStats(owner.getUniqueId()).getPassive();
        if (passive == null || !passive.toString().equals("harvestBlessing")) return;
        if (!PlayerStats.getStats(owner.getUniqueId()).getPassiveActive()) return;

        Location ownerLoc = owner.getLocation();
        boolean enhanced = isNaturalGround(
            owner.getWorld().getBlockAt(ownerLoc.getBlockX(), ownerLoc.getBlockY() - 1, ownerLoc.getBlockZ()));
        double r = enhanced ? 3.0 : 1.5;
        ThornPatch p = new ThornPatch(loc, r, owner.getUniqueId(), false, false, 1.5, 20.0, 100);
        ACTIVE_THORNS.add(p);
        p.start();
    }

    // Called by ThornBlight — always creates a zone, passive not required
    public static void spawnBlightZone(Player owner, Location loc, boolean nearWater) {
        double r = nearWater ? 4.5 : 4.0;
        double dmg = nearWater ? 3.0 : 2.0;
        ThornPatch p = new ThornPatch(loc, r, owner.getUniqueId(), true, nearWater, dmg, 28.0, 120);
        ACTIVE_THORNS.add(p);
        p.start();
    }

    // Called by SeedBarrage on ground impact — small sprout patch
    public static void spawnSeedThorn(Player owner, Location loc) {
        ThornPatch p = new ThornPatch(loc, 1.2, owner.getUniqueId(), false, false, 1.0, 8.0, 60);
        ACTIVE_THORNS.add(p);
        p.start();
    }

    public static List<ThornPatch> getActiveThorns(UUID uuid) {
        List<ThornPatch> result = new ArrayList<>();
        for (ThornPatch t : ACTIVE_THORNS) {
            if (t.ownerUUID.equals(uuid) && !t.detonated) result.add(t);
        }
        return result;
    }

    public static void detonateAll(Player player) {
        for (ThornPatch t : getActiveThorns(player.getUniqueId())) {
            t.detonate(player);
        }
    }

    public static boolean isNaturalGround(Block block) {
        return switch (block.getType()) {
            case GRASS_BLOCK, DIRT, FARMLAND, PODZOL, MYCELIUM,
                 ROOTED_DIRT, COARSE_DIRT, MUD -> true;
            default -> false;
        };
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "harvestBlessing"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.WHEAT)
            .setName(TextContext.formatLegacy("&lHarvest Blessing", false).color(TextColor.color(100, 200, 60)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Hitting enemies with skills leaves a", false),
                TextContext.formatLegacy("&aThorn Patch &7at their feet (damage over time).", false),
                TextContext.formatLegacy("&7Standing on &2grass/dirt/farmland &7doubles", false),
                TextContext.formatLegacy("&7patch size. Patches detonate with &2Harvest Wrath&7.", false)
            )).build();
    }
}
