package net.minervamc.minerva.minigames.ctf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.region.Region2d;
import net.minervamc.minerva.lib.storage.yaml.Config;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RegionManager {
    private static final HashMap<UUID, Location[]> creatingRegions = new HashMap<>();
    private static final HashMap<String, Region2d> savedRegions = new HashMap<>();
    private static final Config regionConfig = new Config("ctf/regions.yml");

    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    public static Region2d getRegion(String name) {
        return savedRegions.get(name);
    }

    public static void enterSelectMode(Player player) {
        UUID playerUUID = player.getUniqueId();
        if(creatingRegions.containsKey(playerUUID)) {
            creatingRegions.remove(playerUUID);
            return;
        }
        creatingRegions.put(playerUUID, new Location[2]);
        player.sendMessage(Component.text("Left click to set pos 1", NamedTextColor.DARK_GREEN)
                .appendNewline()
                .append(Component.text("Right click to set pos 2", NamedTextColor.DARK_GREEN))
                .appendNewline()
                .append(Component.text("Run \"/ctf region save <name>\" to save.", NamedTextColor.GRAY))
        );
    }

    public static boolean isSelectMode(Player player) {
        return creatingRegions.containsKey(player.getUniqueId());
    }

    public static List<String> listRegions() {
        return new ArrayList<>(savedRegions.keySet().stream().toList());
    }

    public static void setPos1(Player player, Location loc) {
        UUID playerUUID = player.getUniqueId();
        if (!creatingRegions.containsKey(playerUUID)) {
            creatingRegions.put(playerUUID, new Location[2]);
        }
        creatingRegions.get(playerUUID)[0] = loc;
    }

    public static void setPos2(Player player, Location loc) {
        UUID playerUUID = player.getUniqueId();
        if (!creatingRegions.containsKey(playerUUID)) {
            creatingRegions.put(playerUUID, new Location[2]);
        }
        creatingRegions.get(playerUUID)[1] = loc;
    }

    public static boolean isPos1set(Player player) {
        UUID playerUUID = player.getUniqueId();
        if(!creatingRegions.containsKey(playerUUID)) return false;
        return creatingRegions.get(playerUUID)[0] != null;
    }

    public static boolean isPos2set(Player player) {
        UUID playerUUID = player.getUniqueId();
        if(!creatingRegions.containsKey(playerUUID)) return false;
        return creatingRegions.get(playerUUID)[1] != null;
    }

    public static void saveRegion(Player player, String regionName) {
        if (!isPos1set(player) || !isPos2set(player)) {
            player.sendMessage(Component.text("Pos1 and Pos2 must be set first!"));
            return;
        }

        Location loc1 = creatingRegions.get(player.getUniqueId())[0];
        Location loc2 = creatingRegions.get(player.getUniqueId())[1];

        Region2d region = new Region2d(regionName, loc1, loc2);
        savedRegions.put(regionName, region);

        try {
            regionConfig.set("regions." + regionName + ".name", region.getName());
            regionConfig.set("regions." + regionName + ".world", region.getWorld().getName());
            regionConfig.set("regions." + regionName + ".minX", region.getMinX());
            regionConfig.set("regions." + regionName + ".maxX", region.getMaxX());
            regionConfig.set("regions." + regionName + ".minZ", region.getMinZ());
            regionConfig.set("regions." + regionName + ".maxZ", region.getMaxZ());
            regionConfig.save();
        } catch (Exception e){
            LOGGER.error("Error saving region '{}': {}", regionName, e.getMessage());
        } finally {
            player.sendMessage(Component.text("Region " + regionName + " saved!"));
            creatingRegions.remove(player.getUniqueId());
        }
    }

    public static void saveRegionsToFile() {
        for (String regionName : savedRegions.keySet()) {
            Region2d region = savedRegions.get(regionName);
            regionConfig.set("regions." + regionName + ".name", region.getName());
            regionConfig.set("regions." + regionName + ".world", region.getWorld().getName());
            regionConfig.set("regions." + regionName + ".minX", region.getMinX());
            regionConfig.set("regions." + regionName + ".maxX", region.getMaxX());
            regionConfig.set("regions." + regionName + ".minZ", region.getMinZ());
            regionConfig.set("regions." + regionName + ".maxZ", region.getMaxZ());
        }
        regionConfig.save();
    }

    public static void loadRegionsFromFile() {
        ConfigurationSection regionsSection = regionConfig.getConfig().getConfigurationSection("regions");
        if (regionsSection == null) return;

        for (String regionName : regionsSection.getKeys(false)) {
            ConfigurationSection section = regionsSection.getConfigurationSection(regionName);

            if (section != null) {
                try {
                    Region2d region = new Region2d(section);
                    savedRegions.put(regionName, region);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Error loading region '{}': {}", regionName, e.getMessage());
                }
            }
        }
    }
}
