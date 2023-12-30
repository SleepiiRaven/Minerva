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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RomanGodsGUI {
    public static final String invName = "Choose a Roman God...";
    private static final ItemStack comingSoon = ItemUtils.getItem(new ItemStack(Material.ENDER_EYE), ChatColor.BLACK + "" + ChatColor.MAGIC + "" + ChatColor.BOLD + "[Clear Sight Mortal]", ChatColor.GRAY + "Coming soon...");
    private static final ItemStack jupiter = ItemUtils.getItem(new ItemStack(Material.GOLDEN_HELMET), ChatColor.RED + "" + ChatColor.BOLD + "Jupiter", ChatColor.GRAY + "Become a child of Jupiter, god of the sky. (Requires Donator Rank)");
    private static final ItemStack neptune = ItemUtils.getItem(new ItemStack(Material.TRIDENT), ChatColor.RED + "" + ChatColor.BOLD + "Neptune", ChatColor.GRAY + "Become a child of Neptune, god of the sea. (Requires Donator Rank)");
    private static final ItemStack pluto = ItemUtils.getItem(new ItemStack(Material.DIAMOND), ChatColor.RED + "" + ChatColor.BOLD + "Pluto", ChatColor.GRAY + "Become a child of Pluto, god of the dead. (Requires Donator Rank)");
    private static final ItemStack somnus = ItemUtils.getItem(new ItemStack(Material.PURPLE_BED), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Somnus", ChatColor.GRAY + "Become a child of Somnus, the god of sleep.");
    private static final ItemStack arcus = ItemUtils.getItem(new ItemStack(Material.GOLD_NUGGET), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Arcus", ChatColor.GRAY + "Become a child of Arcus, the goddess of the rainbow.");
    private static final ItemStack psyche = ItemUtils.getItem(new ItemStack(Material.DRAGON_BREATH), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Psyche", ChatColor.GRAY + "Become a child of Psyche, the goddess of the soul.");
    private static final ItemStack mercury = ItemUtils.getItem(new ItemStack(Material.SUNFLOWER), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Mercury", ChatColor.GRAY + "Become a child of Mercury, the god of commerce.");
    private static final ItemStack diana = ItemUtils.getItem(new ItemStack(Material.ARROW), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Diana", ChatColor.GRAY + "Join the Hunters of Diana, a group of people who hunt with Diana, goddess of the hunt.");
    private static final ItemStack janus = ItemUtils.getItem(new ItemStack(Material.DARK_OAK_DOOR), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Janus", ChatColor.GRAY + "Become a child of Janus, god of beginnings and transitions.");
    private static final ItemStack bellona = ItemUtils.getItem(new ItemStack(Material.IRON_HELMET), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Bellona", ChatColor.GRAY + "Become a child of Bellona, goddess of war.");
    private static final ItemStack mars = ItemUtils.getItem(new ItemStack(Material.SHIELD), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Mars", ChatColor.GRAY + "Become a child of Mars, the god of war.");
    private static final ItemStack bacchus = ItemUtils.getItem(new ItemStack(Material.GLOW_BERRIES), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Bacchus", ChatColor.GRAY + "Become a child of Bacchus, the god of wine.");
    private static final ItemStack vulcan = ItemUtils.getItem(new ItemStack(Material.MAGMA_BLOCK), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Vulcan", ChatColor.GRAY + "Become a child of Vulcan, the god of fire.");
    private static final ItemStack venus = ItemUtils.getItem(new ItemStack(Material.ROSE_BUSH), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Venus", ChatColor.GRAY + "Become a child of Venus, the goddess of love.");
    private static final ItemStack ceres = ItemUtils.getItem(new ItemStack(Material.BEETROOT_SEEDS), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Ceres", ChatColor.GRAY + "Become a child of Ceres, goddess of agriculture.");
    private static final ItemStack apollo = ItemUtils.getItem(new ItemStack(Material.BOW), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Apollo", ChatColor.GRAY + "Become a child of Apollo, the god of light.");
    private static final ItemStack vesta = ItemUtils.getItem(new ItemStack(Material.CAMPFIRE), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Vesta", ChatColor.GRAY + "Become a child of Vesta, goddess of the hearth.");
    private static final ItemStack arce = ItemUtils.getItem(new ItemStack(Material.PAPER), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Arce", ChatColor.GRAY + "Become a child of Arce, messenger of the Titans.");
    private static final ItemStack chione = ItemUtils.getItem(new ItemStack(Material.SNOW_BLOCK), ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Chione", ChatColor.GRAY + "Become a child of Chione, the goddess of snow.");
    private static final ItemStack back = ItemUtils.getItem(new ItemStack(Material.ARROW), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Back");
    private static final int jupiterSlot = 12;
    private static final int neptuneSlot = 13;
    private static final int plutoSlot = 14;
    private static final int somnusSlot = 19;
    private static final int chioneSlot = 25;
    private static final int arcusSlot = 27;
    private static final int psycheSlot = 28;
    private static final int mercurySlot = 29;
    private static final int dianaSlot = 30;
    private static final int marsSlot = 40;
    private static final int vulcanSlot = 42;
    private static final int ceresSlot = 33;
    private static final int vestaSlot = 34;
    private static final int arceSlot = 35;
    private static final int janusSlot = 38;
    private static final int bellonaSlot = 39;
    private static final int bacchusSlot = 31;
    private static final int venusSlot = 41;
    private static final int apolloSlot = 32;
    private static final int backSlot = 45;
    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9 * 6, invName);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " "));
        }
        inv.setItem(jupiterSlot, jupiter);
        inv.setItem(neptuneSlot, neptune);
        inv.setItem(plutoSlot, pluto);
        inv.setItem(somnusSlot, comingSoon);
        inv.setItem(chioneSlot, comingSoon);
        inv.setItem(arcusSlot, comingSoon);
        inv.setItem(psycheSlot, comingSoon);
        inv.setItem(mercurySlot, comingSoon);
        inv.setItem(dianaSlot, diana);
        inv.setItem(bellonaSlot, comingSoon);
        inv.setItem(vulcanSlot, comingSoon);
        inv.setItem(ceresSlot, comingSoon);
        inv.setItem(vestaSlot, comingSoon);
        inv.setItem(arceSlot, comingSoon);
        inv.setItem(janusSlot, comingSoon);
        inv.setItem(apolloSlot, apollo);
        inv.setItem(marsSlot, comingSoon);
        inv.setItem(venusSlot, comingSoon);
        inv.setItem(bacchusSlot, bacchus);
        inv.setItem(backSlot, back);
        player.openInventory(inv);
    }

    public static void clickedGUI(InventoryClickEvent event) {
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
        switch (event.getSlot()) {
            case jupiterSlot:
                if (event.getWhoClicked().hasPermission("minerva.bigthree.zeus")) {
                    chooseHeritage(HeritageType.JUPITER, "child of Jupiter", (Player) event.getWhoClicked());
                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You must have the Big Three rank to play as a child of Jupiter!");
                }
                break;
            case neptuneSlot:
                if (event.getWhoClicked().hasPermission("minerva.bigthree.poseidon")) {
                    chooseHeritage(HeritageType.NEPTUNE, "child of Neptune", (Player) event.getWhoClicked());
                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You must have the Big Three rank to play as a child of Neptune!");
                }

                break;
            case plutoSlot:
                if (event.getWhoClicked().hasPermission("minerva.bigthree.hades")) {
                    chooseHeritage(HeritageType.PLUTO, "child of Pluto", (Player) event.getWhoClicked());
                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You must have the Big Three rank to play as a child of Pluto!");
                }
                break;
            case somnusSlot:
                //chooseHeritage(HeritageType.SOMNUS, "child of Somnus", (Player) event.getWhoClicked());
                break;
            case chioneSlot:
                //chooseHeritage(HeritageType.CHIONE, "child of Chione", (Player) event.getWhoClicked());
                break;
            case arcusSlot:
                //chooseHeritage(HeritageType.ARCUS, "child of Arcus", (Player) event.getWhoClicked());
                break;
            case psycheSlot:
                //chooseHeritage(HeritageType.PSYCHE_ROMAN, "child of Psyche", (Player) event.getWhoClicked());
                break;
            case mercurySlot:
                //chooseHeritage(HeritageType.MERCURY, "child of Mercury", (Player) event.getWhoClicked());
                break;
            case dianaSlot:
                chooseHeritage(HeritageType.DIANA, "Hunter of Diana", (Player) event.getWhoClicked());
                break;
            case bellonaSlot:
                //chooseHeritage(HeritageType.BELLONA, "child of Bellona", (Player) event.getWhoClicked());
                break;
            case vulcanSlot:
                //chooseHeritage(HeritageType.VULCAN, "child of Vulcan", (Player) event.getWhoClicked());
                break;
            case ceresSlot:
                //(HeritageType.CERES, "child of Ceres", (Player) event.getWhoClicked());
                break;
            case vestaSlot:
                //chooseHeritage(HeritageType.VESTA, "child of Vesta", (Player) event.getWhoClicked());
                break;
            case arceSlot:
                //chooseHeritage(HeritageType.ARCE, "child of Arce", (Player) event.getWhoClicked());
                break;
            case janusSlot:
                //chooseHeritage(HeritageType.JANUS, "child of Janus", (Player) event.getWhoClicked());
                break;
            case apolloSlot:
                chooseHeritage(HeritageType.APOLLO_ROMAN, "child of Apollo", (Player) event.getWhoClicked());
                break;
            case marsSlot:
                //chooseHeritage(HeritageType.MARS, "child of Mars", (Player) event.getWhoClicked());
                break;
            case venusSlot:
                //chooseHeritage(HeritageType.VENUS, "child of Venus", (Player) event.getWhoClicked());
                break;
            case bacchusSlot:
                chooseHeritage(HeritageType.BACCHUS, "child of Bacchus", (Player) event.getWhoClicked());
                break;
            case backSlot:
                AncestryGUI.openGUI((Player) event.getWhoClicked());
                ((Player) event.getWhoClicked()).playSound(event.getWhoClicked(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
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
