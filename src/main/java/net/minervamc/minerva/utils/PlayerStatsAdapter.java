package net.minervamc.minerva.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.UUID;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;

public class PlayerStatsAdapter implements JsonSerializer<PlayerStats>, JsonDeserializer<PlayerStats> {
    @Override
    public JsonElement serialize(PlayerStats data, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("uuid", data.getUuid().toString());
        object.addProperty("heritage", HeritageType.toString(data.getHeritage()));
        object.addProperty("skillRRR", data.getSkillRRR().toString());
        object.addProperty("skillRLR", data.getSkillRLR().toString());
        object.addProperty("skillRLL", data.getSkillRLL().toString());
        object.addProperty("skillRRL", data.getSkillRRL().toString());
        object.addProperty("passive", data.getPassive().toString());
        object.addProperty("rrrActive", data.getRRRActive());
        object.addProperty("rlrActive", data.getRLRActive());
        object.addProperty("rllActive", data.getRLLActive());
        object.addProperty("rrlActive", data.getRRLActive());
        object.addProperty("passiveActive", data.getPassiveActive());
        object.addProperty("rrrLevel", data.getRRRLevel());
        object.addProperty("rlrLevel", data.getRLRLevel());
        object.addProperty("rllLevel", data.getRLLLevel());
        object.addProperty("rrlLevel", data.getRRLLevel());
        object.addProperty("passiveLevel", data.getPassiveLevel());
        object.addProperty("points", data.getPoints());
        object.addProperty("maxPoints", data.getMaxPoints());
        return object;
    }

    @Override
    public PlayerStats deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();

            PlayerStats playerDataJSON = new PlayerStats(UUID.fromString(object.get("uuid").getAsString()));
            playerDataJSON.setHeritage(HeritageType.fromString(object.get("heritage").getAsString()));
            playerDataJSON.setSkillRRR(Skill.fromString(object.get("skillRRR").getAsString()));
            playerDataJSON.setSkillRLR(Skill.fromString(object.get("skillRLR").getAsString()));
            playerDataJSON.setSkillRLL(Skill.fromString(object.get("skillRLL").getAsString()));
            playerDataJSON.setSkillRRL(Skill.fromString(object.get("skillRRL").getAsString()));
            playerDataJSON.setPassive(Skill.fromString(object.get("passive").getAsString()));
            playerDataJSON.setRRRActive(object.get("rrrActive").getAsBoolean());
            playerDataJSON.setRLRActive(object.get("rlrActive").getAsBoolean());
            playerDataJSON.setRLLActive(object.get("rllActive").getAsBoolean());
            playerDataJSON.setRRLActive(object.get("rrlActive").getAsBoolean());
            playerDataJSON.setPassiveActive(object.get("passiveActive").getAsBoolean());
            playerDataJSON.setRRRLevel(object.get("rrrLevel").getAsInt());
            playerDataJSON.setRLRLevel(object.get("rlrLevel").getAsInt());
            playerDataJSON.setRLLLevel(object.get("rllLevel").getAsInt());
            playerDataJSON.setRRLLevel(object.get("rrlLevel").getAsInt());
            playerDataJSON.setPassiveLevel(object.get("passiveLevel").getAsInt());
            playerDataJSON.setPoints(object.get("points").getAsInt());
            playerDataJSON.setMaxPoints(object.get("maxPoints").getAsInt());
            return playerDataJSON;
        }
        return null;
    }
}
