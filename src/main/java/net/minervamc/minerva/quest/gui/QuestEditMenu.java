package net.minervamc.minerva.quest.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.storage.QuestStorage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Edit one quest: name, description, repeatable, prerequisites, NPC start command, and its steps. */
public class QuestEditMenu extends Menu {
    private final Quest quest;

    public QuestEditMenu(Quest quest) {
        super(27, Component.text("Edit Quest: " + quest.getId()));
        this.quest = quest;
        MenuUtil.addBorders(getInventory(), Material.GRAY_STAINED_GLASS_PANE);
        render();
    }

    @Override
    public void open(Player p) {
        QuestManager.setResumeScreen(p, quest.getId(), pl -> new QuestEditMenu(quest).open(pl));
        super.open(p);
    }

    private void render() {
        setItem(10, ItemCreator.get(Material.NAME_TAG)
                        .setName(Component.text("Name: " + quest.getName(), NamedTextColor.AQUA)).build(),
                (p, e) -> ChatInput.await(p, "Enter the quest display name:", input -> {
                    quest.setName(input);
                    QuestStorage.save(quest);
                    new QuestEditMenu(quest).open(p);
                }));

        List<Component> descLore = new ArrayList<>();
        for (String line : quest.getDescription()) descLore.add(Component.text("- " + line, NamedTextColor.GRAY));
        descLore.add(Component.empty());
        descLore.add(Component.text("Left-click: add line", NamedTextColor.YELLOW));
        descLore.add(Component.text("Shift-click: clear", NamedTextColor.RED));
        setItem(11, ItemCreator.get(Material.PAPER)
                        .setName(Component.text("Description (" + quest.getDescription().size() + " lines)", NamedTextColor.AQUA))
                        .setLore(descLore).build(),
                (p, e) -> {
                    if (e.isShiftClick()) {
                        quest.getDescription().clear();
                        QuestStorage.save(quest);
                        new QuestEditMenu(quest).open(p);
                    } else {
                        ChatInput.await(p, "Enter a description line to add:", input -> {
                            quest.getDescription().add(input);
                            QuestStorage.save(quest);
                            new QuestEditMenu(quest).open(p);
                        });
                    }
                });

        setItem(12, ItemCreator.create(Component.text("Repeatable: " + quest.isRepeatable(), NamedTextColor.AQUA), Material.LEVER),
                (p, e) -> {
                    quest.setRepeatable(!quest.isRepeatable());
                    QuestStorage.save(quest);
                    new QuestEditMenu(quest).open(p);
                });

        List<Component> reqQuestLore = new ArrayList<>();
        for (String q : quest.getRequiredQuests()) reqQuestLore.add(Component.text("- " + q, NamedTextColor.GRAY));
        reqQuestLore.add(Component.empty());
        reqQuestLore.add(Component.text("Must be completed before starting.", NamedTextColor.DARK_GRAY));
        reqQuestLore.add(Component.text("Left-click: add  ·  Shift-click: clear", NamedTextColor.YELLOW));
        setItem(13, ItemCreator.get(Material.BOOKSHELF)
                        .setName(Component.text("Required quests (" + quest.getRequiredQuests().size() + ")", NamedTextColor.AQUA))
                        .setLore(reqQuestLore).build(),
                (p, e) -> {
                    if (e.isShiftClick()) {
                        quest.getRequiredQuests().clear();
                        QuestStorage.save(quest);
                        new QuestEditMenu(quest).open(p);
                    } else {
                        ChatInput.await(p, "Enter a quest id that must be completed first:", input -> {
                            quest.getRequiredQuests().add(input.trim());
                            QuestStorage.save(quest);
                            new QuestEditMenu(quest).open(p);
                        });
                    }
                });

        List<Component> reqFlagLore = new ArrayList<>();
        for (String f : quest.getRequiredFlags()) reqFlagLore.add(Component.text("- " + f, NamedTextColor.GRAY));
        reqFlagLore.add(Component.empty());
        reqFlagLore.add(Component.text("Player must have these flags set.", NamedTextColor.DARK_GRAY));
        reqFlagLore.add(Component.text("Left-click: add  ·  Shift-click: clear", NamedTextColor.YELLOW));
        setItem(14, ItemCreator.get(Material.REDSTONE)
                        .setName(Component.text("Required flags (" + quest.getRequiredFlags().size() + ")", NamedTextColor.AQUA))
                        .setLore(reqFlagLore).build(),
                (p, e) -> {
                    if (e.isShiftClick()) {
                        quest.getRequiredFlags().clear();
                        QuestStorage.save(quest);
                        new QuestEditMenu(quest).open(p);
                    } else {
                        ChatInput.await(p, "Enter a flag that must be set first:", input -> {
                            quest.getRequiredFlags().add(input.trim());
                            QuestStorage.save(quest);
                            new QuestEditMenu(quest).open(p);
                        });
                    }
                });

        setItem(15, ItemCreator.get(Material.COMMAND_BLOCK)
                        .setName(Component.text("NPC start command", NamedTextColor.GOLD))
                        .setLore(List.of(
                                Component.text("/quests start " + quest.getId(), NamedTextColor.YELLOW),
                                Component.empty(),
                                Component.text("Players run this to start the quest.", NamedTextColor.GRAY),
                                Component.text("Click for the Citizens setup command.", NamedTextColor.DARK_GRAY))).build(),
                (p, e) -> {
                    p.closeInventory();
                    String npcCmd = "/npc command add quests start " + quest.getId();
                    p.sendMessage(Component.text("Select your quest NPC, then run (click to insert):", NamedTextColor.GOLD));
                    p.sendMessage(Component.text(npcCmd, NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand(npcCmd)));
                });

        setItem(16, ItemCreator.create(Component.text("Steps (" + quest.getSteps().size() + ")", NamedTextColor.GOLD), Material.WRITABLE_BOOK),
                (p, e) -> new StepListMenu(quest).open(p));

        setItem(18, ItemCreator.create(Component.text("← Back to list", NamedTextColor.YELLOW), Material.ARROW),
                (p, e) -> new QuestListMenu().open(p));

        setItem(26, ItemCreator.create(Component.text("Delete quest (shift-click)", NamedTextColor.RED), Material.BARRIER),
                (p, e) -> {
                    if (e.isShiftClick()) {
                        QuestManager.deleteQuest(quest.getId());
                        p.sendMessage(Component.text("Deleted quest " + quest.getId(), NamedTextColor.RED));
                        new QuestListMenu().open(p);
                    }
                });
    }
}
