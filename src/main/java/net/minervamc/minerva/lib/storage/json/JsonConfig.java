package net.minervamc.minerva.lib.storage.json;

import com.google.gson.*;
import lombok.Getter;
import net.minervamc.minerva.Minerva;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class JsonConfig {

    private final String path;
    @Getter
    private JsonObject config;
    @Getter private File configFile;
    private final Gson gson;
    private final boolean relativeFromPluginsFolder;
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    public JsonConfig(String path, boolean relativeFromPluginsFolder) {
        this.path = path.endsWith(".json") ? path : path + ".json";
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.relativeFromPluginsFolder = relativeFromPluginsFolder;
        try {
            init();
        } catch (IOException e) {
            LOGGER.error("Creation of storage file at path {} failed!!", path);
            throw new RuntimeException(e);
        }
    }

    private void init() throws IOException {
        File configFile = relativeFromPluginsFolder ? new File(Minerva.getInstance().getDataFolder(), path) : new File(path);
        File parentFile = configFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) configFile.getParentFile().mkdirs();
        if (!configFile.exists()) configFile.createNewFile();

        this.configFile = configFile;

        try (FileReader reader = new FileReader(configFile)) {
            this.config = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            this.config = new JsonObject();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config at path: {}", path);
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        try (FileReader reader = new FileReader(configFile)) {
            this.config = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            LOGGER.error("Failed to reload config at path: {}", path);
            throw new RuntimeException(e);
        }
    }

    public void set(String path, JsonElement value) {
        String[] keys = path.split("\\.");
        JsonObject current = config;
        for (int i = 0; i < keys.length - 1; i++) {
            if (!current.has(keys[i]) || !current.get(keys[i]).isJsonObject()) {
                current.add(keys[i], new JsonObject());
            }
            current = current.getAsJsonObject(keys[i]);
        }
        current.add(keys[keys.length - 1], value);
    }

    public JsonElement get(String path) {
        String[] keys = path.split("\\.");
        JsonObject current = config;
        for (String key : keys) {
            if (current.has(key) && current.get(key).isJsonObject()) {
                current = current.getAsJsonObject(key);
            } else {
                return current.get(key);
            }
        }
        return current;
    }

    public <T> T get(String path, Class<T> type) {
        JsonElement element = get(path);
        return gson.fromJson(element, type);
    }

    public Set<String> getKeys(boolean deep) {
        return deep ? getKeysRecursive(config, "") : config.keySet();
    }

    private Set<String> getKeysRecursive(JsonObject jsonObject, String prefix) {
        Set<String> keys = new java.util.HashSet<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                keys.addAll(getKeysRecursive(entry.getValue().getAsJsonObject(), prefix + entry.getKey() + "."));
            } else {
                keys.add(prefix + entry.getKey());
            }
        }
        return keys;
    }

    public boolean contains(String path) {
        return get(path) != null;
    }
}
