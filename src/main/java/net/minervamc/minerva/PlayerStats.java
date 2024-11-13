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
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class PlayerStats {
    private static final Path STORAGE_FOLDER = Minerva.getInstance().getDataFolder().toPath().resolve("PlayerData");
    public static Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Path storage;

    //region Stats
    private final UUID uuid;
    public SkillTriggers skillTriggers;
    public boolean skillMode = true;
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
    @Setter @Getter private Location logoutLoc;
    private ItemStack[] inventory = new ItemStack[36];
    private ItemStack[] armor = new ItemStack[4];
    private ItemStack[] offhand = new ItemStack[1];
    //endregion

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.storage = STORAGE_FOLDER.resolve(uuid + ".json");
        this.skillTriggers = new SkillTriggers(Bukkit.getPlayer(uuid));
        this.logoutLoc = Bukkit.getPlayer(uuid).getLocation();
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
                // print the error
                e.printStackTrace();
            }
            playerStats.put(uuid, data);
        }
        if (data == null) {
            Minerva.getInstance().getSLF4JLogger().error("PlayerStats.java, somehow got past all the checks as null. UUID: " + uuid);
        }
        return data;
    }

    public static void saveAll() {
        for (PlayerStats stats : playerStats.values()) {
            stats.setLogoutLoc(Bukkit.getPlayer(stats.getUuid()).getLocation());
            stats.save();
        }
    }

    //region Getters
    public UUID getUuid() {
        return uuid;
    }

    public HeritageType getHeritage() {
        return heritage;
    }

    //region Setters
    public void setHeritage(HeritageType heritage) {
        if (heritage == null) {
            this.heritage = HeritageType.NONE;
        } else {
            this.heritage = heritage;
        }
    }

    public Skill getSkillRRR() {
        return skillRRR;
    }

    public void setSkillRRR(Skill skillRRR) {
        if (skillRRR == null) {
            this.skillRRR = Skills.DEFAULT;
        } else {
            this.skillRRR = skillRRR;
        }
    }

    public Skill getSkillRLR() {
        return skillRLR;
    }

    public void setSkillRLR(Skill skillRLR) {
        if (skillRLR == null) {
            this.skillRLR = Skills.DEFAULT;
        } else {
            this.skillRLR = skillRLR;
        }
    }

    public Skill getSkillRLL() {
        return skillRLL;
    }

    public void setSkillRLL(Skill skillRLL) {
        if (skillRLL == null) {
            this.skillRLL = Skills.DEFAULT;
        } else {
            this.skillRLL = skillRLL;
        }
    }

    public Skill getSkillRRL() {
        return skillRRL;
    }

    public void setSkillRRL(Skill skillRRL) {
        if (skillRRL == null) {
            this.skillRRL = Skills.DEFAULT;
        } else {
            this.skillRRL = skillRRL;
        }
    }

    public Skill getPassive() {
        return passive;
    }

    public void setPassive(Skill passive) {
        if (passive == null) {
            this.passive = Skills.DEFAULT;
        } else {
            this.passive = passive;
        }
    }

    public boolean getRRRActive() {
        return rrrActive;
    }

    public void setRRRActive(boolean active) {
        rrrActive = active;
    }

    public boolean getRLRActive() {
        return rlrActive;
    }

    public void setRLRActive(boolean active) {
        rlrActive = active;
    }
    //endregion

    public boolean getRLLActive() {
        return rllActive;
    }

    public void setRLLActive(boolean active) {
        rllActive = active;
    }

    public boolean getRRLActive() {
        return rrlActive;
    }

    public void setRRLActive(boolean active) {
        rrlActive = active;
    }

    public boolean getPassiveActive() {
        return passiveActive;
    }

    public void setPassiveActive(boolean active) {
        passiveActive = active;
    }

    public int getRRRLevel() {
        return rrrLevel;
    }

    public void setRRRLevel(int level) {
        rrrLevel = level;
    }

    public int getRLRLevel() {
        return rlrLevel;
    }

    public void setRLRLevel(int level) {
        rlrLevel = level;
    }

    public int getRLLLevel() {
        return rllLevel;
    }

    public void setRLLLevel(int level) {
        rllLevel = level;
    }

    public int getRRLLevel() {
        return rrlLevel;
    }

    public void setRRLLevel(int level) {
        rrlLevel = level;
    }

    public int getPassiveLevel() {
        return passiveLevel;
    }

    public void setPassiveLevel(int level) {
        passiveLevel = level;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int getPoints() {
        return points;
    }
    //endregion

    public void setPoints(int points) {
        this.points = points;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int points) {
        maxPoints = points;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public ItemStack[] getOffhand() {
        return offhand;
    }

    public void setInventory(ItemStack[] inventory) {
        if (inventory == null) {
            this.inventory = new ItemStack[36];
        } else {
            this.inventory = inventory;
        }
    }

    public void setArmor(ItemStack[] armor) {
        if (armor == null) {
            this.armor = new ItemStack[4];
        } else {
            this.armor = armor;
        }
    }

    public void setOffhand(ItemStack[] offhand) {
        if (offhand == null) {
            this.offhand = new ItemStack[1];
        } else {
            this.offhand = offhand;
        }
    }

    public void createJSON() throws IOException {
        if (!Files.exists(this.storage)) {
            if (!Files.exists(this.storage.getParent())) {
                Files.createDirectory(Paths.get(Minerva.getInstance().getDataFolder() + "/PlayerData"));
            }
            Files.createFile(this.storage);
        }
    }

    public void save() {
        try {
            createJSON();
            Files.writeString(storage, JsonUtils.GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //endregion
}
