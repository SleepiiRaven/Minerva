package net.minervamc.minerva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerStats {
    private static final Path STORAGE_FOLDER = Minerva.getInstance().getDataFolder().toPath().resolve("PlayerData");
    public static Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Path storage;

    //region Getters
    //region Stats
    @Getter private final UUID uuid;
    public SkillTriggers skillTriggers;
    public boolean skillMode = true;
    @Getter private HeritageType heritage = HeritageType.NONE;
    @Getter private Skill skillRRR = Skills.DEFAULT;
    @Getter private Skill skillRLR = Skills.DEFAULT;
    @Getter private Skill skillRLL = Skills.DEFAULT;
    @Getter private Skill skillRRL = Skills.DEFAULT;
    @Getter private Skill passive = Skills.DEFAULT_PASSIVE;
    private boolean rrrActive = true;
    private boolean rlrActive = true;
    private boolean rllActive = true;
    private boolean rrlActive = true;
    private boolean passiveActive = true;
    private int rrrLevel = 1;
    private int rlrLevel = 1;
    private int rllLevel = 1;
    private int rrlLevel = 1;
    @Getter
    @Setter
    private int passiveLevel = 1;
    @Getter
    @Setter
    private int maxLevel = 1;
    @Getter
    @Setter
    private int maxPoints = 0;
    @Getter
    @Setter
    private int points = 0;
    @Setter @Getter private Location logoutLoc;
    @Setter @Getter private String omegaTrail = "rainbow";
    @Getter
    private ItemStack[] inventory = new ItemStack[36];
    @Getter
    private ItemStack[] armor = new ItemStack[4];
    @Getter
    private ItemStack[] offhand = new ItemStack[1];
    @Setter @Getter private Map<String, Integer> stackingAbilities = new HashMap<>();
    @Setter @Getter private static Map<Player, List<Entity>> summoned = new HashMap<>();
    //endregion

    public PlayerStats(UUID uuid) {
        if (Bukkit.getPlayer(uuid) == null) {
            this.uuid = uuid;
            this.storage = null;
            this.skillTriggers = null;
            this.logoutLoc = null;
        } else {
            summoned.put(Bukkit.getPlayer(uuid), new ArrayList<>());
            this.uuid = uuid;
            this.storage = STORAGE_FOLDER.resolve(uuid + ".json");
            this.skillTriggers = new SkillTriggers(Bukkit.getPlayer(uuid));
            this.logoutLoc = Bukkit.getPlayer(uuid).getLocation();
        }
    }

    public static void summon(Player player, Entity entity) {
        summoned.get(player).add(entity);
    }

    public static void removeSummon(Player player, Entity entity) {
        summoned.get(player).remove(entity);
    }

    public static void removeAllSummons() {
        for (UUID uuid : playerStats.keySet()) {
            for (Entity entity : summoned.get(Bukkit.getPlayer(uuid))) {
                entity.remove();
            }
        }
    }

    public static boolean isSummoned(Player player, Entity entity) {
        if (summoned.get(player) == null) return false;
        return summoned.get(player).contains(entity);
    }

    public static Player whoSummonedMe(Entity entity) {
        for (Player player : summoned.keySet()) {
            if (summoned.get(player).contains(entity)) return player;
        }
        return null;
    }

    //region JSON Stuff
    public static PlayerStats getStats(UUID uuid) {
        if (Bukkit.getPlayer(uuid) == null) return null;

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

    //region Setters
    public void setHeritage(HeritageType heritage) {
        if (heritage == null) {
            this.heritage = HeritageType.NONE;
        } else {
            this.heritage = heritage;
        }
    }

    public void setSkillRRR(Skill skillRRR) {
        if (skillRRR == null) {
            this.skillRRR = Skills.DEFAULT;
        } else {
            this.skillRRR = skillRRR;
        }
    }

    public void setSkillRLR(Skill skillRLR) {
        if (skillRLR == null) {
            this.skillRLR = Skills.DEFAULT;
        } else {
            this.skillRLR = skillRLR;
        }
    }

    public void setSkillRLL(Skill skillRLL) {
        if (skillRLL == null) {
            this.skillRLL = Skills.DEFAULT;
        } else {
            this.skillRLL = skillRLL;
        }
    }

    public void setSkillRRL(Skill skillRRL) {
        if (skillRRL == null) {
            this.skillRRL = Skills.DEFAULT;
        } else {
            this.skillRRL = skillRRL;
        }
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

    public void setPassiveActive(boolean active) {
        passiveActive = active;
    }

    public boolean getPassiveActive() {
        return passiveActive;
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
    //endregion

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
