package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.utils.PlayerStatsAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Completes once the player holds {@code amount} items matching a template in their inventory. */
public class CollectItemStep extends QuestStep {
    private ItemStack template = new ItemStack(Material.WHEAT);
    private int amount = 8;

    @Override
    public StepType getType() {
        return StepType.COLLECT_ITEM;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Collect Item"), Material.HOPPER_MINECART);
    }

    @Override
    public String getEditorTitle() {
        return "Collect " + amount + "x " + template.getType().name().toLowerCase();
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Matches items similar to the template.");
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("template", PlayerStatsAdapter.itemStackArrayToBase64(new ItemStack[]{template}));
        o.addProperty("amount", amount);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("template") && !o.get("template").isJsonNull()) {
            try {
                ItemStack[] arr = PlayerStatsAdapter.itemStackArrayFromBase64(o.get("template").getAsString());
                if (arr.length > 0 && arr[0] != null) template = arr[0];
            } catch (IOException ignored) {
            }
        }
        if (o.has("amount")) amount = o.get("amount").getAsInt();
    }

    @Override
    public String statusLine(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) count += item.getAmount();
        }
        return "Collect " + template.getType().name().toLowerCase() + " (" + Math.min(count, amount) + "/" + amount + ")";
    }

    @Override
    public void begin(StepContext ctx) {
        // Check immediately in case the player already has the items, then rely on pickup/inventory events.
        check(ctx);
    }

    /** Called by the quest listener (next tick after a pickup/inventory change) and on begin. */
    public void check(StepContext ctx) {
        Player player = ctx.player();
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) count += item.getAmount();
        }
        if (count >= amount) {
            ctx.complete();
        } else {
            player.sendActionBar(Component.text(getEditorTitle() + "  (" + count + "/" + amount + ")"));
        }
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Collect Item"));
        menu.fillBorder();
        menu.setItem(11, ItemCreator.create(Component.text("Set template to main hand"), Material.HOPPER),
                (p, e) -> {
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        p.sendMessage(Component.text("Hold an item in your main hand first."));
                    } else {
                        template = hand.clone();
                        template.setAmount(1);
                        net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    }
                    openEditor(p, quest, back);
                });
        menu.setItem(13, template.clone());
        menu.setItem(15, ItemCreator.create(Component.text("Amount: " + amount), Material.PAPER),
                (p, e) -> ChatInput.await(p, "Enter how many to collect:", input -> {
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
