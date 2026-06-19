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
import net.minervamc.minerva.quest.storage.QuestStorage;
import net.minervamc.minerva.quest.steps.QuestStep;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Paged list of a quest's steps with reorder (shift-click) and delete (drop / Q) controls. */
public class StepListMenu extends Menu {
    private static final int PER_PAGE = 28;
    private final Quest quest;
    private final int page;

    public StepListMenu(Quest quest) {
        this(quest, 0);
    }

    public StepListMenu(Quest quest, int page) {
        super(54, Component.text("Steps: " + quest.getId()));
        this.quest = quest;
        this.page = Math.max(0, page);
        MenuUtil.addBorders(getInventory(), Material.BLACK_STAINED_GLASS_PANE);
        render();
    }

    @Override
    public void open(Player p) {
        QuestManager.setResumeScreen(p, quest.getId(), pl -> new StepListMenu(quest, page).open(pl));
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
        List<QuestStep> steps = quest.getSteps();
        int start = page * PER_PAGE;

        for (int i = 0; i < inner.length && start + i < steps.size(); i++) {
            final int index = start + i;
            QuestStep step = steps.get(index);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(step.getEditorTitle(), NamedTextColor.GRAY));
            for (String line : step.getEditorLore()) lore.add(Component.text(line, NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Left-click: edit", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift-left: move up", NamedTextColor.AQUA));
            lore.add(Component.text("Shift-right: move down", NamedTextColor.AQUA));
            lore.add(Component.text("Q / drop: delete", NamedTextColor.RED));

            ItemStack icon = ItemCreator.get(step.getIcon())
                    .setName(Component.text((index + 1) + ". " + step.getType().getDisplayName(), NamedTextColor.GOLD))
                    .setLore(lore).build();

            setItem(inner[i], icon, (p, e) -> {
                switch (e.getClick()) {
                    case SHIFT_LEFT -> {
                        if (index > 0) {
                            steps.add(index - 1, steps.remove(index));
                            QuestStorage.save(quest);
                        }
                        new StepListMenu(quest, page).open(p);
                    }
                    case SHIFT_RIGHT -> {
                        if (index < steps.size() - 1) {
                            steps.add(index + 1, steps.remove(index));
                            QuestStorage.save(quest);
                        }
                        new StepListMenu(quest, page).open(p);
                    }
                    case DROP, CONTROL_DROP -> {
                        steps.remove(index);
                        QuestStorage.save(quest);
                        new StepListMenu(quest, page).open(p);
                    }
                    default -> {
                        QuestManager.setResumeScreen(p, quest.getId(),
                                pl -> step.openEditor(pl, quest, () -> new StepListMenu(quest, page).open(pl)));
                        step.openEditor(p, quest, () -> new StepListMenu(quest, page).open(p));
                    }
                }
            });
        }

        if (page > 0) {
            setItem(45, ItemCreator.create(Component.text("← Previous page", NamedTextColor.YELLOW), Material.ARROW),
                    (p, e) -> new StepListMenu(quest, page - 1).open(p));
        }
        if (start + PER_PAGE < steps.size()) {
            setItem(53, ItemCreator.create(Component.text("Next page →", NamedTextColor.YELLOW), Material.ARROW),
                    (p, e) -> new StepListMenu(quest, page + 1).open(p));
        }

        setItem(48, ItemCreator.create(Component.text("← Back to quest", NamedTextColor.YELLOW), Material.BARRIER),
                (p, e) -> new QuestEditMenu(quest).open(p));

        setItem(50, ItemCreator.create(Component.text("+ Add Step", NamedTextColor.GREEN), Material.LIME_CONCRETE),
                (p, e) -> new StepTypePickerMenu(quest).open(p));
    }
}
