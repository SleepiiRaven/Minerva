package net.minervamc.minerva.quest.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.SkinTrait;
import net.minervamc.minerva.Minerva;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Replays a {@link MovementRecording} on a Citizens NPC by teleporting it to each recorded frame once per
 * tick (the client interpolates between teleports, so it looks smooth). Mirrors the spawn/teleport/despawn
 * pattern in {@code MirrorImage}. Only instantiated when Citizens is present.
 */
public class NpcReplayer {
    private final MovementRecording recording;
    private final String name;
    private final String skin;
    private NPC npc;
    private BukkitTask task;

    public NpcReplayer(MovementRecording recording, String name, String skin) {
        this.recording = recording;
        this.name = (name == null || name.isBlank()) ? "NPC" : name;
        this.skin = skin;
    }

    public void start() {
        World world = Bukkit.getWorld(recording.getWorld());
        if (world == null || recording.isEmpty()) return;

        // Use the configured display name (with & colour support) for the nameplate, independent of the skin.
        String displayName = ChatColor.translateAlternateColorCodes('&', name);
        npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, displayName);
        npc.setName(displayName);
        if (skin != null && !skin.isBlank()) {
            npc.getOrAddTrait(SkinTrait.class).setSkinName(skin);
            npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
        }
        npc.getOrAddTrait(Gravity.class).setHasGravity(false);
        npc.setProtected(true);
        npc.spawn(recording.getFrames().get(0).toLocation(world));
        if (npc.getEntity() != null) npc.getEntity().addScoreboardTag("questReplay");

        task = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (npc == null || !npc.isSpawned()) {
                    cancel();
                    return;
                }
                if (i >= recording.getFrames().size()) {
                    // Path finished: leave the NPC standing at its final pose until the cutscene ends.
                    cancel();
                    return;
                }
                MovementFrame frame = recording.getFrames().get(i);
                npc.teleport(frame.toLocation(world), PlayerTeleportEvent.TeleportCause.PLUGIN);
                npc.setSneaking(frame.sneaking);
                i++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (npc != null) {
            if (npc.isSpawned()) npc.despawn();
            CitizensAPI.getNPCRegistry().deregister(npc);
            npc = null;
        }
    }
}
