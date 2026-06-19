package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Runs a console or player command, substituting {@code {player}}, then completes instantly. */
public class RunCommandStep extends QuestStep {
    private String command = "say {player} reached a step";
    private boolean asConsole = true;
    private transient boolean dispatched = false;

    @Override
    public StepType getType() {
        return StepType.RUN_COMMAND;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Run Command"), Material.COMMAND_BLOCK);
    }

    @Override
    public String getEditorTitle() {
        return "Run: /" + command;
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("As: " + (asConsole ? "console" : "player"));
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("command", command);
        o.addProperty("asConsole", asConsole);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("command")) command = o.get("command").getAsString();
        if (o.has("asConsole")) asConsole = o.get("asConsole").getAsBoolean();
    }

    @Override
    public void writeState(JsonObject o) {
        o.addProperty("dispatched", dispatched);
    }

    @Override
    public void readState(JsonObject o) {
        if (o.has("dispatched")) dispatched = o.get("dispatched").getAsBoolean();
    }

    @Override
    public void begin(StepContext ctx) {
        Player player = ctx.player();
        // Run next tick so we are not mid-advance when the command (possibly) advances the quest again.
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
            if (!player.isOnline()) return;
            // Guard against a logout between begin() and this task re-dispatching the command on resume.
            if (!dispatched) {
                String resolved = command.replace("{player}", player.getName());
                if (asConsole) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                } else {
                    Bukkit.dispatchCommand(player, resolved);
                }
                dispatched = true;
                net.minervamc.minerva.quest.QuestManager.saveProgress(player.getUniqueId());
            }
            ctx.complete();
        });
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Run Command"));
        menu.fillBorder();
        menu.setItem(12, ItemCreator.create(Component.text("Command: /" + command), Material.COMMAND_BLOCK),
                (p, e) -> ChatInput.await(p, "Enter the command (no leading /). Use {player} for the player's name:", input -> {
                    command = input.trim();
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                }));
        menu.setItem(14, ItemCreator.create(Component.text("Run as: " + (asConsole ? "Console" : "Player")), Material.LEVER),
                (p, e) -> {
                    asConsole = !asConsole;
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        menu.setBackButton(back);
        menu.open(admin);
    }
}
