package net.minervamc.minerva.quest.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.ActiveQuest;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.QuestProgress;
import net.minervamc.minerva.quest.steps.QuestStep;
import org.slf4j.Logger;

/** Per-player progress persistence at {@code <dataFolder>/QuestData/<uuid>.json}. Mirrors {@code PlayerStats}. */
public class QuestProgressStorage {
    private static final Path FOLDER = Minerva.getInstance().getDataFolder().toPath().resolve("QuestData");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();
    private static final Map<UUID, QuestProgress> cache = new HashMap<>();

    public static QuestProgress get(UUID uuid) {
        QuestProgress cached = cache.get(uuid);
        if (cached != null) return cached;

        QuestProgress progress;
        Path file = FOLDER.resolve(uuid + ".json");
        if (Files.exists(file)) {
            try {
                progress = deserialize(uuid, JsonParser.parseString(Files.readString(file)).getAsJsonObject());
            } catch (Exception e) {
                LOGGER.error("Failed to load quest progress for {}: {}", uuid, e.getMessage());
                progress = new QuestProgress(uuid);
            }
        } else {
            progress = new QuestProgress(uuid);
        }
        cache.put(uuid, progress);
        return progress;
    }

    public static void save(UUID uuid) {
        QuestProgress progress = cache.get(uuid);
        if (progress == null) return;
        try {
            Files.createDirectories(FOLDER);
            Files.writeString(FOLDER.resolve(uuid + ".json"), GSON.toJson(serialize(progress)));
        } catch (IOException e) {
            LOGGER.error("Failed to save quest progress for {}: {}", uuid, e.getMessage());
        }
    }

    public static void saveAll() {
        for (UUID uuid : cache.keySet()) save(uuid);
    }

    public static void unload(UUID uuid) {
        cache.remove(uuid);
    }

    private static JsonObject serialize(QuestProgress progress) {
        JsonObject o = new JsonObject();
        JsonArray completed = new JsonArray();
        for (String id : progress.getCompleted()) completed.add(id);
        o.add("completed", completed);

        JsonArray flags = new JsonArray();
        for (String flag : progress.getFlags()) flags.add(flag);
        o.add("flags", flags);

        JsonArray actives = new JsonArray();
        for (ActiveQuest active : progress.getActives()) {
            JsonObject ao = new JsonObject();
            ao.addProperty("questId", active.getQuestId());
            ao.addProperty("stepIndex", active.getStepIndex());
            QuestStep current = active.currentStep();
            if (current != null) {
                JsonObject state = new JsonObject();
                current.writeState(state);
                ao.add("state", state);
            }
            actives.add(ao);
        }
        o.add("active", actives);
        return o;
    }

    private static QuestProgress deserialize(UUID uuid, JsonObject o) {
        QuestProgress progress = new QuestProgress(uuid);
        if (o.has("completed") && o.get("completed").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("completed")) progress.getCompleted().add(el.getAsString());
        }
        if (o.has("flags") && o.get("flags").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("flags")) progress.getFlags().add(el.getAsString());
        }
        if (o.has("active")) {
            JsonElement active = o.get("active");
            if (active.isJsonArray()) {
                for (JsonElement el : active.getAsJsonArray()) {
                    if (el.isJsonObject()) readActive(uuid, progress, el.getAsJsonObject());
                }
            } else if (active.isJsonObject()) {
                // Backward compatibility with the old single-active format.
                readActive(uuid, progress, active.getAsJsonObject());
            }
        }
        return progress;
    }

    private static void readActive(UUID uuid, QuestProgress progress, JsonObject ao) {
        String questId = ao.has("questId") ? ao.get("questId").getAsString() : null;
        Quest quest = questId != null ? QuestManager.getQuest(questId) : null;
        if (quest == null) return;
        int stepIndex = ao.has("stepIndex") ? ao.get("stepIndex").getAsInt() : 0;
        ActiveQuest activeQuest = new ActiveQuest(uuid, quest, stepIndex);
        if (ao.has("state") && ao.get("state").isJsonObject() && activeQuest.currentStep() != null) {
            activeQuest.currentStep().readState(ao.getAsJsonObject("state"));
        }
        progress.addActive(activeQuest);
    }
}
