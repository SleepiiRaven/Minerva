package net.minervamc.minerva.skills.cooldown;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class CooldownContainer implements ConfigurationSerializable {
    // A hashmap is a data structure which maps a key to a value. You can get the value by inputting the key
    private final Map<String, Long> cooldownMap = new HashMap<>();

    public CooldownContainer(Map<String, Object> map) {
        map.forEach((key, value) -> cooldownMap.put(key, (long) value));
    }

    public CooldownContainer() {
        this(new HashMap<>());
    }

    public void setCooldownFromNow(String name, long millis) {
        // System.currentTimeMillis() gets the current timestamp. so + millis is that many milliseconds in the future.
        cooldownMap.put(name, System.currentTimeMillis() + millis);
    }

    public long getCooldownLeft(String name) {
        return cooldownMap.getOrDefault(name, 0L) - System.currentTimeMillis();
    }

    public boolean isCooldownDone(String name) {
        return getCooldownLeft(name) <= 0;
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<>(cooldownMap);
    }
}
