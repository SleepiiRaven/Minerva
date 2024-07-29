package net.minervamc.minerva.guis;

import net.kyori.adventure.text.Component;
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

public class TitansGUI {
    public static final String invName = "Choose a Titan...";
    private static final ItemStack oceanus = ItemUtils.getItem(new ItemStack(Material.NAUTILUS_SHELL), ChatColor.RED + "" + ChatColor.BOLD + "Oceanus", ChatColor.GRAY + "Become a child of Oceanus, titan of the sea. (Requires Donator Rank)");
    private static final ItemStack hyperion = ItemUtils.getItem(new ItemStack(Material.SHROOMLIGHT), ChatColor.RED + "" + ChatColor.BOLD + "Hyperion", ChatColor.GRAY + "Become a child of Hyperion, titan of light. (Requires Donator Rank)");
    private static final ItemStack atlas = ItemUtils.getItem(new ItemStack(Material.HEART_OF_THE_SEA), ChatColor.RED + "" + ChatColor.BOLD + "Atlas", ChatColor.GRAY + "Become a child of Atlas, titan of endurance. (Requires Donator Rank)");
    private static final ItemStack styx = ItemUtils.getItem(new ItemStack(Material.WITHER_ROSE), ChatColor.RED + "" + ChatColor.BOLD + "Styx", ChatColor.GRAY + "Become a child of Styx, titan of oaths, hatred, and the river Styx. (Requires Donator Rank)");
    private static final ItemStack mnemosyne = ItemUtils.getItem(new ItemStack(Material.CANDLE), ChatColor.RED + "" + ChatColor.BOLD + "Mnemosyne", ChatColor.GRAY + "Become a child of Styx, titan of memory. (Requires Donator Rank)");
    private static final ItemStack koios = ItemUtils.getItem(new ItemStack(Material.ENDER_PEARL), ChatColor.RED + "" + ChatColor.BOLD + "Koios", ChatColor.GRAY + "Become a child of Koios, titan of foresight. (Requires Donator Rank)");
    private static final ItemStack rhea = ItemUtils.getItem(new ItemStack(Material.VINE), ChatColor.RED + "" + ChatColor.BOLD + "Rhea", ChatColor.GRAY + "Become a child of Rhea, titan of fertility. (Requires Donator Rank)");
    private static final ItemStack prometheus = ItemUtils.getItem(new ItemStack(Material.TOTEM_OF_UNDYING), ChatColor.RED + "" + ChatColor.BOLD + "Prometheus", ChatColor.GRAY + "Become a child of Prometheus, titan of fire and the creator of humans. (Requires Donator Rank)");
    private static final ItemStack back = ItemUtils.getItem(new ItemStack(Material.ARROW), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Back");
    private static final int oceanusSlot = 21;
    private static final int hyperionSlot = 22;
    private static final int atlasSlot = 23;
    private static final int styxSlot = 29;
    private static final int mnemosyneSlot = 30;
    private static final int koiosSlot = 31;
    private static final int rheaSlot = 32;
    private static final int prometheusSlot = 33;
    private static final int backSlot = 45;
    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9 * 6, Component.text(invName));
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " "));
        }
        inv.setItem(hyperionSlot, hyperion);
        inv.setItem(oceanusSlot, oceanus);
        inv.setItem(atlasSlot, atlas);
        inv.setItem(styxSlot, styx);
        inv.setItem(mnemosyneSlot, mnemosyne);
        inv.setItem(koiosSlot, koios);
        inv.setItem(rheaSlot, rhea);
        inv.setItem(prometheusSlot, prometheus);
        inv.setItem(backSlot, back);
        player.openInventory(inv);
    }

    public static void clickedGUI(InventoryClickEvent event) {
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
        switch (event.getSlot()) {
            case oceanusSlot:
                chooseHeritage(HeritageType.OCEANUS, "child of Oceanus", (Player) event.getWhoClicked());
                break;
            case atlasSlot:
                chooseHeritage(HeritageType.HYPERION, "child of Hyperion", (Player) event.getWhoClicked());
                break;
            case hyperionSlot:
                chooseHeritage(HeritageType.ATLAS, "child of Atlas", (Player) event.getWhoClicked());
                break;
            case styxSlot:
                chooseHeritage(HeritageType.STYX, "child of Styx", (Player) event.getWhoClicked());
                break;
            case mnemosyneSlot:
                chooseHeritage(HeritageType.MNEMOSYNE, "child of Mnemosyne", (Player) event.getWhoClicked());
                break;
            case koiosSlot:
                chooseHeritage(HeritageType.KOIOS, "child of Koios", (Player) event.getWhoClicked());
                break;
            case rheaSlot:
                chooseHeritage(HeritageType.RHEA, "child of Rhea", (Player) event.getWhoClicked());
                break;
            case prometheusSlot:
                chooseHeritage(HeritageType.PROMETHEUS, "child of Prometheus", (Player) event.getWhoClicked());
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
