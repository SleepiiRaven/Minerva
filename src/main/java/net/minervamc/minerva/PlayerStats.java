package net.minervamc.minerva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import net.minervamc.minerva.skills.SkillTriggers;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.JsonUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerStats {
    public static Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private static final Path STORAGE_FOLDER = Minerva.getInstance().getDataFolder().toPath().resolve("PlayerData");
    private final Path storage;

    @Getter private final UUID uuid;
    public SkillTriggers skillTriggers;
    @Getter @Setter private boolean skillMode = true;

    @Setter @Getter private HeritageType heritage = HeritageType.NONE;
    @Setter @Getter private Skill skillRRR = Skills.DEFAULT;
    @Setter @Getter private Skill skillRLR = Skills.DEFAULT;
    @Setter @Getter private Skill skillRLL = Skills.DEFAULT;
    @Setter @Getter private Skill skillRRL = Skills.DEFAULT;
    @Setter @Getter private Skill passive = Skills.DEFAULT_PASSIVE;

    private boolean rrrActive = true;
    private boolean rlrActive = true;
    private boolean rllActive = true;
    private boolean rrlActive = true;

    @Setter private boolean passiveActive = true;

    private int rrrLevel = 1;
    private int rlrLevel = 1;
    private int rllLevel = 1;
    private int rrlLevel = 1;
    @Setter @Getter private int passiveLevel = 1;
    @Setter @Getter private int maxLevel = 1;
    @Setter @Getter private int maxPoints = 0;
    @Setter @Getter private int points = 0;

    public boolean getRRRActive() { return rrrActive; }
    public boolean getRLRActive() { return rlrActive; }
    public boolean getRLLActive() { return rllActive; }
    public boolean getRRLActive() { return rrlActive; }
    public boolean getPassiveActive() { return passiveActive; }
    public int getRRRLevel() { return rrrLevel; }
    public int getRLRLevel() { return rlrLevel; }
    public int getRLLLevel() { return rllLevel; }
    public int getRRLLevel() { return rrlLevel; }

    public void setRRRActive(boolean active) { rrrActive = active; }
    public void setRLRActive(boolean active) { rlrActive = active; }
    public void setRLLActive(boolean active) { rllActive = active; }
    public void setRRLActive(boolean active) { rrlActive = active; }

    public void setRRRLevel(int level) { rrrLevel = level; }
    public void setRLRLevel(int level) { rlrLevel = level; }
    public void setRLLLevel(int level) { rllLevel = level; }
    public void setRRLLevel(int level) { rrlLevel = level; }

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.storage = STORAGE_FOLDER.resolve(uuid + ".json");
        this.skillTriggers = new SkillTriggers(Bukkit.getPlayer(uuid));
    }

    public void toggleSkillMode() {
        skillMode = !skillMode;
    }

    //region JSON Stuff
    public static PlayerStats getStats(UUID uuid) {
        PlayerStats data = playerStats.getOrDefault(uuid, null);
        if (data == null) {
            Path path = STORAGE_FOLDER.resolve(uuid.toString() + ".json");
            if (!Files.exists(path)) {
                data = new PlayerStats(uuid);
                playerStats.put(uuid, data);
                return data;
            }
            try {
                // getting the data
                String json = Files.readString(path);
                data = JsonUtils.GSON.fromJson(json, PlayerStats.class);
            } catch (IOException e) {
                // throw an exception
                throw new RuntimeException("Failed to read from json file: " + uuid, e);
            }
            playerStats.put(uuid, data);
        }
        return data;
    }

    public static PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }

    public void createJSON() throws IOException {
        if (!Files.exists(this.storage)) {
            if (!Files.exists(this.storage.getParent())) {
                Files.createDirectory(Paths.get(Minerva.getInstance().getDataFolder() + "/PlayerData"));
            }
            Files.createFile(this.storage);
        }
    }

    public static void saveAll() {
        playerStats.values().forEach(PlayerStats::save);
    }

    public void save() {
        try {
            createJSON();
            Files.writeString(storage, JsonUtils.GSON.toJson(this));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save json file: " + uuid, e);
        }
    }
}
