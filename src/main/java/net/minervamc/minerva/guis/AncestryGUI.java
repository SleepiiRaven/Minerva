package net.minervamc.minerva.guis;

import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AncestryGUI {
    public static final String invName = "Choose Your Ancestry...";
    private static final ItemStack greek = ItemUtils.getItem(new ItemStack(Material.ORANGE_BANNER), ChatColor.GOLD + "" + ChatColor.BOLD + "[Greek Gods]", ChatColor.GRAY + "Become a Greek demigod!");
    private static final ItemStack roman = ItemUtils.getItem(new ItemStack(Material.PURPLE_BANNER), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[Roman Gods]", ChatColor.GRAY + "Become a Roman demigod!");
    //private static final ItemStack titan = ItemUtils.getItem(new ItemStack(Material.RED_BANNER), ChatColor.RED + "" + ChatColor.BOLD + "[Titans]", ChatColor.GRAY + "Become a demititan! (Requires Donator Rank)");
    //private static final ItemStack mythicalCreature = ItemUtils.getItem(new ItemStack(Material.ENDER_EYE), ChatColor.GREEN + "" + ChatColor.BOLD + "[Mythical Creature]", ChatColor.GRAY + "Become a mythical creature!");
    private static final ItemStack comingSoon = ItemUtils.getItem(new ItemStack(Material.BLACK_BANNER), ChatColor.BLACK + "" + ChatColor.MAGIC + "" + ChatColor.BOLD + "[Clear Sight Mortal]", ChatColor.GRAY + "Coming soon...");
    private static final int mythicalSlot = 13;
    private static final int greekSlot = 28;
    private static final int romanSlot = 30;
    private static final int titanSlot = 32;
    private static final int mortalSlot = 34;
    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9 * 6, invName);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " "));
        }
        inv.setItem(mythicalSlot, comingSoon);
        inv.setItem(greekSlot, greek);
        inv.setItem(romanSlot, roman);
        inv.setItem(titanSlot, comingSoon);
        inv.setItem(mortalSlot, comingSoon);
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.GRASS_BLOCK), " "));
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.DIRT), " "));
        }
        player.openInventory(inv);
    }

    public static void clickedGUI(InventoryClickEvent event) {
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
        switch (event.getSlot()) {
            //case mythicalSlot -> MythicalCreaturesGUI.openGUI((Player) event.getWhoClicked());
            case greekSlot -> GreekGodsGUI.openGUI((Player) event.getWhoClicked());
            case romanSlot -> RomanGodsGUI.openGUI((Player) event.getWhoClicked());
            //case titanSlot -> TitansGUI.openGUI((Player) event.getWhoClicked());
            case mortalSlot, titanSlot, mythicalSlot -> {
                event.getWhoClicked().sendMessage(ChatColor.RED + "This option has not been unlocked yet.");
                ((Player) event.getWhoClicked()).playSound(event.getWhoClicked(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
        }
    }
}