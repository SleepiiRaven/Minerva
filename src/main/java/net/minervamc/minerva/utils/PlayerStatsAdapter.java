package net.minervamc.minerva.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.storage.json.JsonConfig;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

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
        object.addProperty("maxLevel", data.getMaxLevel());
        object.addProperty("points", data.getPoints());
        object.addProperty("maxPoints", data.getMaxPoints());
        object.addProperty("inventory", itemStackArrayToBase64(data.getInventory()));
        object.addProperty("armor", itemStackArrayToBase64(data.getArmor()));
        object.addProperty("offhand", itemStackArrayToBase64(data.getOffhand()));
        return object;
    }

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.length);

            // Save every element in the list
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
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
            playerDataJSON.setMaxLevel(object.get("maxLevel").getAsInt());
            playerDataJSON.setPoints(object.get("points").getAsInt());
            playerDataJSON.setMaxPoints(object.get("maxPoints").getAsInt());

            try {
                String inv = object.get("inventory").getAsString();
                if (inv != null) {
                    playerDataJSON.setInventory(itemStackArrayFromBase64(inv));
                }
                String armor = object.get("armor").getAsString();
                if (armor != null) {
                    playerDataJSON.setArmor(itemStackArrayFromBase64(armor));
                }
                String offhand = object.get("offhand").getAsString();
                if (offhand != null) {
                    playerDataJSON.setOffhand(itemStackArrayFromBase64(offhand));
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // failed
            }
        }
        return null;
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
