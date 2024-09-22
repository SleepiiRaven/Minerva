package net.minervamc.minerva.minigames;

import java.util.List;
import net.minervamc.minerva.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public abstract class Minigame {

    public static void saveAndClearInventories(List<Player> players) {
        for (Player player : players) {
            PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
            stats.setInventory(player.getInventory().getStorageContents());
            player.getInventory().clear();
        }
    }
    public static void loadInventories(List<Player> players) {
        for (Player player : players) {
            PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
            player.getInventory().clear();
            player.getInventory().setStorageContents(stats.getInventory());
        }
    }
}
