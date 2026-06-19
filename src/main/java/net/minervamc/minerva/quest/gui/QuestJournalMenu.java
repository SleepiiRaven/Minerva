package net.minervamc.minerva.quest.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import net.minervamc.minerva.quest.ActiveQuest;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.steps.QuestStep;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Read-only player journal: every active quest with its current step and live progress, plus completed count. */
public class QuestJournalMenu extends Menu {
    private final Player viewer;

    public QuestJournalMenu(Player viewer) {
        super(54, Component.text("My Quests"));
        this.viewer = viewer;
        MenuUtil.addBorders(getInventory(), Material.BLACK_STAINED_GLASS_PANE);
        render();
    }

    private void render() {
        List<ActiveQuest> actives = new ArrayList<>(QuestManager.getActives(viewer));

        if (actives.isEmpty()) {
            setItem(22, ItemCreator.get(Material.BARRIER)
                    .setName(Component.text("No active quests", NamedTextColor.RED))
                    .setLore(List.of(Component.text("Talk to a quest NPC to start one.", NamedTextColor.GRAY))).build());
        }

        int[] inner = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < actives.size() && i < inner.length; i++) {
            ActiveQuest active = actives.get(i);
            QuestStep current = active.currentStep();
            Quest def = QuestManager.getQuest(active.getQuestId());

            List<Component> lore = new ArrayList<>();
            if (def != null) {
                for (String line : def.getDescription()) lore.add(Component.text(line, NamedTextColor.GRAY));
                if (!def.getDescription().isEmpty()) lore.add(Component.empty());
            }
            lore.add(Component.text("Step " + (active.getStepIndex() + 1) + " of " + active.getSteps().size(), NamedTextColor.DARK_GRAY));
            lore.add(Component.text("→ " + (current == null ? "—" : current.statusLine(viewer)), NamedTextColor.YELLOW));

            setItem(inner[i], ItemCreator.get(Material.WRITTEN_BOOK)
                    .setName(Component.text(active.getQuestName(), NamedTextColor.AQUA))
                    .setLore(lore).build());
        }

        int completed = QuestManager.progress(viewer).getCompleted().size();
        setItem(49, ItemCreator.get(Material.EMERALD)
                .setName(Component.text("Completed quests: " + completed, NamedTextColor.GREEN)).build());
    }
}
