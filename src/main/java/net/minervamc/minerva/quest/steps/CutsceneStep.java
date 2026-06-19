package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.cutscene.CameraPath;
import net.minervamc.minerva.quest.cutscene.CutsceneEngine;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.quest.npc.CutsceneActor;
import net.minervamc.minerva.quest.npc.MovementRecording;
import net.minervamc.minerva.quest.storage.QuestStorage;
import net.minervamc.minerva.lib.util.ItemCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Plays a camera cutscene (keyframed pan / instant cut) plus any number of NPC actors (each a recorded
 * movement with its own name/skin), then completes. Camera keyframes and actor movement are recorded in-game.
 */
public class CutsceneStep extends QuestStep {
    @Getter private final CameraPath path = new CameraPath();
    @Getter private final List<CutsceneActor> actors = new ArrayList<>();
    @Getter @Setter private int defaultPanDuration = 40;

    @Override
    public StepType getType() {
        return StepType.CUTSCENE;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Cutscene"), Material.PAINTING);
    }

    @Override
    public String getEditorTitle() {
        return "Cutscene (" + path.getKeyframes().size() + " keyframes, " + actors.size() + " NPC(s))";
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Camera pan/cut + NPC replays.");
    }

    @Override
    public void writeJson(JsonObject o) {
        o.add("path", path.toJson());
        JsonArray arr = new JsonArray();
        for (CutsceneActor actor : actors) arr.add(actor.toJson());
        o.add("actors", arr);
        o.addProperty("defaultPanDuration", defaultPanDuration);
    }

