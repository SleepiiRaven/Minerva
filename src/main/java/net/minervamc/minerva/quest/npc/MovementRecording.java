package net.minervamc.minerva.quest.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** A captured path: the world it was recorded in plus one {@link MovementFrame} per tick. */
public class MovementRecording {
    @Getter @Setter private String world;
    @Getter private final List<MovementFrame> frames = new ArrayList<>();

    public MovementRecording(String world) {
        this.world = world;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("world", world);
        JsonArray arr = new JsonArray();
        for (MovementFrame frame : frames) arr.add(frame.toCsv());
        o.add("frames", arr);
        return o;
    }

    public static MovementRecording fromJson(JsonObject o) {
        MovementRecording rec = new MovementRecording(o.has("world") ? o.get("world").getAsString() : null);
        if (o.has("frames") && o.get("frames").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("frames")) {
                rec.frames.add(MovementFrame.fromCsv(el.getAsString()));
            }
        }
        return rec;
    }
}
