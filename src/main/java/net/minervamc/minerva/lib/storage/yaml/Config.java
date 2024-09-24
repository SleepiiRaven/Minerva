package net.minervamc.minerva.lib.storage.yaml;

import lombok.Getter;
import net.minervamc.minerva.Minerva;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class Config {

    private final String path;
    @Getter private FileConfiguration config;
    @Getter private File configFile;
    private final File dataFolder;

    /**
     *
     * @param path The path relative to the plugin's data folder where the config file should be located.
     * If the path doesn't exist, it will be created.
     */
    public Config(String path) {
        this.path = path.endsWith(".yml") ? path : path + ".yml";
        this.dataFolder = Minerva.getInstance().getDataFolder();
        try {
            init();
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("Creation of storage file at path " + path + " failed!!");
            throw new RuntimeException(e);
        }
    }

    public void init() throws IOException {
        File configFile = new File(dataFolder, path);

        if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
        if (!configFile.exists()) configFile.createNewFile();

        this.configFile = configFile;
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("Failed to save config at path: " + path);
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getConsoleSender().sendMessage("Failed to reload config at path: " + path);
            throw new RuntimeException(e);
        }
    }

    public void set(String path, Object value) {
        config.set(path, value);
        save();
    }

    public void set(String path, Object value, boolean save) {
        config.set(path, value);
        if(save) save();
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return config.getConfigurationSection(path);
    }

    public File getParentFolder() {
        return configFile.getParentFile();
    }

    public String getRelativePath() {
        return configFile.getPath().replace("plugins" + File.separator + Minerva.getInstance().getName() + File.separator, "");
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String path, Class<T> type) {
        Object value = switch (type.getSimpleName()) {
            case "Integer" -> config.getInt(path);
            case "Long" -> config.getLong(path);
            case "Double" -> config.getDouble(path);
            case "Boolean" -> config.getBoolean(path);
            case "String" -> config.getString(path);
            case "List" -> config.getList(path);
            case "Float" -> (float) config.getDouble(path);
            case "Short" -> (short) config.getInt(path);
            case "Byte" -> (byte) config.getInt(path);
            case "Map" -> {
                ConfigurationSection section = config.getConfigurationSection(path);
                yield (section != null) ? section.getValues(false) : null;
            }
            default -> config.get(path);
        };
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getOrDefault(String path, Class<T> type, @NotNull T def) {
        Object value = switch (type.getSimpleName()) {
            case "Integer" -> config.getInt(path);
            case "Long" -> config.getLong(path);
            case "Double" -> config.getDouble(path);
            case "Boolean" -> config.getBoolean(path);
            case "String" -> config.getString(path);
            case "List" -> config.getList(path);
            case "Float" -> (float) config.getDouble(path);
            case "Short" -> (short) config.getInt(path);
            case "Byte" -> (byte) config.getInt(path);
            case "Map" -> {
                ConfigurationSection section = config.getConfigurationSection(path);
                yield (section != null) ? section.getValues(false) : def;
            }
            default -> config.get(path, def);
        };
        return value == null ? def : (T) value;
    }

    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

}
