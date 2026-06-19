package net.minervamc.minerva.quest.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.Minerva;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Admin "record my movement" mode. Captures the admin's location + facing + sneak every tick into a
 * {@link MovementRecording}. Mirrors {@code RegionManager.creatingRegions}: a transient per-admin map.
 * Feedback is a steady action bar (no flashing — the user is photosensitive).
 */
public class NpcRecorder {
    private static final Map<UUID, Session> sessions = new HashMap<>();

    private static final class Session {
        final MovementRecording recording;
        final BukkitTask task;

        Session(MovementRecording recording, BukkitTask task) {
            this.recording = recording;
            this.task = task;
        }
    }

    public static boolean isRecording(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public static void start(Player player) {
        if (isRecording(player)) return;
        MovementRecording recording = new MovementRecording(player.getWorld().getName());
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                recording.getFrames().add(MovementFrame.of(player.getLocation(), player.isSneaking()));
                player.sendActionBar(Component.text("● REC  frames=" + recording.getFrames().size()
                        + "  ·  left-click to stop", NamedTextColor.RED));
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        sessions.put(player.getUniqueId(), new Session(recording, task));
    }

    /** Stops recording and returns the captured path, or {@code null} if the admin was not recording. */
    public static MovementRecording stop(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return null;
        session.task.cancel();
        player.sendActionBar(Component.text("Recording stopped (" + session.recording.getFrames().size() + " frames).", NamedTextColor.GREEN));
        return session.recording;
    }

    public static void cancelAll() {
        for (Session session : sessions.values()) session.task.cancel();
        sessions.clear();
    }
}
