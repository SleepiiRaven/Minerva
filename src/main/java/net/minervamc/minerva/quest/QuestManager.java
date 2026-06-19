package net.minervamc.minerva.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.quest.cutscene.CameraKeyframe;
import net.minervamc.minerva.quest.cutscene.CutsceneEngine;
import net.minervamc.minerva.quest.gui.QuestListMenu;
import net.minervamc.minerva.quest.gui.StepListMenu;
import net.minervamc.minerva.quest.npc.CutsceneActor;
import net.minervamc.minerva.quest.npc.MovementRecording;
import net.minervamc.minerva.quest.npc.NpcRecorder;
import net.minervamc.minerva.quest.steps.CutsceneStep;
import net.minervamc.minerva.quest.storage.QuestProgressStorage;
import net.minervamc.minerva.quest.storage.QuestStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Central registry for quest definitions + entry point for starting/advancing/completing quests. Mirrors the
 * static-registry style of {@code RegionManager}/{@code CaptureTheFlag}. Also coordinates the in-game
 * authoring helpers (NPC binding, NPC movement recording, in-game camera keyframe editing) and remembers
 * which builder screen each admin was last on so {@code /quest} reopens it.
 */
public class QuestManager {
    private static final Map<String, Quest> quests = new HashMap<>();
    private static boolean citizensEnabled = false;
    private static boolean protocolEnabled = false;

    /** Admin -> callback awaiting the next NPC right-click (used by editors to bind NPCs). */
    private static final Map<UUID, IntConsumer> npcBindRequests = new HashMap<>();
    /** Admin -> cutscene step + actor currently capturing an NPC movement recording. */
    private static final Map<UUID, CutsceneStep> recordingStep = new HashMap<>();
    private static final Map<UUID, Quest> recordingQuest = new HashMap<>();
    private static final Map<UUID, CutsceneActor> recordingActor = new HashMap<>();
    /** Admin -> in-game camera keyframe editing session. */
    private static final Map<UUID, CameraEdit> cameraEdits = new HashMap<>();
    /** Admin -> "reopen my last builder screen" + the quest it belongs to (so /quest resumes it). */
    private static final Map<UUID, Consumer<Player>> resumeScreen = new HashMap<>();
    private static final Map<UUID, String> resumeQuestId = new HashMap<>();

    private static final class CameraEdit {
        final CutsceneStep step;
        final Quest quest;
        final boolean prevAllowFlight;
        final boolean prevFlying;
        int prevSlot;
        ItemStack prevItem;
        BukkitTask task;

        CameraEdit(CutsceneStep step, Quest quest, boolean prevAllowFlight, boolean prevFlying) {
            this.step = step;
            this.quest = quest;
            this.prevAllowFlight = prevAllowFlight;
            this.prevFlying = prevFlying;
        }
    }

    // ---- Capabilities ----
    public static void detectCapabilities() {
        citizensEnabled = Bukkit.getPluginManager().isPluginEnabled("Citizens");
        protocolEnabled = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    }

    public static boolean isCitizensEnabled() {
        return citizensEnabled;
    }

    public static boolean isProtocolEnabled() {
        return protocolEnabled;
    }

    // ---- Definitions ----
    public static void loadAll() {
        quests.clear();
        quests.putAll(QuestStorage.loadAll());
    }

    public static Quest getQuest(String id) {
        return quests.get(id);
    }

    public static Collection<Quest> allQuests() {
        return quests.values();
    }

    public static Quest createQuest(String id, String name) {
        Quest quest = new Quest(id, name);
        quests.put(id, quest);
        QuestStorage.save(quest);
        return quest;
    }

    public static void deleteQuest(String id) {
        quests.remove(id);
        QuestStorage.delete(id);
    }

    // ---- Progress ----
    public static QuestProgress progress(Player player) {
        return QuestProgressStorage.get(player.getUniqueId());
    }

