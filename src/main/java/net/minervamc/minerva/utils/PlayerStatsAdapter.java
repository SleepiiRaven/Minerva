package net.minervamc.minerva.utils;

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
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
        object.addProperty("logoutLoc", locationToString(data.getLogoutLoc()));
        object.addProperty("inventory", itemStackArrayToBase64(data.getInventory()));
        object.addProperty("armor", itemStackArrayToBase64(data.getArmor()));
        object.addProperty("offhand", itemStackArrayToBase64(data.getOffhand()));
        object.addProperty("omegaTrail", data.getOmegaTrail());
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

            if (!object.has("uuid")) return null;
            PlayerStats playerDataJSON = new PlayerStats(UUID.fromString(object.get("uuid").getAsString()));
            if (!object.has("heritage")) playerDataJSON.setHeritage(null);
            else playerDataJSON.setHeritage(HeritageType.fromString(object.get("heritage").getAsString()));
            if (!object.has("skillRRR")) playerDataJSON.setSkillRRR(null);
            else playerDataJSON.setSkillRRR(Skill.fromString(object.get("skillRRR").getAsString()));
            if (!object.has("skillRLR")) playerDataJSON.setSkillRLR(null);
            else playerDataJSON.setSkillRLR(Skill.fromString(object.get("skillRLR").getAsString()));
            if (!object.has("skillRLL")) playerDataJSON.setSkillRLL(null);
            else playerDataJSON.setSkillRLL(Skill.fromString(object.get("skillRLL").getAsString()));
            if (!object.has("skillRRL")) playerDataJSON.setSkillRRL(null);
            else playerDataJSON.setSkillRRL(Skill.fromString(object.get("skillRRL").getAsString()));
            if (!object.has("passive")) playerDataJSON.setPassive(null);
            else playerDataJSON.setPassive(Skill.fromString(object.get("passive").getAsString()));
            if (!object.has("rrrActive")) playerDataJSON.setRRRActive(false);
            else playerDataJSON.setRRRActive(object.get("rrrActive").getAsBoolean());
            if (!object.has("rlrActive")) playerDataJSON.setRLRActive(false);
            else playerDataJSON.setRLRActive(object.get("rlrActive").getAsBoolean());
            if (!object.has("rllActive")) playerDataJSON.setRLLActive(false);
            else playerDataJSON.setRLLActive(object.get("rllActive").getAsBoolean());
            if (!object.has("rrlActive")) playerDataJSON.setRRLActive(false);
            else playerDataJSON.setRRLActive(object.get("rrlActive").getAsBoolean());
            if (!object.has("passiveActive")) playerDataJSON.setPassiveActive(false);
            else playerDataJSON.setPassiveActive(object.get("passiveActive").getAsBoolean());
            if (!object.has("rrrLevel")) playerDataJSON.setRRRLevel(1);
            else playerDataJSON.setRRRLevel(object.get("rrrLevel").getAsInt());
            if (!object.has("rlrLevel")) playerDataJSON.setRLRLevel(1);
            else playerDataJSON.setRLRLevel(object.get("rlrLevel").getAsInt());
            if (!object.has("rllLevel")) playerDataJSON.setRLLLevel(1);
            else playerDataJSON.setRLLLevel(object.get("rllLevel").getAsInt());
            if (!object.has("rrlLevel")) playerDataJSON.setRRLLevel(1);
            else playerDataJSON.setRRLLevel(object.get("rrlLevel").getAsInt());
            if (!object.has("passiveLevel")) playerDataJSON.setPassiveLevel(1);
            else playerDataJSON.setPassiveLevel(object.get("passiveLevel").getAsInt());
            if (!object.has("maxLevel")) playerDataJSON.setMaxLevel(1);
            else playerDataJSON.setMaxLevel(object.get("maxLevel").getAsInt());
            if (!object.has("points")) playerDataJSON.setPoints(0);
            else playerDataJSON.setPoints(object.get("points").getAsInt());
            if (!object.has("maxPoints")) playerDataJSON.setMaxPoints(0);
            else playerDataJSON.setMaxPoints(object.get("maxPoints").getAsInt());
            if (!object.has("logoutLoc")) playerDataJSON.setLogoutLoc(null);
            else playerDataJSON.setLogoutLoc(stringToLocation(object.get("logoutLoc").getAsString()));
            if (!object.has("omegaTrail")) playerDataJSON.setOmegaTrail("rainbow");
            else playerDataJSON.setOmegaTrail(object.get("omegaTrail").getAsString());
            try {
                if (object.has("inventory")) {
                    playerDataJSON.setInventory(itemStackArrayFromBase64(object.get("inventory").getAsString()));
                } else {
                    playerDataJSON.setInventory(new ItemStack[36]);
                }
                
                if (object.has("armor")) {
                    playerDataJSON.setArmor(itemStackArrayFromBase64(object.get("armor").getAsString()));
                } else {
                    playerDataJSON.setArmor(new ItemStack[4]);
                }

                if (object.has("offhand")) {
                    playerDataJSON.setOffhand(itemStackArrayFromBase64(object.get("offhand").getAsString()));
                } else {
                    playerDataJSON.setOffhand(new ItemStack[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return playerDataJSON;
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

    public static String locationToString(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.getWorld().getName() + "," +
                location.getX() + "," +
                location.getY() + "," +
                location.getZ() + "," +
                location.getYaw() + "," +
                location.getPitch();
    }

    public static Location stringToLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }
        String[] parts = locationString.split(",");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid location format.");
        }
        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }

}
