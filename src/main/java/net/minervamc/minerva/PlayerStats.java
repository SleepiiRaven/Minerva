package net.minervamc.minerva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.skills.SkillTriggers;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.JsonUtils;
import org.bukkit.Bukkit;

public class PlayerStats {
    public static Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private static final Path STORAGE_FOLDER = Minerva.getInstance().getDataFolder().toPath().resolve("PlayerData");
    private final Path storage;
    private final UUID uuid;
    public SkillTriggers skillTriggers;
    private HeritageType heritage = HeritageType.NONE;
    private Skill skillRRR = Skills.DEFAULT;
    private Skill skillRLR = Skills.DEFAULT;
    private Skill skillRLL = Skills.DEFAULT;
    private Skill skillRRL = Skills.DEFAULT;
    private Skill passive = Skills.DEFAULT_PASSIVE;
    private boolean rrrActive = true;
    private boolean rlrActive = true;
    private boolean rllActive = true;
    private boolean rrlActive = true;
    private boolean passiveActive = true;
    private int rrrLevel = 1;
    private int rlrLevel = 1;
    private int rllLevel = 1;
    private int rrlLevel = 1;
    private int passiveLevel = 1;
    private int maxLevel = 1;
    private int maxPoints = 0;
    private int points = 0;

    public UUID getUuid() {
        return uuid;
    }
    public HeritageType getHeritage() { return heritage; }
    public Skill getSkillRRR() { return skillRRR; }
    public Skill getSkillRLR() { return skillRLR; }
    public Skill getSkillRLL() { return skillRLL; }
    public Skill getSkillRRL() { return skillRRL; }
    public Skill getPassive() { return passive; }
    public boolean getRRRActive() { return rrrActive; }
    public boolean getRLRActive() { return rlrActive; }
    public boolean getRLLActive() { return rllActive; }
    public boolean getRRLActive() { return rrlActive; }
    public boolean getPassiveActive() { return passiveActive; }
    public int getRRRLevel() { return rrrLevel; }
    public int getRLRLevel() { return rlrLevel; }
    public int getRLLLevel() { return rllLevel; }
    public int getRRLLevel() { return rrlLevel; }
    public int getPassiveLevel() { return passiveLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getPoints() { return points; }
    public int getMaxPoints() { return maxPoints; }
    public void setHeritage(HeritageType heritage) { this.heritage = heritage; }
    public void setSkillRRR(Skill skillRRR) { this.skillRRR = skillRRR; }
    public void setSkillRLR(Skill skillRLR) { this.skillRLR = skillRLR; }
    public void setSkillRLL(Skill skillRLL) { this.skillRLL = skillRLL; }
    public void setSkillRRL(Skill skillRRL) { this.skillRRL = skillRRL; }
    public void setPassive(Skill passive) { this.passive = passive; }
    public void setRRRActive(boolean active) { rrrActive = active; }
    public void setRLRActive(boolean active) { rlrActive = active; }
    public void setRLLActive(boolean active) { rllActive = active; }
    public void setRRLActive(boolean active) { rrlActive = active; }
    public void setPassiveActive(boolean active) { passiveActive = active; }
    public void setRRRLevel(int level) { rrrLevel = level; }
    public void setRLRLevel(int level) { rlrLevel = level; }
    public void setRLLLevel(int level) { rllLevel = level; }
    public void setRRLLevel(int level) { rrlLevel = level; }
    public void setPassiveLevel(int level) { passiveLevel = level; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }
    public void setPoints(int points) { this.points = points; }
    public void setMaxPoints(int points) { maxPoints = points; }

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.storage = STORAGE_FOLDER.resolve(uuid + ".json");
        this.skillTriggers = new SkillTriggers(Bukkit.getPlayer(uuid));
    }

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
                // print the error
                e.printStackTrace();
            }
            playerStats.put(uuid, data);
        }
        return data;
    }

    public void createJSON() throws IOException {
        if (!Files.exists(this.storage)) {
            if (!Files.exists(this.storage.getParent())) {
                Bukkit.getServer().getConsoleSender().sendMessage(Minerva.getInstance().getDataFolder() + "/PlayerData");
                Files.createDirectory(Paths.get(Minerva.getInstance().getDataFolder() + "/PlayerData"));
            }
            Files.createFile(this.storage);
        }
    }

    public static void saveAll() {
        playerStats.forEach((k, v) -> v.saveAndDelete());
    }

    public void save() {
        try {
            createJSON();
            Files.writeString(storage, JsonUtils.GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAndDelete() {
        save();
        playerStats.remove(uuid, this);
    }
}