    /** All of a player's currently active quests (a player may run several at once). */
    public static Collection<ActiveQuest> getActives(Player player) {
        return progress(player).getActives();
    }

    public static ActiveQuest getActive(Player player, String questId) {
        return progress(player).getActive(questId);
    }

    public static void saveProgress(UUID uuid) {
        QuestProgressStorage.save(uuid);
    }

    public static void startQuest(Player player, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            player.sendMessage(Component.text("Quest not found: " + questId, NamedTextColor.RED));
            return;
        }
        QuestProgress prog = progress(player);
        if (prog.hasActive(questId)) {
            player.sendMessage(Component.text("You are already on this quest.", NamedTextColor.RED));
            return;
        }
        if (prog.getCompleted().contains(questId) && !quest.isRepeatable()) {
            player.sendMessage(Component.text("You already completed this quest.", NamedTextColor.YELLOW));
            return;
        }
        List<String> missing = missingPrerequisites(player, quest);
        if (!missing.isEmpty()) {
            player.sendMessage(Component.text("You can't start this quest yet — needs: " + String.join(", ", missing), NamedTextColor.RED));
            return;
        }
        if (quest.getSteps().isEmpty()) {
            player.sendMessage(Component.text("This quest has no steps yet.", NamedTextColor.RED));
            return;
        }
        ActiveQuest active = new ActiveQuest(player.getUniqueId(), quest);
        prog.addActive(active);
        saveProgress(player.getUniqueId());
        player.sendMessage(Component.text("Quest started: ", NamedTextColor.GOLD)
                .append(Component.text(quest.getName(), NamedTextColor.YELLOW)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        active.beginCurrent();
    }

    // ---- Flags & prerequisites ----
    public static void setFlag(Player player, String flag, boolean value) {
        QuestProgress prog = progress(player);
        if (value) prog.getFlags().add(flag);
        else prog.getFlags().remove(flag);
        saveProgress(player.getUniqueId());
    }

    public static boolean meetsPrerequisites(Player player, Quest quest) {
        return missingPrerequisites(player, quest).isEmpty();
    }

    /** @return human-readable list of unmet prerequisites (empty if the quest can be started). */
    public static List<String> missingPrerequisites(Player player, Quest quest) {
        QuestProgress prog = progress(player);
        List<String> missing = new ArrayList<>();
        for (String q : quest.getRequiredQuests()) {
            if (!prog.getCompleted().contains(q)) missing.add("quest '" + q + "'");
        }
        for (String f : quest.getRequiredFlags()) {
            if (!prog.getFlags().contains(f)) missing.add("flag '" + f + "'");
        }
        return missing;
    }

    /** Called by {@link ActiveQuest#advance()} once the last step finishes. */
    public static void completeActive(UUID uuid, ActiveQuest active) {
        QuestProgress prog = QuestProgressStorage.get(uuid);
        prog.getCompleted().add(active.getQuestId());
        prog.removeActive(active.getQuestId());
        saveProgress(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(Component.text("Quest complete: ", NamedTextColor.GREEN)
                    .append(Component.text(active.getQuestName(), NamedTextColor.AQUA)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        }
    }

    public static void resumeOnJoin(Player player) {
        for (ActiveQuest active : new ArrayList<>(progress(player).getActives())) {
            active.beginCurrent();
        }
    }

    public static void cleanupOnQuit(Player player) {
        UUID uuid = player.getUniqueId();
        for (ActiveQuest active : progress(player).getActives()) {
            active.cleanupCurrent();
        }
        saveProgress(uuid);
        CutsceneEngine.end(uuid, false);
        npcBindRequests.remove(uuid);
        if (NpcRecorder.isRecording(player)) NpcRecorder.stop(player);
        recordingStep.remove(uuid);
        recordingQuest.remove(uuid);
        recordingActor.remove(uuid);
        cancelCameraEdit(player);
        clearResumeScreen(player);
        QuestProgressStorage.unload(uuid);
    }

    // ---- NPC binding (editors) ----
    public static void requestNpcBind(Player admin, IntConsumer onBind) {
        npcBindRequests.put(admin.getUniqueId(), onBind);
        admin.closeInventory();
        admin.sendMessage(Component.text("Right-click an NPC to bind it to this step.", NamedTextColor.YELLOW));
    }

    public static boolean consumeNpcBind(Player admin, int npcId) {
        IntConsumer callback = npcBindRequests.remove(admin.getUniqueId());
        if (callback == null) return false;
        callback.accept(npcId);
        return true;
    }

    // ---- NPC movement recording (stopped with /quest record) ----
    public static void beginNpcRecording(Player admin, Quest quest, CutsceneStep step, CutsceneActor actor) {
        recordingStep.put(admin.getUniqueId(), step);
        recordingQuest.put(admin.getUniqueId(), quest);
        recordingActor.put(admin.getUniqueId(), actor);
        NpcRecorder.start(admin);
    }

    public static boolean isRecordingNpc(Player admin) {
        return recordingStep.containsKey(admin.getUniqueId());
    }

    public static void finishNpcRecording(Player admin) {
        CutsceneStep step = recordingStep.remove(admin.getUniqueId());
        Quest quest = recordingQuest.remove(admin.getUniqueId());
        CutsceneActor actor = recordingActor.remove(admin.getUniqueId());
        MovementRecording recording = NpcRecorder.stop(admin);
        if (recording != null && actor != null) actor.setRecording(recording);
        if (quest != null) {
            QuestStorage.save(quest);
            if (step != null) step.openEditor(admin, quest, () -> new StepListMenu(quest).open(admin));
        }
    }

    // ---- In-game camera keyframe editing (gesture-driven, using a held Camera Tool) ----
    public static void beginCameraEdit(Player admin, Quest quest, CutsceneStep step) {
        CameraEdit edit = new CameraEdit(step, quest, admin.getAllowFlight(), admin.isFlying());
        // Give a Camera Tool in the held slot (and remember the original) so Q-undo / F-finish always work,
        // and lock the slot via the held-item listener so the tool stays in hand.
        edit.prevSlot = admin.getInventory().getHeldItemSlot();
        ItemStack prev = admin.getInventory().getItem(edit.prevSlot);
        edit.prevItem = prev == null ? null : prev.clone();
        admin.getInventory().setItem(edit.prevSlot, cameraTool());

        cameraEdits.put(admin.getUniqueId(), edit);
        admin.setAllowFlight(true);
        admin.closeInventory();
        admin.sendMessage(Component.text("Editing camera with the 🎬 Camera Tool. Fly around and:", NamedTextColor.GOLD));
        admin.sendMessage(Component.text("  Left-click", NamedTextColor.YELLOW).append(Component.text(" = add pan keyframe", NamedTextColor.GRAY)));
        admin.sendMessage(Component.text("  Right-click", NamedTextColor.YELLOW).append(Component.text(" = add instant cut", NamedTextColor.GRAY)));
        admin.sendMessage(Component.text("  Q (drop)", NamedTextColor.YELLOW).append(Component.text(" = undo last keyframe", NamedTextColor.GRAY)));
        admin.sendMessage(Component.text("  F (swap hands) or /quests creator record", NamedTextColor.YELLOW).append(Component.text(" = finish", NamedTextColor.GRAY)));
        // Steady action bar so the live keyframe count is always visible (refreshed, never flashing).
        edit.task = Bukkit.getScheduler().runTaskTimer(Minerva.getInstance(),
                () -> {
                    if (!admin.isOnline() || !isEditingCamera(admin)) return;
                    admin.sendActionBar(Component.text("🎬 Camera edit · "
                            + edit.step.getPath().getKeyframes().size() + " keyframes  ·  Q=undo · F=finish",
                            NamedTextColor.AQUA));
                }, 0L, 10L);
    }

    private static ItemStack cameraTool() {
        return ItemCreator.get(Material.BLAZE_ROD)
                .setName(Component.text("🎬 Camera Tool", NamedTextColor.GOLD))
                .setLore(java.util.List.of(
                        Component.text("Left-click: add pan keyframe", NamedTextColor.YELLOW),
                        Component.text("Right-click: add instant cut", NamedTextColor.YELLOW),
                        Component.text("Q (drop): undo last keyframe", NamedTextColor.YELLOW),
                        Component.text("F or /quests creator record: finish", NamedTextColor.YELLOW)))
                .build();
    }

    public static boolean isEditingCamera(Player admin) {
        return cameraEdits.containsKey(admin.getUniqueId());
    }

    public static void addPanKeyframe(Player admin) {
        CameraEdit edit = cameraEdits.get(admin.getUniqueId());
        if (edit == null) return;
        edit.step.getPath().getKeyframes().add(new CameraKeyframe(admin.getLocation().clone(), edit.step.getDefaultPanDuration(), true));
        QuestStorage.save(edit.quest);
        admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    public static void addSwitchKeyframe(Player admin) {
        CameraEdit edit = cameraEdits.get(admin.getUniqueId());
        if (edit == null) return;
        edit.step.getPath().getKeyframes().add(new CameraKeyframe(admin.getLocation().clone(), 1, false));
        QuestStorage.save(edit.quest);
        admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.8f);
    }

    public static void undoKeyframe(Player admin) {
        CameraEdit edit = cameraEdits.get(admin.getUniqueId());
        if (edit == null) return;
        var keyframes = edit.step.getPath().getKeyframes();
        if (keyframes.isEmpty()) {
            admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.6f);
            return;
        }
        keyframes.remove(keyframes.size() - 1);
        QuestStorage.save(edit.quest);
        admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
    }

    public static void finishCameraEdit(Player admin) {
        CameraEdit edit = cameraEdits.remove(admin.getUniqueId());
        if (edit == null) return;
        if (edit.task != null) edit.task.cancel();
        restoreState(admin, edit);
        QuestStorage.save(edit.quest);
        admin.sendActionBar(Component.text("Camera saved (" + edit.step.getPath().getKeyframes().size() + " keyframes).", NamedTextColor.GREEN));
        edit.step.openEditor(admin, edit.quest, () -> new StepListMenu(edit.quest).open(admin));
    }

    private static void cancelCameraEdit(Player admin) {
        CameraEdit edit = cameraEdits.remove(admin.getUniqueId());
        if (edit == null) return;
        if (edit.task != null) edit.task.cancel();
        if (admin.isOnline()) restoreState(admin, edit);
    }

    private static void restoreState(Player admin, CameraEdit edit) {
        // Give back the original held item.
        admin.getInventory().setItem(edit.prevSlot, edit.prevItem);
        // Restore flight.
        if (!edit.prevAllowFlight) {
            admin.setFlying(false);
            admin.setAllowFlight(false);
        } else {
            admin.setAllowFlight(true);
            admin.setFlying(edit.prevFlying);
        }
    }

    // ---- Sticky builder screen ----
    public static void setResumeScreen(Player player, String questId, Consumer<Player> open) {
        resumeScreen.put(player.getUniqueId(), open);
        if (questId != null) resumeQuestId.put(player.getUniqueId(), questId);
        else resumeQuestId.remove(player.getUniqueId());
    }

    public static void clearResumeScreen(Player player) {
        resumeScreen.remove(player.getUniqueId());
        resumeQuestId.remove(player.getUniqueId());
    }

    /** Opens the builder where the admin left off, or the quest list if there's no (still-valid) saved screen. */
    public static void openBuilder(Player player) {
        Consumer<Player> resume = resumeScreen.get(player.getUniqueId());
        String questId = resumeQuestId.get(player.getUniqueId());
        if (resume != null && (questId == null || getQuest(questId) != null)) {
            resume.accept(player);
            return;
        }
        clearResumeScreen(player);
        new QuestListMenu().open(player);
    }
}
