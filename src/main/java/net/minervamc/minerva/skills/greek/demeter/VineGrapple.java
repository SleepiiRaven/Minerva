package net.minervamc.minerva.skills.greek.demeter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class VineGrapple extends Skill {

    private static final Map<UUID, GrappleState> ACTIVE = new ConcurrentHashMap<>();
    private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, BukkitTask> STATE_TIMERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BOOST = new ConcurrentHashMap<>();

    private static final int MAX_RANGE = 25;
    private static final double PROJECTILE_SPEED = 1.9;
    // Pendulum constraint physics — not spring pull
    private static final double CORRECTION_K = 0.35; // position drift correction per tick
    private static final double MAX_CORRECTION = 0.6; // cap on correction impulse
    private static final double MAX_SPEED = 1.9;      // velocity cap during swing
    // Boost
    private static final double BOOST_IMPULSE = 0.9;  // blocks/tick toward anchor
    private static final double BOOST_REEL = 5.0;     // blocks reeled in per boost
    private static final long BOOST_COOLDOWN_MS = 650;
    // Detach threshold
    private static final double MIN_ROPE = 1.8;

    private static final Color[] VINE_COLORS = {
        Color.fromRGB(35, 120, 20), Color.fromRGB(55, 148, 32),
        Color.fromRGB(72, 165, 48), Color.fromRGB(25, 100, 14),
    };

    static final class GrappleState {
        final Location anchor;
        final Location anchorBlock; // block the vine is actually embedded in
        double ropeLen;             // mutable — changed by boost
        final BukkitRunnable physicsLoop;
        final ItemDisplay vineDisplay;

        GrappleState(Location anchor, Location anchorBlock, double ropeLen,
                     BukkitRunnable physicsLoop, ItemDisplay vineDisplay) {
            this.anchor = anchor.clone();
            this.anchorBlock = anchorBlock.clone();
            this.ropeLen = ropeLen;
            this.physicsLoop = physicsLoop;
            this.vineDisplay = vineDisplay;
        }

        void release() {
            physicsLoop.cancel();
            if (vineDisplay != null && vineDisplay.isValid()) vineDisplay.remove();
        }
    }

    // ─── Cast ──────────────────────────────────────────────────────────────────

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        if (player.getScoreboardTags().contains("vineGrappleActive")) {
            exitState(player);
            return;
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "vineGrapple")) {
            onCooldown(player);
            return;
        }

        enterState(player);
    }

    // ─── State management ─────────────────────────────────────────────────────

    private static void enterState(Player player) {
        player.addScoreboardTag("vineGrappleActive");

        Location loc = player.getLocation().add(0, 1, 0);
        for (Color c : VINE_COLORS) {
            loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.5, 0.6, 0.5, 0,
                new Particle.DustOptions(c, 1.3f));
        }
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 18, 0.5, 0.6, 0.5, 0.02);
        loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 14, 0.5, 0.7, 0.5, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 1f, 0.75f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.7f, 1.1f);
        player.sendActionBar(Component.text("✦ Vine active — left click to latch!", TextColor.color(65, 155, 40)));

        BukkitTask timer = new BukkitRunnable() {
            @Override public void run() { exitState(player); }
        }.runTaskLater(Minerva.getInstance(), 500L);
        STATE_TIMERS.put(player.getUniqueId(), timer);
    }

    public static void exitState(Player player) {
        if (!player.getScoreboardTags().contains("vineGrappleActive")) return;
        player.removeScoreboardTag("vineGrappleActive");
        BukkitTask timer = STATE_TIMERS.remove(player.getUniqueId());
        if (timer != null) timer.cancel();
        releaseGrapple(player);
        IN_FLIGHT.remove(player.getUniqueId());
        LAST_BOOST.remove(player.getUniqueId());

        long cooldown = 10000L;
        CooldownManager cd = Minerva.getInstance().getCdInstance();
        cd.setCooldownFromNow(player.getUniqueId(), "vineGrapple", cooldown);
        cooldownAlarm(player, cooldown, "Vine Grapple");
        player.sendActionBar(Component.text("✦ Vine Grapple faded.", TextColor.color(120, 80, 40)));
    }

    // ─── Left click routing ───────────────────────────────────────────────────

    public static void onLeftClick(Player player) {
        if (ACTIVE.containsKey(player.getUniqueId())) {
            boostGrapple(player);
        } else if (!IN_FLIGHT.contains(player.getUniqueId())) {
            launchVine(player);
        }
    }

    // ─── Boost ────────────────────────────────────────────────────────────────

    private static void boostGrapple(Player player) {
        GrappleState state = ACTIVE.get(player.getUniqueId());
        if (state == null) return;

        long now = System.currentTimeMillis();
        Long last = LAST_BOOST.get(player.getUniqueId());
        if (last != null && now - last < BOOST_COOLDOWN_MS) return;
        LAST_BOOST.put(player.getUniqueId(), now);

        Vector impulse = player.getEyeLocation().getDirection().normalize().multiply(BOOST_IMPULSE);
        Vector vel = player.getVelocity().add(impulse);
        if (vel.length() > MAX_SPEED) vel = vel.normalize().multiply(MAX_SPEED);
        player.setVelocity(vel);
        state.ropeLen = Math.max(state.ropeLen - BOOST_REEL, MIN_ROPE);

        Location loc = player.getLocation().add(0, 1, 0);
        for (Color c : VINE_COLORS) {
            loc.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(c, 1.3f));
        }
        loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 12, 0.5, 0.5, 0.5, 0.09);
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 6, 0.3, 0.4, 0.3, 0.015);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.9f, 1.6f);
        player.sendActionBar(Component.text("↗ VINE BOOST!", TextColor.color(85, 195, 60)));
    }

    // ─── Vine launch ──────────────────────────────────────────────────────────

    private static void launchVine(Player player) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation().add(dir.clone().multiply(1.2));

        ItemDisplay vine = player.getWorld().spawn(start, ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.BAMBOO));
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        });

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.8f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.45f, 0.7f);
        IN_FLIGHT.add(player.getUniqueId());

        new BukkitRunnable() {
            int t = 0;
            double dist = 0;

            @Override
            public void run() {
                if (!vine.isValid() || !player.isOnline()
                        || !player.getScoreboardTags().contains("vineGrappleActive")) {
                    vine.remove();
                    IN_FLIGHT.remove(player.getUniqueId());
                    cancel(); return;
                }
                if (dist >= MAX_RANGE || t > 50) {
                    vine.remove();
                    IN_FLIGHT.remove(player.getUniqueId());
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_VINE_BREAK, 0.5f, 1.4f);
                    cancel(); return;
                }

                Matrix4f spin = new Matrix4f().identity()
                    .rotateY((float) Math.toRadians(t * 30f))
                    .rotateX((float) Math.toRadians(t * 18f));
                vine.setTransformationMatrix(spin);
                vine.setInterpolationDelay(0);
                vine.setInterpolationDuration(1);

                vine.teleport(vine.getLocation().add(dir.clone().multiply(PROJECTILE_SPEED)));
                dist += PROJECTILE_SPEED;

                Location loc = vine.getLocation();
                for (Color c : VINE_COLORS) {
                    if (Math.random() < 0.45)
                        loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.07, 0.07, 0.07, 0,
                            new Particle.DustOptions(c, 1.0f));
                }
                if (t % 2 == 0) loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 1, 0.05, 0.1, 0.05, 0.01);
                if (t % 3 == 0) loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 1, 0.1, 0.1, 0.1, 0.04);

                // Entity hit — before block check so vine grabs enemies in front of walls
                for (Entity e : vine.getWorld().getNearbyEntities(loc, 0.75, 0.75, 0.75)) {
                    if (!(e instanceof LivingEntity living) || e == player || e == vine) continue;
                    if (e instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, e)) continue;
                    IN_FLIGHT.remove(player.getUniqueId());
                    onEntityHit(player, vine, living);
                    cancel(); return;
                }

                // Block hit — latch
                if (loc.clone().add(dir.clone().multiply(0.5)).getBlock().isSolid()) {
                    IN_FLIGHT.remove(player.getUniqueId());
                    latchToBlock(player, vine, loc.clone(), dir.clone());
                    cancel();
                }

                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    // ─── Entity hit ───────────────────────────────────────────────────────────

    private static void onEntityHit(Player player, ItemDisplay vine, LivingEntity target) {
        vine.remove();
        exitState(player); // hitting an enemy consumes the state

        RootSurge.applyRoot(player, target, 50);
        HarvestBlessing.spawnThorn(player, target.getLocation());
        Skill.damage(target, 6, player, true, true);

        Vector pull = player.getLocation().add(0, 0.9, 0)
            .subtract(target.getLocation().add(0, 0.9, 0)).toVector();
        if (pull.length() > 0.5)
            target.setVelocity(pull.normalize().multiply(1.2).add(new Vector(0, 0.35, 0)));

        Location hitLoc = target.getLocation().add(0, 1, 0);
        for (Color c : VINE_COLORS)
            hitLoc.getWorld().spawnParticle(Particle.DUST, hitLoc, 6, 0.3, 0.5, 0.3, 0, new Particle.DustOptions(c, 1.4f));
        hitLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, hitLoc, 28, 0.5, 0.7, 0.5, 0.1);
        hitLoc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, hitLoc, 15, 0.4, 0.6, 0.4, 0.02);
        hitLoc.getWorld().spawnParticle(Particle.FLASH, hitLoc, 1, 0, 0, 0, 0);
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1.2f, 0.6f);
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GRASS_HIT, 1f, 1.0f);
        hitLoc.getWorld().playSound(hitLoc, Sound.ITEM_BONE_MEAL_USE, 0.7f, 0.8f);
    }

    // ─── Block latch ──────────────────────────────────────────────────────────

    private static void latchToBlock(Player player, ItemDisplay vine, Location anchor, Vector hitDir) {
        vine.setTransformationMatrix(new Matrix4f().identity().scaling(0.55f));
        vine.setInterpolationDelay(0);
        vine.setInterpolationDuration(2);

        anchor.getWorld().spawnParticle(Particle.CHERRY_LEAVES, anchor, 18, 0.3, 0.3, 0.3, 0.09);
        for (Color c : VINE_COLORS)
            anchor.getWorld().spawnParticle(Particle.DUST, anchor, 4, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(c, 1.5f));
        anchor.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, anchor, 10, 0.2, 0.3, 0.2, 0.02);
        anchor.getWorld().playSound(anchor, Sound.BLOCK_GRASS_HIT, 1.2f, 0.65f);
        anchor.getWorld().playSound(anchor, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.8f, 0.55f);

        Location anchorBlock = anchor.clone().add(hitDir.clone().multiply(0.5));
        double initialDist = player.getLocation().add(0, 0.9, 0).distance(anchor);
        double startRope = Math.max(initialDist, MIN_ROPE);

        BukkitRunnable physicsLoop = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                GrappleState state = ACTIVE.get(player.getUniqueId());
                if (state == null || !player.isOnline()) { cancel(); return; }

                // Release if anchor block was broken
                if (!anchorBlock.getBlock().isSolid()) {
                    releaseGrapple(player); cancel(); return;
                }
                if (t > 400) { releaseGrapple(player); cancel(); return; }

                Location pCenter = player.getLocation().add(0, 0.9, 0);
                Vector toAnchor = state.anchor.clone().subtract(pCenter).toVector();
                double dist = toAnchor.length();

                // Auto-detach when close enough (reeled all the way in via boosts)
                if (dist <= MIN_ROPE + 0.4) {
                    releaseGrapple(player);
                    cancel(); return;
                }

                // ── Pendulum constraint physics ──────────────────────────────
                // The rope is taut — remove any velocity component pulling AWAY from the anchor.
                // Tangential (swing) velocity is left completely untouched.
                // A small corrective impulse snaps the player back onto the rope sphere.
                if (dist > state.ropeLen) {
                    Vector norm = toAnchor.clone().normalize(); // toward anchor
                    Vector vel = player.getVelocity();

                    // radialVel: positive = moving toward anchor, negative = moving away
                    double radialVel = vel.dot(norm);
                    if (radialVel < 0) {
                        // Strip the outward component so the rope constrains without redirecting
                        vel = vel.clone().subtract(norm.clone().multiply(radialVel));
                    }

                    // Gentle position correction to prevent the rope from stretching over time
                    double overshoot = dist - state.ropeLen;
                    vel = vel.add(norm.clone().multiply(Math.min(overshoot * CORRECTION_K, MAX_CORRECTION)));

                    if (vel.length() > MAX_SPEED) vel = vel.normalize().multiply(MAX_SPEED);
                    player.setVelocity(vel);
                }

                // ── Rope visual ──────────────────────────────────────────────
                Location ropeStart = player.getLocation().add(0, 0.9, 0);
                Vector rope = state.anchor.clone().subtract(ropeStart).toVector();
                double ropeDist = rope.length();
                if (ropeDist > 0.3) {
                    Vector step = rope.clone().normalize().multiply(0.5);
                    int numPts = (int)(ropeDist / 0.5);
                    double sagAmt = ropeDist * 0.07;
                    for (int i = 1; i <= numPts; i++) {
                        Location pt = ropeStart.clone().add(step.clone().multiply(i));
                        double sag = Math.sin(Math.PI * (double) i / numPts) * sagAmt;
                        pt.subtract(0, sag, 0);
                        pt.getWorld().spawnParticle(Particle.DUST, pt, 0, 0, 0, 0, 0,
                            new Particle.DustOptions(VINE_COLORS[i % VINE_COLORS.length], 0.72f));
                        if (t % 3 == 0 && i % 4 == 0)
                            pt.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, pt, 1, 0.05, 0.15, 0.05, 0.008);
                        if (t % 5 == 0 && i % 7 == 0)
                            pt.getWorld().spawnParticle(Particle.CHERRY_LEAVES, pt, 1, 0.08, 0.1, 0.08, 0.03);
                    }
                }
                if (t % 6 == 0)
                    state.anchor.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, state.anchor, 3, 0.12, 0.12, 0.12, 0.01);
                if (t % 10 == 0)
                    state.anchor.getWorld().spawnParticle(Particle.CHERRY_LEAVES, state.anchor, 2, 0.1, 0.1, 0.1, 0.03);

                t++;
            }
        };

        GrappleState state = new GrappleState(anchor, anchorBlock, startRope, physicsLoop, vine);
        ACTIVE.put(player.getUniqueId(), state);
        physicsLoop.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    // ─── Release / query ──────────────────────────────────────────────────────

    public static void releaseGrapple(Player player) {
        GrappleState state = ACTIVE.remove(player.getUniqueId());
        if (state == null) return;
        state.release();
        LAST_BOOST.remove(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_VINE_BREAK, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
            player.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.07);
    }

    public static boolean isLatched(Player player) {
        return ACTIVE.containsKey(player.getUniqueId());
    }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "vineGrapple"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.BAMBOO)
            .setName(TextContext.formatLegacy("&lVine Grapple", false).color(TextColor.color(65, 155, 40)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Activate vines for &a15s&7. Left click to shoot.", false),
                TextContext.formatLegacy("&2Hits surface&7: latches. Swing freely!", false),
                TextContext.formatLegacy("&7&oLeft click again: vine boost + reel in.", false),
                TextContext.formatLegacy("&7Sneak to release latch (re-fire from there).", false),
                TextContext.formatLegacy("&2Hits enemy&7: roots, yanks, &aThorn Patch&7.", false)
            )).build();
    }
}