    @Override
    public void readJson(JsonObject o) {
        path.getKeyframes().clear();
        if (o.has("path")) path.getKeyframes().addAll(CameraPath.fromJson(o.get("path")).getKeyframes());

        actors.clear();
        if (o.has("actors") && o.get("actors").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("actors")) {
                if (el.isJsonObject()) actors.add(CutsceneActor.fromJson(el.getAsJsonObject()));
            }
        } else if (o.has("recording") && o.get("recording").isJsonObject()) {
            // Migrate the old single-NPC format into one actor.
            CutsceneActor actor = new CutsceneActor();
            actor.setRecording(MovementRecording.fromJson(o.getAsJsonObject("recording")));
            if (o.has("npcName")) actor.setName(o.get("npcName").getAsString());
            if (o.has("npcSkin")) actor.setSkin(o.get("npcSkin").getAsString());
            actors.add(actor);
        }
        if (o.has("defaultPanDuration")) defaultPanDuration = o.get("defaultPanDuration").getAsInt();
    }

    @Override
    public void begin(StepContext ctx) {
        CutsceneEngine.play(ctx.player(), path, actors, ctx::complete);
    }

    @Override
    public void cleanup(StepContext ctx) {
        CutsceneEngine.end(ctx.player().getUniqueId(), false);
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(45, Component.text("Edit: Cutscene"));
        menu.fillBorder();

        menu.setItem(10, ItemCreator.get(Material.ENDER_EYE)
                        .setName(Component.text("Edit camera in-game (" + path.getKeyframes().size() + " keyframes)", NamedTextColor.GOLD))
                        .setLore(List.of(
                                Component.text("Click, then fly around and:", NamedTextColor.GRAY),
                                Component.text("Left-click = add pan keyframe", NamedTextColor.YELLOW),
                                Component.text("Right-click = add instant cut", NamedTextColor.YELLOW),
                                Component.text("Q (drop) = undo last keyframe", NamedTextColor.YELLOW),
                                Component.text("F or /quests creator record = finish", NamedTextColor.YELLOW))).build(),
                (p, e) -> QuestManager.beginCameraEdit(p, quest, this));

        menu.setItem(11, ItemCreator.create(Component.text("Clear keyframes (" + path.getKeyframes().size() + ")"), Material.BARRIER),
                (p, e) -> {
                    path.getKeyframes().clear();
                    QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        menu.setItem(12, ItemCreator.create(Component.text("Pan duration: " + defaultPanDuration + " ticks"), Material.CLOCK),
                (p, e) -> ChatInput.await(p, "Enter default pan duration in ticks (20 = 1s):", input -> {
                    try {
                        defaultPanDuration = Math.max(1, Integer.parseInt(input.trim()));
                        QuestStorage.save(quest);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(Component.text("Not a valid number."));
                    }
                    openEditor(p, quest, back);
                }));

        menu.setItem(22, ItemCreator.create(Component.text("▶ Preview cutscene"), Material.LIME_DYE),
                (p, e) -> {
                    p.closeInventory();
                    if (path.isEmpty()) {
                        p.sendMessage(Component.text("Add at least one keyframe first.", NamedTextColor.RED));
                        return;
                    }
                    CutsceneEngine.play(p, path, actors,
                            () -> p.sendMessage(Component.text("Preview complete.", NamedTextColor.GREEN)));
                });

        if (QuestManager.isCitizensEnabled()) {
            menu.setItem(15, ItemCreator.get(Material.PLAYER_HEAD)
                            .setName(Component.text("+ Add custom NPC", NamedTextColor.GREEN))
                            .setLore(List.of(Component.text("A custom-skinned actor you configure.", NamedTextColor.GRAY))).build(),
                    (p, e) -> {
                        CutsceneActor actor = new CutsceneActor();
                        actors.add(actor);
                        QuestStorage.save(quest);
                        openActorEditor(p, quest, actor, back);
                    });
            menu.setItem(16, ItemCreator.get(Material.PLAYER_HEAD)
                            .setName(Component.text("+ Add player stand-in", NamedTextColor.GREEN))
                            .setLore(List.of(
                                    Component.text("Auto-uses the viewing player's", NamedTextColor.GRAY),
                                    Component.text("skin and name — for a cutscene", NamedTextColor.GRAY),
                                    Component.text("that mirrors whoever's watching.", NamedTextColor.GRAY))).build(),
                    (p, e) -> {
                        CutsceneActor actor = new CutsceneActor();
                        actor.setSkin(CutsceneActor.SELF);
                        actor.setName(CutsceneActor.SELF);
                        actors.add(actor);
                        QuestStorage.save(quest);
                        openActorEditor(p, quest, actor, back);
                    });

            // One head per actor along the bottom inner row.
            int[] actorSlots = {28, 29, 30, 31, 32, 33, 34};
            for (int i = 0; i < actors.size() && i < actorSlots.length; i++) {
                CutsceneActor actor = actors.get(i);
                menu.setItem(actorSlots[i], ItemCreator.get(Material.PLAYER_HEAD)
                                .setName(Component.text((i + 1) + ". " + actor.getName(), NamedTextColor.AQUA))
                                .setLore(List.of(
                                        Component.text("Skin: " + actor.skinLabel(), NamedTextColor.GRAY),
                                        Component.text(actor.frameCount() + " frames", NamedTextColor.GRAY),
                                        Component.text("Left-click: edit", NamedTextColor.YELLOW),
                                        Component.text("Shift-click: delete", NamedTextColor.RED))).build(),
                        (p, e) -> {
                            if (e.isShiftClick()) {
                                actors.remove(actor);
                                QuestStorage.save(quest);
                                openEditor(p, quest, back);
                            } else {
                                openActorEditor(p, quest, actor, back);
                            }
                        });
            }
        } else {
            menu.setItem(16, ItemCreator.create(Component.text("NPC actors need Citizens", NamedTextColor.RED), Material.BARRIER));
        }

        menu.setBackButton(back, 40);
        menu.open(admin);
    }

    /** Per-actor editor: record movement, set skin (incl. the viewing player), name, or delete. */
    private void openActorEditor(Player admin, Quest quest, CutsceneActor actor, Runnable cutsceneBack) {
        Runnable backToCutscene = () -> openEditor(admin, quest, cutsceneBack);
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit NPC: " + actor.getName()));
        menu.fillBorder();

        menu.setItem(10, ItemCreator.create(Component.text("Record this NPC's movement (" + actor.frameCount() + " frames)"), Material.JUKEBOX),
                (p, e) -> {
                    QuestManager.beginNpcRecording(p, quest, this, actor);
                    p.closeInventory();
                    p.sendMessage(Component.text("Recording NPC movement. Walk the path, then left-click to stop.", NamedTextColor.YELLOW));
                });
        menu.setItem(11, ItemCreator.create(Component.text("Clear recording"), Material.RED_DYE),
                (p, e) -> {
                    actor.setRecording(null);
                    QuestStorage.save(quest);
                    openActorEditor(p, quest, actor, cutsceneBack);
                });
        menu.setItem(13, ItemCreator.create(Component.text("Skin: " + actor.skinLabel(), NamedTextColor.AQUA), Material.NAME_TAG),
                (p, e) -> ChatInput.await(p, "Enter a player name for the skin, '{player}' for the viewer, or 'none':", input -> {
                    String in = input.trim();
                    actor.setSkin(in.equalsIgnoreCase("none") ? "" : in);
                    QuestStorage.save(quest);
                    openActorEditor(p, quest, actor, cutsceneBack);
                }));
        menu.setItem(14, ItemCreator.create(Component.text("Use viewing player's skin"), Material.PLAYER_HEAD),
                (p, e) -> {
                    actor.setSkin(CutsceneActor.SELF);
                    QuestStorage.save(quest);
                    openActorEditor(p, quest, actor, cutsceneBack);
                });
        menu.setItem(15, ItemCreator.create(Component.text("Name: " + actor.getName(), NamedTextColor.AQUA), Material.OAK_SIGN),
                (p, e) -> ChatInput.await(p, "Enter the NPC name (or '{player}' for the viewer's name):", input -> {
                    actor.setName(input.trim().isEmpty() ? "Actor" : input.trim());
                    QuestStorage.save(quest);
                    openActorEditor(p, quest, actor, cutsceneBack);
                }));
        menu.setItem(16, ItemCreator.create(Component.text("Delete NPC (shift-click)", NamedTextColor.RED), Material.BARRIER),
                (p, e) -> {
                    if (e.isShiftClick()) {
                        actors.remove(actor);
                        QuestStorage.save(quest);
                        backToCutscene.run();
                    }
                });

        menu.setBackButton(backToCutscene, 22);
        menu.open(admin);
    }
}
