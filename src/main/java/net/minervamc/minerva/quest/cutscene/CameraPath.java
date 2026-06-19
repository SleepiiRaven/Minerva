package net.minervamc.minerva.quest.cutscene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** An ordered list of {@link CameraKeyframe}s describing a cutscene camera move. */
public class CameraPath {
    @Getter private final List<CameraKeyframe> keyframes = new ArrayList<>();

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public int totalDurationTicks() {
        int total = 0;
        for (CameraKeyframe kf : keyframes) total += Math.max(0, kf.durationTicks);
        return Math.max(1, total);
    }

    public JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (CameraKeyframe kf : keyframes) arr.add(kf.toJson());
        return arr;
    }

    public static CameraPath fromJson(JsonElement element) {
        CameraPath path = new CameraPath();
        if (element != null && element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonObject()) {
                    CameraKeyframe kf = CameraKeyframe.fromJson(el.getAsJsonObject());
                    if (kf.location != null) path.keyframes.add(kf);
                }
            }
        }
        return path;
    }
}
