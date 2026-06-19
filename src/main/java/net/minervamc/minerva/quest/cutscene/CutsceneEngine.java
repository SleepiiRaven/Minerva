package net.minervamc.minerva.quest.cutscene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.npc.CutsceneActor;
import net.minervamc.minerva.quest.npc.NpcReplayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Owns all running cutscenes. Plays a {@link CameraPath} for a player together with any number of NPC actors
 * using ProtocolLib + Citizens. {@code {player}} skins/names resolve to the viewing player here, so the same
 * cutscene mirrors whoever is watching. Degrades gracefully: without ProtocolLib it becomes a timed wait.
 */
public class CutsceneEngine {
    private static final Map<UUID, CutsceneSession> sessions = new HashMap<>();

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    }

    public static void play(Player viewer, CameraPath path, List<CutsceneActor> actors, Runnable onComplete) {
        end(viewer.getUniqueId(), false);

        boolean playable = isAvailable() && path != null && !path.isEmpty()
                && path.getKeyframes().get(0).location != null
                && path.getKeyframes().get(0).location.getWorld() != null;
        if (!playable) {
            int delay = path == null ? 1 : path.totalDurationTicks();
            Bukkit.getScheduler().runTaskLater(Minerva.getInstance(), () -> {
                if (onComplete != null) onComplete.run();
            }, Math.max(1, delay));
            return;
        }

        List<NpcReplayer> replayers = new ArrayList<>();
        if (actors != null && Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            for (CutsceneActor actor : actors) {
                if (actor.hasRecording()) {
                    replayers.add(new NpcReplayer(actor.getRecording(),
                            actor.resolveName(viewer.getName()), actor.resolveSkin(viewer.getName())));
                }
            }
        }

        CutsceneSession session = new CutsceneSession(viewer, path, replayers, onComplete);
        sessions.put(viewer.getUniqueId(), session);
        session.start();
    }

    public static void end(UUID uuid, boolean completed) {
        CutsceneSession session = sessions.get(uuid);
        if (session != null) session.end(completed);
    }

    static void forget(UUID uuid) {
        sessions.remove(uuid);
    }

    public static boolean isInCutscene(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public static void endAll() {
        for (CutsceneSession session : new ArrayList<>(sessions.values())) {
            session.end(false);
        }
        sessions.clear();
    }
}
