package net.minervamc.minerva.lib.util;

import java.util.HashMap;
public class CooldownManager {

    private static final HashMap<String, HashMap<String, Long>> cooldownMap = new HashMap<>();

    /**
     * Sets a cooldown for the specified user and command.
     *
     * @param name    The user's name or unique identifier.
     * @param command The command to apply the cooldown to.
     * @param seconds The cooldown duration in seconds.
     */
    public static void setCooldown(String name, String command, int seconds) {
        long currentTime = System.currentTimeMillis();
        long cooldownEndTime = currentTime + (seconds * 1000L);

        cooldownMap.computeIfAbsent(name, k -> new HashMap<>());

        cooldownMap.get(name).put(command, cooldownEndTime);
    }

    /**
     * Checks if the cooldown has expired for the specified user and command.
     *
     * @param name    The user's name or unique identifier.
     * @param command The command to check.
     * @return True if the cooldown has expired, false otherwise.
     */
    public static boolean isCooldownExpired(String name, String command) {
        if (!cooldownMap.containsKey(name)) {
            return true;
        }

        Long cooldownEndTime = cooldownMap.get(name).get(command);
        if (cooldownEndTime == null) return true;

        if (System.currentTimeMillis() > cooldownEndTime) {
            removeCooldown(name, command);
            return true;
        }
        return false;
    }

    /**
     * Removes the cooldown for the specified user and command.
     *
     * @param name    The user's name or unique identifier.
     * @param command The command to remove the cooldown from.
     */
    public static void removeCooldown(String name, String command) {
        if (cooldownMap.containsKey(name)) {
            cooldownMap.get(name).remove(command);
        }
    }
}
