package net.minervamc.minerva.lib.region;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author BillyGalbreath
 */
@Getter
@SuppressWarnings("unused")
public class Region2d {
    private final String name;
    private final World world;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    public Region2d(String name, Location loc1, Location loc2) {
        this(name, loc1.getWorld(), loc1.getBlockX(), loc1.getBlockZ(), loc2.getBlockX(), loc2.getBlockZ());
        if(!loc1.getWorld().equals(loc2.getWorld())){
            throw new RuntimeException("Attempted to register a region in two different worlds");
        }
    }

    public Region2d(String name, World world, int x1, int z1, int x2, int z2) {
        this.name = name;
        this.world = world;

        minX = Math.min(x1, x2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxZ = Math.max(z1, z2);
    }

    public Region2d(ConfigurationSection section) {
        this.name = section.getString("name");
        if (this.name == null || this.name.isEmpty()) {
            throw new IllegalArgumentException("Region name is missing or empty in the configuration section");
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.isEmpty()) {
            throw new IllegalArgumentException("World name is missing or empty in the configuration section");
        }

        this.world = Bukkit.getWorld(worldName);
        if (this.world == null) {
            throw new IllegalArgumentException("World '" + worldName + "' does not exist");
        }

        if (!section.isSet("minX") || !section.isSet("maxX") || !section.isSet("minZ") || !section.isSet("maxZ")) {
            throw new IllegalArgumentException("Region coordinates (minX, maxX, minZ, maxZ) are missing in the configuration section");
        }

        this.minX = section.getInt("minX");
        this.maxX = section.getInt("maxX");
        this.minZ = section.getInt("minZ");
        this.maxZ = section.getInt("maxZ");

        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("Invalid region coordinates: minX must be <= maxX, and minZ must be <= maxZ");
        }
    }


    public boolean contains(Region2d region) {
        return region.getWorld().equals(world) &&
                region.getMinX() >= minX && region.getMaxX() <= maxX &&
                region.getMinZ() >= minZ && region.getMaxZ() <= maxZ;
    }

    public boolean contains(Location location) {
        return contains(location.getBlockX(), location.getBlockZ());
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX &&
                z >= minZ && z <= maxZ;
    }

    public boolean overlaps(Region2d region) {
        return region.getWorld().equals(world) &&
                !(region.getMinX() > maxX || region.getMinZ() > maxZ ||
                        minZ > region.getMaxX() || minZ > region.getMaxZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Region2d other)) {
            return false;
        }
        return world.equals(other.world)
                && minX == other.minX
                && minZ == other.minZ
                && maxX == other.maxX
                && maxZ == other.maxZ;
    }

    @Override
    public String toString() {
        return "Region2d[world:" + world.getName() +
                ", minX:" + minX +
                ", minZ:" + minZ +
                ", maxX:" + maxX +
                ", maxZ:" + maxZ + "]";
    }
}