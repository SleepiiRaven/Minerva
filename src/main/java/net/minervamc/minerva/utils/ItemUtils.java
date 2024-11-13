package net.minervamc.minerva.utils;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {
    public static void sendItemMessage(Player player, String message) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = item.getItemMeta();
        if (item.getType().isAir()) return;
        String oldName = itemMeta.getDisplayName();
        String storedName = itemMeta.getPersistentDataContainer().get(Minerva.itemMessageKey, PersistentDataType.STRING);
        if (!oldName.equals(storedName) && storedName != null) {
            oldName = storedName;
        }
        itemMeta.getPersistentDataContainer().set(Minerva.itemMessageKey, PersistentDataType.STRING, oldName);
        if (ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(message))) {
//                itemMeta.setDisplayName("a ");
//                item.setItemMeta(itemMeta);

            message = " " + message + " ";

            player.updateInventory();

        }
        itemMeta.setDisplayName(message);
        item.setItemMeta(itemMeta);
    }

    public static void resetItemDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();
            String previousName = itemMeta.getPersistentDataContainer().get(Minerva.itemMessageKey, PersistentDataType.STRING);
            if (previousName != null) {
                itemMeta.setDisplayName(previousName);
            } else {
                itemMeta.getPersistentDataContainer().set(Minerva.itemMessageKey, PersistentDataType.STRING, itemMeta.getDisplayName());
            }

            item.setItemMeta(itemMeta);
        }
    }

    public static ItemStack getItem(ItemStack item, String name, String... lore) {
        ItemMeta iM = item.getItemMeta();
        iM.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(iM);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> lores = new ArrayList<>();
        for (String s : lore) {
            if (s != null) lores.add(ChatColor.translateAlternateColorCodes('&', s));
        }

        meta.setLore(lores);

        item.setItemMeta(meta);

        return item;
    }
}
