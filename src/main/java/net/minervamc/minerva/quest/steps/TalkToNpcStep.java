package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Completes when the player right-clicks the bound Citizens NPC. Optionally shows dialogue lines. */
public class TalkToNpcStep extends QuestStep {
    private int npcId = -1;
    private final List<String> dialogue = new ArrayList<>();

    public int getNpcId() {
        return npcId;
    }

    @Override
    public StepType getType() {
        return StepType.TALK_TO_NPC;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Talk to NPC"), Material.VILLAGER_SPAWN_EGG);
    }

    @Override
    public String getEditorTitle() {
        return npcId < 0 ? "Talk to (no NPC bound)" : "Talk to NPC #" + npcId;
    }

    @Override
    public List<String> getEditorLore() {
        return List.of(dialogue.size() + " dialogue line(s)");
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("npcId", npcId);
        JsonArray arr = new JsonArray();
        for (String line : dialogue) arr.add(line);
        o.add("dialogue", arr);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("npcId")) npcId = o.get("npcId").getAsInt();
        dialogue.clear();
        if (o.has("dialogue") && o.get("dialogue").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("dialogue")) dialogue.add(el.getAsString());
        }
    }

    @Override
    public void begin(StepContext ctx) {
        ctx.player().sendActionBar(Component.text("Talk to the marked NPC."));
    }

    /** Called by the NPC listener when the player right-clicks an NPC. */
    public void handleTalk(int clickedNpcId, StepContext ctx) {
        if (clickedNpcId != npcId) return;
        for (String line : dialogue) {
            ctx.player().sendMessage(TextContext.hexAndLegacy(line));
        }
        ctx.complete();
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Talk to NPC"));
        menu.fillBorder();
        menu.setItem(11, ItemCreator.create(Component.text(npcId < 0 ? "Bind to an NPC" : "Bound to NPC #" + npcId), Material.VILLAGER_SPAWN_EGG),
                (p, e) -> QuestManager.requestNpcBind(p, id -> {
                    npcId = id;
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    p.sendMessage(Component.text("Bound this step to NPC #" + id, NamedTextColor.GREEN));
                    openEditor(p, quest, back);
                }));
        menu.setItem(13, ItemCreator.create(Component.text("Add dialogue line"), Material.WRITABLE_BOOK),
                (p, e) -> ChatInput.await(p, "Enter a dialogue line (supports & color codes):", input -> {
                    dialogue.add(input);
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                }));
        menu.setItem(15, ItemCreator.create(Component.text("Clear dialogue (" + dialogue.size() + ")"), Material.BARRIER),
                (p, e) -> {
                    dialogue.clear();
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        menu.setBackButton(back);
        menu.open(admin);
    }
}
