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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/** Completes when the player kills {@code amount} mobs of {@code mobType}. */
public class KillMobsStep extends QuestStep {
    private EntityType mobType = EntityType.ZOMBIE;
    private int amount = 5;
    private transient int killed = 0;

    @Override
    public StepType getType() {
        return StepType.KILL_MOBS;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Kill Mobs"), Material.DIAMOND_SWORD);
    }

    @Override
    public String getEditorTitle() {
        return "Kill " + amount + "x " + mobType.name().toLowerCase();
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Progress is tracked per player.");
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("mobType", mobType.name());
        o.addProperty("amount", amount);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("mobType")) {
            try {
                mobType = EntityType.valueOf(o.get("mobType").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (o.has("amount")) amount = o.get("amount").getAsInt();
    }

    @Override
    public void writeState(JsonObject o) {
        o.addProperty("killed", killed);
    }

    @Override
    public void readState(JsonObject o) {
        if (o.has("killed")) killed = o.get("killed").getAsInt();
    }

    @Override
    public String statusLine(Player player) {
        return "Kill " + mobType.name().toLowerCase() + " (" + Math.min(killed, amount) + "/" + amount + ")";
    }

    @Override
    public void begin(StepContext ctx) {
        // Purely event-driven; nothing to arm. The quest listener routes EntityDeathEvent here.
        sendProgress(ctx.player());
    }

    /** Called by the quest listener on every {@link EntityDeathEvent}. */
    public void handleKill(EntityDeathEvent event, StepContext ctx) {
        if (event.getEntityType() != mobType) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.getUniqueId().equals(ctx.player().getUniqueId())) return;
        killed++;
        QuestManager.saveProgress(ctx.player().getUniqueId());
        if (killed >= amount) {
            ctx.complete();
        } else {
            sendProgress(ctx.player());
        }
    }

    private void sendProgress(Player player) {
        player.sendActionBar(Component.text(getEditorTitle() + "  (" + Math.min(killed, amount) + "/" + amount + ")"));
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Kill Mobs"));
        menu.fillBorder();
        menu.setItem(11, ItemCreator.create(Component.text("Mob type: " + mobType.name()), Material.ZOMBIE_HEAD),
                (p, e) -> ChatInput.await(p, "Enter the mob type (e.g. ZOMBIE, SKELETON, BLAZE):", input -> {
                    try {
                        mobType = EntityType.valueOf(input.trim().toUpperCase());
                        net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage(Component.text("Unknown mob type."));
                    }
                    openEditor(p, quest, back);
                }));
        menu.setItem(15, ItemCreator.create(Component.text("Amount: " + amount), Material.PAPER),
                (p, e) -> ChatInput.await(p, "Enter how many to kill:", input -> {
                    try {
                        amount = Math.max(1, Integer.parseInt(input.trim()));
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
