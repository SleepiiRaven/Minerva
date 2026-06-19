package net.minervamc.minerva.quest.steps;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.ActiveQuest;
import org.bukkit.entity.Player;

/**
 * Everything a {@link QuestStep} needs at runtime: the player running the quest, their
 * {@link ActiveQuest}, and the plugin instance. A step signals it is finished by calling
 * {@link #complete()}, which advances the quest to the next step.
 */
public record StepContext(Player player, ActiveQuest active, Minerva plugin) {
    public void complete() {
        active.advance();
    }
}
