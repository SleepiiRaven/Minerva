package net.minervamc.minerva.skills.greek.hermes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class GrappleHook extends Skill {

    private static final Map<UUID, GrappleState> ACTIVE = new ConcurrentHashMap<>();

    private static final int MAX_RANGE = 30;
    private static final double PROJECTILE_SPEED = 2.0;
    // Spring: only pulls player when farther than current rope length (one-directional tether)
    private static final double SPRING_K = 0.10;
    private static final double REEL_RATE = 0.957; // rope shortens ~4.3%/tick; reaches min in ~40 ticks
    private static final double MIN_ROPE = 1.8;     // detach when within this distance
    private static final double MAX_SPEED = 1.3;    // velocity cap (blocks/tick)

    private static final Color ROPE_COLOR = Color.fromRGB(155, 135, 95);
    private static final Color HOOK_COLOR = Color.fromRGB(215, 185, 100);

    private static final class GrappleState {
        final Location anchor;
        final BukkitRunnable physicsLoop;
        final ItemDisplay hookDisplay;

        GrappleState(Location anchor, BukkitRunnable physicsLoop, ItemDisplay hookDisplay) {
            this.anchor = anchor.clone();
            this.physicsLoop = physicsLoop;
            this.hookDisplay = hookDisplay;
        }

        void release() {
            physicsLoop.cancel();
            if (hookDisplay.isValid()) hookDisplay.remove();
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        // Re-cast while hooked = release
        if (ACTIVE.containsKey(player.getUniqueId())) {
            releaseGrapple(player);
            return;
        }

        long cooldown = 10000;
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "grappleHook")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "grappleHook", cooldown);
        cooldownAlarm(player, cooldown, "Grapple Hook");

        fireHook(player);
    }

    private void fireHook(Player player) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation().add(dir.clone().multiply(1.2));

        // Tripwire hook item rotated to face the throw direction
        ItemDisplay hook = player.getWorld().spawn(start, ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.TRIPWIRE_HOOK));
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
        });

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 1.3f);

        new BukkitRunnable() {
            int t = 0;
            double dist = 0;
            @Override
            public void run() {
                if (!hook.isValid()) { cancel(); return; }
                if (!player.isOnline() || ACTIVE.containsKey(player.getUniqueId())) {
                    hook.remove(); cancel(); return;
                }
                if (dist >= MAX_RANGE || t > 50) {
                    hook.remove(); cancel(); return;
                }

                // Spin on the Z axis during flight so it looks like it's tumbling through the air
                Matrix4f spin = new Matrix4f().identity().rotateZ((float) Math.toRadians(t * 36f));
                hook.setTransformationMatrix(spin);
                hook.setInterpolationDelay(0);
                hook.setInterpolationDuration(1);

                hook.teleport(hook.getLocation().add(dir.clone().multiply(PROJECTILE_SPEED)));
                dist += PROJECTILE_SPEED;

                // Trail — thin chain of crit sparks + golden dust
                Location loc = hook.getLocation();
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.04, 0.04, 0.04, 0.02);
                if (t % 2 == 0) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(HOOK_COLOR, 0.85f));
                }

                // Block collision — check a small step ahead so we don't embed inside
                if (hook.getLocation().clone().add(dir.clone().multiply(0.5)).getBlock().isSolid()) {
                    latchHook(player, hook, hook.getLocation().clone(), dir.clone());
                    cancel();
                }

                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    private void latchHook(Player player, ItemDisplay hook, Location anchor, Vector hitDir) {
        // Stop spinning — sit flush with the wall
        hook.setTransformationMatrix(new Matrix4f().identity());
        hook.setInterpolationDelay(0);
        hook.setInterpolationDuration(2);

        // Impact burst
        anchor.getWorld().spawnParticle(Particle.CRIT, anchor, 18, 0.18, 0.18, 0.18, 0.12);
        anchor.getWorld().spawnParticle(Particle.DUST, anchor, 8, 0.12, 0.12, 0.12, 0,
            new Particle.DustOptions(HOOK_COLOR, 1.5f));
        anchor.getWorld().spawnParticle(Particle.FLASH, anchor, 1, 0, 0, 0, 0);
        anchor.getWorld().playSound(anchor, Sound.BLOCK_STONE_HIT, 1.2f, 0.75f);
        anchor.getWorld().playSound(anchor, Sound.ENTITY_FISHING_BOBBER_THROW, 0.5f, 0.5f);

        // Rope starts at current distance minus a bit — immediately applies tension
        Location playerCenter = player.getLocation().add(0, 0.9, 0);
        double initialDist = playerCenter.distance(anchor);
        // The block we're hooked into — cache its location for solid checks
        Location anchorBlock = anchor.clone().add(hitDir.clone().multiply(0.5));

        BukkitRunnable physicsLoop = new BukkitRunnable() {
            double ropeLen = Math.max(initialDist - 1.5, MIN_ROPE);
            int t = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !ACTIVE.containsKey(player.getUniqueId())) {
                    cancel(); return;
                }
                // Release if anchor block was broken
                if (!anchorBlock.getBlock().isSolid()) {
                    releaseGrapple(player);
                    cancel(); return;
                }
                // Safety timeout
                if (t > 400) {
                    releaseGrapple(player);
                    cancel(); return;
                }

                Location pCenter = player.getLocation().add(0, 0.9, 0);
                Vector toAnchor = anchor.clone().subtract(pCenter).toVector();
                double dist = toAnchor.length();

                // Reel in — rope shortens each tick (datapack-style winch)
                ropeLen = Math.max(ropeLen * REEL_RATE, MIN_ROPE);

                // Close enough — detach
                if (dist <= MIN_ROPE + 0.4) {
                    releaseGrapple(player);
                    cancel(); return;
                }

                // Spring force: one-directional tether — only pulls when stretched past rope length
                // This is the core mechanic from the datapack's collider_apply_spring_cable
                if (dist > ropeLen) {
                    double stretch = dist - ropeLen;
                    Vector force = toAnchor.normalize().multiply(SPRING_K * stretch);
                    Vector vel = player.getVelocity().add(force);
                    // Velocity cap — prevents the spring from launching at insane speed on first tick
                    if (vel.length() > MAX_SPEED) vel = vel.normalize().multiply(MAX_SPEED);
                    player.setVelocity(vel);
                }

                // --- Rope visual ---
                // Catenary sag: sin(πt) offset downward at each point, proportional to total length
                Location ropeStart = player.getLocation().add(0, 0.9, 0);
                Vector rope = anchor.clone().subtract(ropeStart).toVector();
                double ropeLen3d = rope.length();
                if (ropeLen3d > 0.2) {
                    Vector step = rope.clone().normalize().multiply(0.5);
                    int numPts = (int)(ropeLen3d / 0.5);
                    double sagAmt = ropeLen3d * 0.04; // sag increases with rope length
                    for (int i = 1; i <= numPts; i++) {
                        Location pt = ropeStart.clone().add(step.clone().multiply(i));
                        double sag = Math.sin(Math.PI * (double) i / numPts) * sagAmt;
                        pt.subtract(0, sag, 0);
                        // Alternate rope link types for a chain-like appearance
                        if (i % 2 == 0) {
                            pt.getWorld().spawnParticle(Particle.DUST, pt, 0, 0, 0, 0, 0,
                                new Particle.DustOptions(ROPE_COLOR, 0.65f));
                        } else {
                            pt.getWorld().spawnParticle(Particle.CRIT, pt, 0, 0, 0, 0, 0);
                        }
                    }
                }

                // Glow at anchor
                if (t % 5 == 0) {
                    anchor.getWorld().spawnParticle(Particle.END_ROD, anchor, 2, 0.06, 0.06, 0.06, 0.02);
                }

                t++;
            }
        };
        physicsLoop.runTaskTimer(Minerva.getInstance(), 0L, 1L);

        ACTIVE.put(player.getUniqueId(), new GrappleState(anchor, physicsLoop, hook));
    }

    public static void releaseGrapple(Player player) {
        GrappleState state = ACTIVE.remove(player.getUniqueId());
        if (state == null) return;
        state.release();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.7f, 1.5f);
    }

    public static boolean isGrappling(Player player) {
        return ACTIVE.containsKey(player.getUniqueId());
    }

    @Override public String getLevelDescription(int level) { return ""; }
    @Override public String toString() { return "grappleHook"; }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.TRIPWIRE_HOOK)
            .setName(TextContext.formatLegacy("&lGrapple Hook", false).color(TextColor.color(200, 175, 100)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Fires a hook that latches to any surface.", false),
                TextContext.formatLegacy("&7Automatically reels you in (range: &e30 blocks&7).", false),
                TextContext.formatLegacy("&7Recast or sneak to release early.", false)
            )).build();
    }
}
