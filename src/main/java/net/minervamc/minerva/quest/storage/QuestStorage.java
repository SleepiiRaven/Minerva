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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.steps.QuestStep;
import net.minervamc.minerva.quest.steps.StepType;
import org.slf4j.Logger;

/** Loads/saves quest DEFINITIONS, one file per quest under {@code <dataFolder>/quests/<id>.json}. */
public class QuestStorage {
    private static final Path FOLDER = Minerva.getInstance().getDataFolder().toPath().resolve("quests");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    public static Map<String, Quest> loadAll() {
        Map<String, Quest> map = new HashMap<>();
        try {
            if (!Files.exists(FOLDER)) {
                Files.createDirectories(FOLDER);
                return map;
            }
            try (Stream<Path> stream = Files.list(FOLDER)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                    try {
                        JsonObject o = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                        Quest quest = deserialize(o);
                        if (quest != null) map.put(quest.getId(), quest);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load quest file {}: {}", path, e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list quest folder: {}", e.getMessage());
        }
        return map;
    }

    public static void save(Quest quest) {
        try {
            Files.createDirectories(FOLDER);
            Files.writeString(FOLDER.resolve(quest.getId() + ".json"), GSON.toJson(serialize(quest)));
        } catch (IOException e) {
            LOGGER.error("Failed to save quest {}: {}", quest.getId(), e.getMessage());
        }
    }

    public static void delete(String id) {
        try {
            Files.deleteIfExists(FOLDER.resolve(id + ".json"));
        } catch (IOException ignored) {
        }
    }

    public static JsonObject serialize(Quest quest) {
        JsonObject o = new JsonObject();
        o.addProperty("id", quest.getId());
        o.addProperty("name", quest.getName());
        JsonArray desc = new JsonArray();
        for (String line : quest.getDescription()) desc.add(line);
        o.add("description", desc);
        o.addProperty("repeatable", quest.isRepeatable());
        JsonArray reqQuests = new JsonArray();
        for (String q : quest.getRequiredQuests()) reqQuests.add(q);
        o.add("requiredQuests", reqQuests);
        JsonArray reqFlags = new JsonArray();
        for (String f : quest.getRequiredFlags()) reqFlags.add(f);
        o.add("requiredFlags", reqFlags);
        JsonArray steps = new JsonArray();
        for (QuestStep step : quest.getSteps()) {
            JsonObject so = new JsonObject();
            so.addProperty("type", step.getType().name());
            step.writeJson(so);
            steps.add(so);
        }
        o.add("steps", steps);
        return o;
    }

    public static Quest deserialize(JsonObject o) {
        if (!o.has("id")) return null;
        String id = o.get("id").getAsString();
        Quest quest = new Quest(id, o.has("name") ? o.get("name").getAsString() : id);
        if (o.has("description") && o.get("description").isJsonArray()) {
            List<String> desc = new ArrayList<>();
            for (JsonElement el : o.getAsJsonArray("description")) desc.add(el.getAsString());
            quest.setDescription(desc);
        }
        if (o.has("repeatable")) quest.setRepeatable(o.get("repeatable").getAsBoolean());
        if (o.has("requiredQuests") && o.get("requiredQuests").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("requiredQuests")) quest.getRequiredQuests().add(el.getAsString());
        }
        if (o.has("requiredFlags") && o.get("requiredFlags").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("requiredFlags")) quest.getRequiredFlags().add(el.getAsString());
        }
        if (o.has("steps") && o.get("steps").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("steps")) {
                if (!el.isJsonObject()) continue;
                JsonObject so = el.getAsJsonObject();
                if (!so.has("type")) continue;
                try {
                    StepType type = StepType.valueOf(so.get("type").getAsString());
                    QuestStep step = type.create();
                    step.readJson(so);
                    quest.getSteps().add(step);
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Skipping unknown step type {} in quest {}", so.get("type"), id);
                }
            }
        }
        return quest;
    }
}
