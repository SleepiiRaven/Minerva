package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.utils.PlayerStatsAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Completes when the player right-clicks the bound NPC while carrying the required items (optionally consumed). */
public class DeliverItemStep extends QuestStep {
    private int npcId = -1;
    private ItemStack template = new ItemStack(Material.WHEAT);
    private int amount = 1;
    private boolean consume = true;

    public int getNpcId() {
        return npcId;
    }

    @Override
    public StepType getType() {
        return StepType.DELIVER_ITEM;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Deliver Item"), Material.CHEST_MINECART);
    }

    @Override
    public String getEditorTitle() {
        return "Deliver " + amount + "x " + template.getType().name().toLowerCase() + (npcId < 0 ? " (no NPC)" : " to #" + npcId);
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Consume on delivery: " + consume);
    }

    @Override
    public void writeJson(JsonObject o) {
        o.addProperty("npcId", npcId);
        o.addProperty("template", PlayerStatsAdapter.itemStackArrayToBase64(new ItemStack[]{template}));
        o.addProperty("amount", amount);
        o.addProperty("consume", consume);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("npcId")) npcId = o.get("npcId").getAsInt();
        if (o.has("template") && !o.get("template").isJsonNull()) {
            try {
                ItemStack[] arr = PlayerStatsAdapter.itemStackArrayFromBase64(o.get("template").getAsString());
                if (arr.length > 0 && arr[0] != null) template = arr[0];
            } catch (IOException ignored) {
            }
        }
        if (o.has("amount")) amount = o.get("amount").getAsInt();
        if (o.has("consume")) consume = o.get("consume").getAsBoolean();
    }

    @Override
    public void begin(StepContext ctx) {
        ctx.player().sendActionBar(Component.text("Bring " + amount + "x " + template.getType().name().toLowerCase() + " to the NPC."));
    }

    /** Called by the NPC listener when the player right-clicks an NPC. */
    public void handleDeliver(int clickedNpcId, StepContext ctx) {
        if (clickedNpcId != npcId) return;
        Player player = ctx.player();
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) count += item.getAmount();
        }
        if (count < amount) {
            player.sendMessage(Component.text("You need " + amount + "x " + template.getType().name().toLowerCase()
                    + " (" + count + "/" + amount + ").", NamedTextColor.RED));
            return;
        }
        if (consume) {
            ItemStack toRemove = template.clone();
            toRemove.setAmount(amount);
            player.getInventory().removeItem(toRemove);
        }
        ctx.complete();
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(36, Component.text("Edit: Deliver Item"));
        menu.fillBorder();
        menu.setItem(10, ItemCreator.create(Component.text(npcId < 0 ? "Bind to an NPC" : "Bound to NPC #" + npcId), Material.VILLAGER_SPAWN_EGG),
                (p, e) -> QuestManager.requestNpcBind(p, id -> {
                    npcId = id;
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    p.sendMessage(Component.text("Bound delivery target to NPC #" + id, NamedTextColor.GREEN));
                    openEditor(p, quest, back);
                }));
        menu.setItem(12, ItemCreator.create(Component.text("Set item to main hand"), Material.HOPPER),
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
        menu.setItem(14, ItemCreator.create(Component.text("Amount: " + amount), Material.PAPER),
                (p, e) -> ChatInput.await(p, "Enter how many to deliver:", input -> {
                    try {
                        amount = Math.max(1, Integer.parseInt(input.trim()));
                        net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(Component.text("Not a valid number."));
                    }
                    openEditor(p, quest, back);
                }));
        menu.setItem(16, ItemCreator.create(Component.text("Consume on delivery: " + consume), Material.LEVER),
                (p, e) -> {
                    consume = !consume;
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        menu.setBackButton(back);
        menu.open(admin);
    }
}
