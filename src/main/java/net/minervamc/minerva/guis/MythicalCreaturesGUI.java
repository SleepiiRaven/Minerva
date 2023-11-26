package net.minervamc.minerva.guis;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MythicalCreaturesGUI {
    public static final String invName = "Choose a Mythical Creature...";
    private static final ItemStack satyr = ItemUtils.getItem(new ItemStack(Material.BAMBOO), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Satyr", ChatColor.GRAY + "Become a satyr, a half-goat, half-human mythical being.");
    private static final ItemStack harpy = ItemUtils.getItem(new ItemStack(Material.FEATHER), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Harpy", ChatColor.GRAY + "Become a harpy, a winged spirit of sharp wind.");
    private static final ItemStack dryad = ItemUtils.getItem(new ItemStack(Material.OAK_SAPLING), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Dryad", ChatColor.GRAY + "Become a dryad, a tree spirit.");
    private static final ItemStack cyclops = ItemUtils.getItem(new ItemStack(Material.RABBIT_STEW), ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Cyclops", ChatColor.GRAY + "Become a cyclops, a strong one-eyed giant.");
    private static final ItemStack back = ItemUtils.getItem(new ItemStack(Material.ARROW), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Back");
    private static final int satyrSlot = 22;
    private static final int harpySlot = 30;
    private static final int dryadSlot = 31;
    private static final int cyclopsSlot = 32;
    private static final int backSlot = 45;
    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9 * 6, invName);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " "));
        }
        inv.setItem(satyrSlot, satyr);
        inv.setItem(harpySlot, harpy);
        inv.setItem(dryadSlot, dryad);
        inv.setItem(cyclopsSlot, cyclops);
        inv.setItem(backSlot, back);
        player.openInventory(inv);
    }

    public static void clickedGUI(InventoryClickEvent event) {
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
        switch (event.getSlot()) {
            case satyrSlot:
                chooseHeritage(HeritageType.SATYR, "satyr", (Player) event.getWhoClicked());
                break;
            case harpySlot:
                chooseHeritage(HeritageType.HARPY, "harpy", (Player) event.getWhoClicked());
                break;
            case dryadSlot:
                chooseHeritage(HeritageType.DRYAD, "dryad", (Player) event.getWhoClicked());
                break;
            case cyclopsSlot:
                chooseHeritage(HeritageType.CYCLOPS, "cyclops", (Player) event.getWhoClicked());
                break;
            case backSlot:
                AncestryGUI.openGUI((Player) event.getWhoClicked());
                event.getWhoClicked().getWorld().playSound(event.getWhoClicked().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                break;
        }
    }
    private static void chooseHeritage(HeritageType type, String youAreNowABlank, Player player) {
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        stats.setHeritage(type);
        stats.save();
        player.sendMessage(ChatColor.GREEN + "You are now a " + youAreNowABlank + "!");
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        SkillUtils.setDefaultSkills(type, player);
        player.closeInventory();
    }
}
