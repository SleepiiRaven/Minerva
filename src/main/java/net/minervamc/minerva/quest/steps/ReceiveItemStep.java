package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.utils.PlayerStatsAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Gives the player one or more items (a quest item or a reward), then completes instantly.
 * Tracks a "granted" flag so a resume after logout never duplicates the items.
 */
public class ReceiveItemStep extends QuestStep {
    private final List<ItemStack> items = new ArrayList<>();
    private transient boolean granted = false;

    @Override
    public StepType getType() {
        return StepType.RECEIVE_ITEM;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Receive Item / Reward"), Material.CHEST);
    }

    @Override
    public String getEditorTitle() {
        return "Receive " + items.size() + " item(s)";
    }

    @Override
    public List<String> getEditorLore() {
        List<String> lore = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) lore.add("- " + item.getAmount() + "x " + item.getType().name().toLowerCase());
        }
        return lore;
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("items", PlayerStatsAdapter.itemStackArrayToBase64(items.toArray(new ItemStack[0])));
    }

    @Override
    public void readJson(JsonObject o) {
        items.clear();
        if (o.has("items") && !o.get("items").isJsonNull()) {
            try {
                for (ItemStack item : PlayerStatsAdapter.itemStackArrayFromBase64(o.get("items").getAsString())) {
                    if (item != null) items.add(item);
                }
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void writeState(JsonObject o) {
        o.addProperty("granted", granted);
    }

    @Override
    public void readState(JsonObject o) {
        if (o.has("granted")) granted = o.get("granted").getAsBoolean();
    }

    @Override
    public void begin(StepContext ctx) {
        Player player = ctx.player();
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
            if (!player.isOnline()) return;
            if (!granted) {
                for (ItemStack item : items) {
                    if (item == null) continue;
                    player.getInventory().addItem(item.clone()).values()
                            .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
                }
                granted = true;
                net.minervamc.minerva.quest.QuestManager.saveProgress(player.getUniqueId());
                player.sendMessage(Component.text("You received quest items.", NamedTextColor.GREEN));
            }
            ctx.complete();
        });
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Receive Item"));
        menu.fillBorder();
        menu.setItem(11, ItemCreator.create(Component.text("Add item in main hand"), Material.HOPPER),
                (p, e) -> {
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        p.sendMessage(Component.text("Hold an item in your main hand first."));
                    } else {
                        items.add(hand.clone());
                        net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    }
                    openEditor(p, quest, back);
                });
        menu.setItem(15, ItemCreator.create(Component.text("Clear all items"), Material.BARRIER),
                (p, e) -> {
                    items.clear();
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        // Preview current items along the middle row.
        int slot = 0;
        for (ItemStack item : items) {
            if (slot >= 7) break;
            menu.setItem(slot == 0 ? 19 : 19 + slot, item.clone());
            slot++;
        }
        menu.setBackButton(back);
        menu.open(admin);
    }
}
