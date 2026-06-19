package net.minervamc.minerva.quest;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.minervamc.minerva.quest.steps.QuestStep;

/**
 * A quest DEFINITION. These are central and shared; they are never mutated while a player is running them
 * (each {@link ActiveQuest} holds its own deep-copied steps). Stored one file per quest under
 * {@code <dataFolder>/quests/<id>.json}.
 */
public class Quest {
    @Getter private final String id;
    @Getter @Setter private String name;
    @Getter @Setter private List<String> description = new ArrayList<>();
    @Getter @Setter private boolean repeatable = false;
    /** Quest ids that must be completed before this quest can start. */
    @Getter private final List<String> requiredQuests = new ArrayList<>();
    /** Player flags that must be present before this quest can start. */
    @Getter private final List<String> requiredFlags = new ArrayList<>();
    @Getter private final List<QuestStep> steps = new ArrayList<>();

    public Quest(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
