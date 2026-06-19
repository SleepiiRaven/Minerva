package net.minervamc.minerva.quest.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Top-level builder screen: a paged list of every quest with edit/delete and a "new quest" button. */
public class QuestListMenu extends Menu {
    private static final int PER_PAGE = 28;
    private final int page;

    public QuestListMenu() {
        this(0);
    }

    public QuestListMenu(int page) {
        super(54, Component.text("Quest Builder"));
        this.page = Math.max(0, page);
        MenuUtil.addBorders(getInventory(), Material.BLACK_STAINED_GLASS_PANE);
        render();
    }

    @Override
    public void open(Player p) {
        QuestManager.clearResumeScreen(p); // the list is the top level
        super.open(p);
    }

    private static int[] innerSlots() {
        int[] slots = new int[PER_PAGE];
        int idx = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[idx++] = row * 9 + col;
            }
        }
        return slots;
    }

    private void render() {
        int[] inner = innerSlots();
        List<Quest> quests = new ArrayList<>(QuestManager.allQuests());
        int start = page * PER_PAGE;

        for (int i = 0; i < inner.length && start + i < quests.size(); i++) {
            Quest quest = quests.get(start + i);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("id: " + quest.getId(), NamedTextColor.DARK_GRAY));
            lore.add(Component.text(quest.getSteps().size() + " step(s)", NamedTextColor.GRAY));
            lore.add(Component.text("Prereqs: " + quest.getRequiredQuests().size() + " quest(s), "
                    + quest.getRequiredFlags().size() + " flag(s)", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Left-click: edit", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift-click: delete", NamedTextColor.RED));
            ItemCreator item = ItemCreator.get(Material.BOOK)
                    .setName(Component.text(quest.getName(), NamedTextColor.AQUA))
                    .setLore(lore);
            setItem(inner[i], item.build(), (p, e) -> {
                if (e.isShiftClick()) {
                    QuestManager.deleteQuest(quest.getId());
                    p.sendMessage(Component.text("Deleted quest " + quest.getId(), NamedTextColor.RED));
                    new QuestListMenu(page).open(p);
                } else {
                    new QuestEditMenu(quest).open(p);
                }
            });
        }

        if (page > 0) {
            setItem(45, ItemCreator.create(Component.text("← Previous page", NamedTextColor.YELLOW), Material.ARROW),
                    (p, e) -> new QuestListMenu(page - 1).open(p));
        }
        if (start + PER_PAGE < quests.size()) {
            setItem(53, ItemCreator.create(Component.text("Next page →", NamedTextColor.YELLOW), Material.ARROW),
                    (p, e) -> new QuestListMenu(page + 1).open(p));
        }
        int totalPages = Math.max(1, (quests.size() + PER_PAGE - 1) / PER_PAGE);
        setItem(47, ItemCreator.create(Component.text("Page " + (page + 1) + "/" + totalPages, NamedTextColor.GRAY), Material.PAPER));

        setItem(49, ItemCreator.create(Component.text("+ New Quest", NamedTextColor.GREEN), Material.LIME_CONCRETE),
                (p, e) -> ChatInput.await(p, "Enter a unique quest id (letters/numbers/underscores):", raw -> {
                    String id = raw.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    if (id.isEmpty()) {
                        p.sendMessage(Component.text("Invalid id.", NamedTextColor.RED));
                        new QuestListMenu(page).open(p);
                        return;
                    }
                    if (QuestManager.getQuest(id) != null) {
                        p.sendMessage(Component.text("A quest with that id already exists.", NamedTextColor.RED));
                        new QuestListMenu(page).open(p);
                        return;
                    }
                    Quest quest = QuestManager.createQuest(id, id);
                    new QuestEditMenu(quest).open(p);
                }));
    }
}
