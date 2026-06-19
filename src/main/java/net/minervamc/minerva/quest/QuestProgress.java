package net.minervamc.minerva.quest;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

/** One player's persisted quest state: active quests, completed quest ids, and arbitrary flags. */
public class QuestProgress {
    @Getter private final UUID uuid;
    private final Map<String, ActiveQuest> actives = new LinkedHashMap<>();
    @Getter private final Set<String> completed = new HashSet<>();
    /** Arbitrary string flags a quest can set/clear and other quests can require as prerequisites. */
    @Getter private final Set<String> flags = new HashSet<>();

    public QuestProgress(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public Collection<ActiveQuest> getActives() {
        return actives.values();
    }

    public ActiveQuest getActive(String questId) {
        return actives.get(questId);
    }

    public boolean hasActive(String questId) {
        return actives.containsKey(questId);
    }

    public void addActive(ActiveQuest active) {
        actives.put(active.getQuestId(), active);
    }

    public void removeActive(String questId) {
        actives.remove(questId);
    }
}
