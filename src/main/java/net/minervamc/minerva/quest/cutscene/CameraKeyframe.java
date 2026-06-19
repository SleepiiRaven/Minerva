package net.minervamc.minerva.quest.cutscene;

import com.google.gson.JsonObject;
import net.minervamc.minerva.utils.PlayerStatsAdapter;
import org.bukkit.Location;

/**
 * One camera waypoint placed in-game. {@code durationTicks} is the time taken to travel from the PREVIOUS
 * keyframe to this one. {@code pan == true} smoothly interpolates that travel; {@code pan == false} is an
 * instant cut/switch.
 */
public class CameraKeyframe {
    public Location location;
    public int durationTicks;
    public boolean pan;

    public CameraKeyframe(Location location, int durationTicks, boolean pan) {
        this.location = location;
        this.durationTicks = durationTicks;
        this.pan = pan;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("location", PlayerStatsAdapter.locationToString(location));
        o.addProperty("durationTicks", durationTicks);
        o.addProperty("pan", pan);
        return o;
    }

    public static CameraKeyframe fromJson(JsonObject o) {
        Location loc = o.has("location") && !o.get("location").isJsonNull()
                ? PlayerStatsAdapter.stringToLocation(o.get("location").getAsString()) : null;
        int dur = o.has("durationTicks") ? o.get("durationTicks").getAsInt() : 40;
        boolean pan = !o.has("pan") || o.get("pan").getAsBoolean();
        return new CameraKeyframe(loc, dur, pan);
    }
}
