package net.minervamc.minerva.quest.npc;

import org.bukkit.Location;
import org.bukkit.World;

/** One tick of recorded admin movement. Stored compactly as CSV to keep recordings small. */
public class MovementFrame {
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final boolean sneaking;

    public MovementFrame(double x, double y, double z, float yaw, float pitch, boolean sneaking) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sneaking = sneaking;
    }

    public static MovementFrame of(Location loc, boolean sneaking) {
        return new MovementFrame(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), sneaking);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String toCsv() {
        return x + "," + y + "," + z + "," + yaw + "," + pitch + "," + (sneaking ? 1 : 0);
    }

    public static MovementFrame fromCsv(String csv) {
        String[] p = csv.split(",");
        return new MovementFrame(
                Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                Float.parseFloat(p[3]), Float.parseFloat(p[4]), p[5].equals("1"));
    }
}
