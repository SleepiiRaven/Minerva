package net.minervamc.minerva.quest.listeners;

import java.util.ArrayList;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.ActiveQuest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.steps.CollectItemStep;
import net.minervamc.minerva.quest.steps.KillMobsStep;
import net.minervamc.minerva.quest.steps.StepContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Always-registered quest listener: lifecycle (join resume / quit cleanup), the non-Citizens progress events
 * (kills, item pickups), and the in-game camera keyframe editing gestures. Contains no Citizens types so it
 * loads even when Citizens is absent.
 */
public class QuestListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;
        // Resume next tick so PlayerStats (inventory/teleport) finishes first.
        Bukkit.getScheduler().runTask(Minerva.getInstance(), () -> {
            if (player.isOnline()) QuestManager.resumeOnJoin(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;
        QuestManager.cleanupOnQuit(player);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || killer.hasMetadata("NPC")) return;
        // Copy because a completing step advances and mutates the active-quest collection.
        for (ActiveQuest active : new ArrayList<>(QuestManager.getActives(killer))) {
            if (active.currentStep() instanceof KillMobsStep kill) {
                kill.handleKill(event, new StepContext(killer, active, Minerva.getInstance()));
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.hasMetadata("NPC")) return;
        if (QuestManager.getActives(player).isEmpty()) return;
        // Item is added to the inventory after this event; recount next tick across all active quests.
        Bukkit.getScheduler().runTask(Minerva.getInstance(), () -> {
            if (!player.isOnline()) return;
            for (ActiveQuest active : new ArrayList<>(QuestManager.getActives(player))) {
                if (active.currentStep() instanceof CollectItemStep collect) {
                    collect.check(new StepContext(player, active, Minerva.getInstance()));
                }
            }
        });
    }

    // ---- In-game camera keyframe editing gestures ----

    /** Left-click (arm swing): adds a pan keyframe while editing the camera, or stops NPC movement recording. */
    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (QuestManager.isEditingCamera(player)) {
            QuestManager.addPanKeyframe(player);
        } else if (QuestManager.isRecordingNpc(player)) {
            QuestManager.finishNpcRecording(player);
        }
    }

    /** Right-click adds an instant-cut keyframe; left-click on a block is swallowed (no mining). */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!QuestManager.isEditingCamera(player)) return;
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            QuestManager.addSwitchKeyframe(player);
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true); // pan keyframe handled by the arm-swing event
        }
    }

    /** Lock the hotbar slot while editing so the Camera Tool stays in hand. */
    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        if (QuestManager.isEditingCamera(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Q (drop the Camera Tool) undoes the last keyframe. */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (QuestManager.isEditingCamera(player)) {
            event.setCancelled(true);
            QuestManager.undoKeyframe(player);
        }
    }

    /** F (swap hands) finishes camera editing. */
    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (QuestManager.isEditingCamera(player)) {
            event.setCancelled(true);
            QuestManager.finishCameraEdit(player);
        }
    }

    /** Don't let the left-click controls (keyframe / stop recording) break blocks. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (QuestManager.isEditingCamera(player) || QuestManager.isRecordingNpc(player)) event.setCancelled(true);
    }
}
