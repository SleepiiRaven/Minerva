package net.minervamc.minerva.utils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minervamc.minerva.Minerva;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemUtils {

    public static void sendItemMessage(Player player, String message) {

        ItemStack item = player.getInventory().getItemInMainHand();
        sendItemMessage(player, message, item);
    }

    public static void sendItemMessage(Player player, String message, ItemStack item) {
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

    public static ItemStack getItem(ItemStack item, String name, String ... lore) {
        ItemMeta iM = item.getItemMeta();
        iM.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(iM);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> lores = new ArrayList<>();
        for (String s : lore) {
            if (s == null)
            lores.add(ChatColor.translateAlternateColorCodes('&', s));
        }

        meta.setLore(lores);

        item.setItemMeta(meta);

        return item;
    }
    public static final Set<Material> weapons = EnumSet.of (
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.GOLDEN_AXE,
            Material.IRON_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE,
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.GOLDEN_SWORD,
            Material.IRON_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD,
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.IRON_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL,
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.GOLDEN_HOE,
            Material.IRON_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE,
            Material.SHEARS
    );
}
