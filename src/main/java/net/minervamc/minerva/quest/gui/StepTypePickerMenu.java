package net.minervamc.minerva.quest.gui;

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
import net.minervamc.minerva.quest.steps.StepType;
import org.bukkit.Material;

/** Pick a step type to append. Iterates {@link StepType#values()} so new step types appear automatically. */
public class StepTypePickerMenu extends Menu {
    private final Quest quest;

    public StepTypePickerMenu(Quest quest) {
        super(36, Component.text("Add Step"));
        this.quest = quest;
        MenuUtil.addBorders(getInventory(), Material.GRAY_STAINED_GLASS_PANE);
        render();
    }

    private void render() {
        int slot = 10;
        for (StepType type : StepType.values()) {
            if (slot % 9 == 8) slot += 2; // skip right/left border columns
            int placed = slot;
            setItem(placed, ItemCreator.get(type.getIcon())
                            .setName(Component.text(type.getDisplayName(), NamedTextColor.GOLD))
                            .setLore(List.of(Component.text("Click to add this step", NamedTextColor.YELLOW))).build(),
                    (p, e) -> {
                        QuestStep step = type.create();
                        quest.getSteps().add(step);
                        QuestStorage.save(quest);
                        QuestManager.setResumeScreen(p, quest.getId(),
                                pl -> step.openEditor(pl, quest, () -> new StepListMenu(quest).open(pl)));
                        step.openEditor(p, quest, () -> new StepListMenu(quest).open(p));
                    });
            slot++;
        }

        setItem(31, ItemCreator.create(Component.text("← Back", NamedTextColor.YELLOW), Material.ARROW),
                (p, e) -> new StepListMenu(quest).open(p));
    }
}
