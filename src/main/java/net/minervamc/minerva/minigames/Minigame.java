package net.minervamc.minerva.minigames;

import java.util.List;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.CTFKitGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public abstract class Minigame {

    public static void saveAndClearInventories(List<Player> players) {
        for (Player player : players) {
            System.out.println(player + " is getting saved!");
            PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
            stats.setInventory(player.getInventory().getStorageContents());
            stats.setArmor(player.getInventory().getArmorContents());
            ItemStack[] offhand = {player.getInventory().getItemInOffHand()};
            stats.setOffhand(offhand);
            stats.save();
            player.getInventory().clear();
        }
    }
    public static void loadInventories(List<Player> players) {
        for (Player player : players) {
            PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
            player.getInventory().clear();
            player.getInventory().setStorageContents(stats.getInventory());
            player.getInventory().setArmorContents(stats.getArmor());
            player.getInventory().setItemInOffHand(stats.getOffhand()[0]);
        }
    }

    public static void kits(Player player, String type) {
        switch (type) {
            case "ctf" -> new CTFKitGUI().open(player);
        }
    }
}
