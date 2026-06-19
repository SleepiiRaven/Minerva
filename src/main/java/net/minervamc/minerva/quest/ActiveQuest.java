package net.minervamc.minerva.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.steps.QuestStep;
import net.minervamc.minerva.quest.steps.StepContext;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * One player's in-progress run of a {@link Quest}. Holds per-player copies of the quest's steps (so two
 * players never share counters) and the current step index. Advancing is push-driven by the steps.
 */
public class ActiveQuest {
    @Getter private final UUID owner;
    @Getter private final String questId;
    @Getter private final String questName;
    @Getter private final List<QuestStep> steps = new ArrayList<>();
    @Getter private int stepIndex;

    public ActiveQuest(UUID owner, Quest quest) {
        this(owner, quest, 0);
    }

    public ActiveQuest(UUID owner, Quest quest, int stepIndex) {
        this.owner = owner;
        this.questId = quest.getId();
        this.questName = quest.getName();
        for (QuestStep step : quest.getSteps()) {
            this.steps.add(step.copyDefinition());
        }
        this.stepIndex = Math.max(0, Math.min(stepIndex, this.steps.size()));
    }

    public QuestStep currentStep() {
        return (stepIndex >= 0 && stepIndex < steps.size()) ? steps.get(stepIndex) : null;
    }

    private StepContext context(Player player) {
        return new StepContext(player, this, Minerva.getInstance());
    }

    /** Begin (or re-arm on login) the current step and tell the player what to do. */
    public void beginCurrent() {
        Player player = Bukkit.getPlayer(owner);
        QuestStep step = currentStep();
        if (player == null || step == null) return;
        step.begin(context(player));
        player.sendMessage(Component.text("▶ ", NamedTextColor.GOLD)
                .append(Component.text(step.getEditorTitle(), NamedTextColor.YELLOW)));
    }

    /** Cleanup the current step + advance to the next one. Called by steps via {@link StepContext#complete()}. */
    public void advance() {
        Player player = Bukkit.getPlayer(owner);
        QuestStep current = currentStep();
        if (current != null && player != null) {
            current.cleanup(context(player));
        }
        stepIndex++;
        if (stepIndex >= steps.size()) {
            QuestManager.completeActive(owner, this);
            return;
        }
        QuestManager.saveProgress(owner);
        if (player != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        }
        beginCurrent();
    }

    /** Cleanup without advancing (logout / shutdown). */
    public void cleanupCurrent() {
        Player player = Bukkit.getPlayer(owner);
        QuestStep step = currentStep();
        if (step != null && player != null) {
            step.cleanup(context(player));
        }
    }
}
