package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/** Waits a fixed amount of time, then completes. */
public class WaitStep extends QuestStep {
    private int durationTicks = 60;
    private transient BukkitTask task;

    @Override
    public StepType getType() {
        return StepType.WAIT;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Wait / Delay"), Material.CLOCK);
    }

    @Override
    public String getEditorTitle() {
        return "Wait " + (durationTicks / 20.0) + "s";
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Pauses the quest for a set time.");
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("durationTicks", durationTicks);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("durationTicks")) durationTicks = o.get("durationTicks").getAsInt();
    }

    @Override
    public void begin(StepContext ctx) {
        task = ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), ctx::complete, durationTicks);
    }

    @Override
    public void cleanup(StepContext ctx) {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Wait"));
        menu.fillBorder();
        menu.setItem(13, ItemCreator.create(Component.text("Duration: " + (durationTicks / 20.0) + "s"), Material.CLOCK),
                (p, e) -> ChatInput.await(p, "Enter wait time in seconds (e.g. 3.5):", input -> {
                    try {
                        durationTicks = (int) Math.round(Double.parseDouble(input.trim()) * 20);
                        if (durationTicks < 1) durationTicks = 1;
                        net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(Component.text("Not a valid number."));
                    }
                    openEditor(p, quest, back);
                }));
        menu.setBackButton(back);
        menu.open(admin);
    }
}
