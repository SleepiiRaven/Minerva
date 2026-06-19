package net.minervamc.minerva.quest.cutscene;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.npc.NpcReplayer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * One running cutscene for one player. Puts the viewer in SPECTATOR (so the hotbar, held item, XP bar, health
 * and hunger HUD are hidden) and points their camera (ProtocolLib {@code CAMERA} packet) at an invisible
 * carrier entity, which we move along the {@link CameraPath}: a PAN keyframe smoothly interpolates the carrier,
 * a SWITCH keyframe respawns it for an instant cut. The camera is re-sent each tick so the lock survives a
 * stray sneak. {@link #end(boolean)} restores gamemode, position and view.
 */
public class CutsceneSession {
    private final Player viewer;
    private final CameraPath path;
    private final List<NpcReplayer> replayers;
    private final Runnable onComplete;

    private ArmorStand carrier;
    private BukkitTask task;
    private Location savedLoc;
    private GameMode savedGameMode;
    private boolean ended = false;

    private int segIndex = 1;
    private int tickInSeg = 0;

    public CutsceneSession(Player viewer, CameraPath path, List<NpcReplayer> replayers, Runnable onComplete) {
        this.viewer = viewer;
        this.path = path;
        this.replayers = replayers;
        this.onComplete = onComplete;
    }

    public void start() {
        savedLoc = viewer.getLocation().clone();
        savedGameMode = viewer.getGameMode();

        Location first = path.getKeyframes().get(0).location;
        if (first.getWorld() == null) {
            end(true);
            return;
        }

        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(first); // co-locate with the scene so the carrier entity is tracked (also handles other worlds)
        carrier = spawnCarrier(first);
        sendCamera(carrier.getEntityId());

        for (NpcReplayer replayer : replayers) replayer.start();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    private void tick() {
        if (ended || !viewer.isOnline() || carrier == null || carrier.isDead()) {
            end(false);
            return;
        }

        if (segIndex >= path.getKeyframes().size()) {
            end(true);
            return;
        }

        CameraKeyframe target = path.getKeyframes().get(segIndex);
        CameraKeyframe prev = path.getKeyframes().get(segIndex - 1);

        boolean differentWorld = prev.location.getWorld() == null
                || !prev.location.getWorld().equals(target.location.getWorld());
        if (!target.pan || differentWorld) {
            respawnCarrier(target.location); // instant cut
            segIndex++;
            tickInSeg = 0;
        } else {
            int dur = Math.max(1, target.durationTicks);
            double t = Math.min(1.0, (double) tickInSeg / dur);
            double e = t * t * (3 - 2 * t); // smoothstep ease
            carrier.teleport(interpolate(prev.location, target.location, e));
            tickInSeg++;
            if (tickInSeg > dur) {
                segIndex++;
                tickInSeg = 0;
            }
        }

        // Re-lock the spectator camera to the carrier (recovers if the player sneaked to detach).
        sendCamera(carrier.getEntityId());
    }

    /** Idempotent teardown. {@code completed} true only on natural end (runs the onComplete callback). */
    public void end(boolean completed) {
        if (ended) return;
        ended = true;

        if (task != null) {
            task.cancel();
            task = null;
        }
        if (viewer.isOnline()) {
            sendCamera(viewer.getEntityId());
            if (savedLoc != null) viewer.teleport(savedLoc);
            if (savedGameMode != null) viewer.setGameMode(savedGameMode);
        }
        if (carrier != null) {
            carrier.remove();
            carrier = null;
        }
        for (NpcReplayer replayer : replayers) replayer.stop();

        CutsceneEngine.forget(viewer.getUniqueId());

        if (completed && onComplete != null) {
            onComplete.run();
        }
    }

    private ArmorStand spawnCarrier(Location loc) {
        World world = loc.getWorld();
        return world.spawn(loc, ArmorStand.class, a -> {
            a.setVisible(false);
            a.setMarker(true);
            a.setGravity(false);
            a.setInvulnerable(true);
            a.setSilent(true);
            a.setCollidable(false);
            a.setPersistent(false);
        });
    }

    private void respawnCarrier(Location loc) {
        if (loc.getWorld() == null) return;
        if (carrier != null) carrier.remove();
        carrier = spawnCarrier(loc);
        sendCamera(carrier.getEntityId());
    }

    private Location interpolate(Location a, Location b, double t) {
        return new Location(a.getWorld(),
                lerp(a.getX(), b.getX(), t),
                lerp(a.getY(), b.getY(), t),
                lerp(a.getZ(), b.getZ(), t),
                lerpAngle(a.getYaw(), b.getYaw(), t),
                lerpAngle(a.getPitch(), b.getPitch(), t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static float lerpAngle(float a, float b, double t) {
        float delta = b - a;
        while (delta < -180f) delta += 360f;
        while (delta > 180f) delta -= 360f;
        return (float) (a + delta * t);
    }

    private void sendCamera(int entityId) {
        if (!viewer.isOnline()) return;
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = pm.createPacket(PacketType.Play.Server.CAMERA);
            packet.getIntegers().write(0, entityId);
            pm.sendServerPacket(viewer, packet);
        } catch (Exception ignored) {
            // ProtocolLib unavailable or send failed; player keeps their own view.
        }
    }
}
