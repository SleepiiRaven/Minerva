package net.minervamc.minerva.skills.cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, CooldownContainer> cooldownContainerMap = new HashMap<>();

    public void createContainer(UUID playerId) {
        cooldownContainerMap.put(playerId, new CooldownContainer());
    }

    public void removeContainer(UUID playerId) {
        cooldownContainerMap.remove(playerId);
    }

    public CooldownContainer getContainer(UUID playerId) {
        if (cooldownContainerMap.getOrDefault(playerId, null) == null) {
            createContainer(playerId);
        }
        return cooldownContainerMap.get(playerId);
    }

    public void setCooldownFromNow(UUID pUUID, String name, Long millis) {
        CooldownContainer container = getContainer(pUUID);
        container.setCooldownFromNow(name, millis);
    }

    public long getCooldownLeft(UUID pUUID, String name) {
        CooldownContainer container = getContainer(pUUID);
        return container.getCooldownLeft(name);
    }

    public boolean isCooldownDone(UUID pUUID, String name) {
        CooldownContainer container = getContainer(pUUID);
        return container.isCooldownDone(name);
    }
}
