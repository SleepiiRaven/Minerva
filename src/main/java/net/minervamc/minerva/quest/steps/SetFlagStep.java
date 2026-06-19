package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Sets or clears a player flag, then completes instantly. Other quests can require that flag to start. */
public class SetFlagStep extends QuestStep {
    private String flag = "my_flag";
    private boolean value = true; // true = set, false = clear

    @Override
    public StepType getType() {
        return StepType.SET_FLAG;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Set Flag"), Material.LEVER);
    }

    @Override
    public String getEditorTitle() {
        return (value ? "Set" : "Clear") + " flag: " + flag;
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Quests can require this flag as a prerequisite.");
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("flag", flag);
        o.addProperty("value", value);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("flag")) flag = o.get("flag").getAsString();
        if (o.has("value")) value = o.get("value").getAsBoolean();
    }

    @Override
    public void begin(StepContext ctx) {
        Player player = ctx.player();
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
            if (!player.isOnline()) return;
            QuestManager.setFlag(player, flag, value);
            ctx.complete();
        });
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Set Flag"));
        menu.fillBorder();
        menu.setItem(12, ItemCreator.create(Component.text("Flag: " + flag), Material.NAME_TAG),
                (p, e) -> ChatInput.await(p, "Enter the flag name:", input -> {
                    flag = input.trim();
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                }));
        menu.setItem(14, ItemCreator.create(Component.text("Action: " + (value ? "Set flag" : "Clear flag")), Material.LEVER),
                (p, e) -> {
                    value = !value;
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        menu.setBackButton(back);
        menu.open(admin);
    }
}
