package net.minervamc.minerva.quest.npc;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * One NPC in a cutscene: a recorded movement plus a display name and skin. The skin/name may be the literal
 * {@code {player}}, which resolves at playback to the viewing player — so the same cutscene shows an NPC that
 * looks like whoever is watching it.
 */
public class CutsceneActor {
    public static final String SELF = "{player}";

    @Getter @Setter private String name = "Actor";
    /** A player name for a fixed skin, {@link #SELF} for the viewer's skin, or "" for the default. */
    @Getter @Setter private String skin = "";
    @Getter @Setter private MovementRecording recording;

    public CutsceneActor() {
    }

    public boolean hasRecording() {
        return recording != null && !recording.isEmpty();
    }

    public int frameCount() {
        return recording == null ? 0 : recording.getFrames().size();
    }

    public String resolveName(String viewerName) {
        return name.equalsIgnoreCase(SELF) ? viewerName : name;
    }

    public String resolveSkin(String viewerName) {
        return skin.equalsIgnoreCase(SELF) ? viewerName : skin;
    }

    public String skinLabel() {
        if (skin.equalsIgnoreCase(SELF)) return "viewing player";
        return skin.isBlank() ? "(default)" : skin;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("skin", skin);
        if (recording != null) o.add("recording", recording.toJson());
        return o;
    }

    public static CutsceneActor fromJson(JsonObject o) {
        CutsceneActor actor = new CutsceneActor();
        if (o.has("name")) actor.name = o.get("name").getAsString();
        if (o.has("skin")) actor.skin = o.get("skin").getAsString();
        if (o.has("recording") && o.get("recording").isJsonObject()) {
            actor.recording = MovementRecording.fromJson(o.getAsJsonObject("recording"));
        }
        return actor;
    }
}
