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

public class GreekGodsGUI {
    public static final String invName = "Choose a Greek God...";
    private static final ItemStack comingSoon = ItemUtils.getItem(new ItemStack(Material.ENDER_EYE), ChatColor.BLACK + "" + ChatColor.MAGIC + ChatColor.BOLD + "[Clear Sight Mortal]", ChatColor.GRAY + "Coming soon...");
    private static final ItemStack zeus = ItemUtils.getItem(new ItemStack(Material.LIGHTNING_ROD), ChatColor.RED + "" + ChatColor.BOLD + "Zeus", ChatColor.GRAY + "Become a child of Zeus, god", ChatColor.GRAY + "of the " + ChatColor.YELLOW + "sky." + ChatColor.GRAY + " (Requires Donator Rank)");
    private static final ItemStack poseidon = ItemUtils.getItem(new ItemStack(Material.TRIDENT), ChatColor.RED + "" + ChatColor.BOLD + "Poseidon", ChatColor.GRAY + "Become a child of Poseidon, god", ChatColor.GRAY + "of the " + ChatColor.BLUE + "sea." + ChatColor.GRAY + " (Requires Donator Rank)");
    private static final ItemStack hades = ItemUtils.getItem(new ItemStack(Material.WITHER_SKELETON_SKULL), ChatColor.RED + "" + ChatColor.BOLD + "Hades", ChatColor.GRAY + "Become a child of Hades, god", ChatColor.GRAY + "of the " + ChatColor.DARK_GRAY + "dead." + ChatColor.GRAY + " (Requires Donator Rank)");
    private static final ItemStack hypnos = ItemUtils.getItem(new ItemStack(Material.ORANGE_BED), ChatColor.GOLD + "" + ChatColor.BOLD + "Hypnos", ChatColor.GRAY + "Become a child of Hypnos, the god of sleep.");
    private static final ItemStack iris = ItemUtils.getItem(new ItemStack(Material.GOLD_NUGGET), ChatColor.GOLD + "" + ChatColor.BOLD + "Iris", ChatColor.GRAY + "Become a child of Iris, the goddess of the rainbow.");
    private static final ItemStack psyche = ItemUtils.getItem(new ItemStack(Material.DRAGON_BREATH), ChatColor.GOLD + "" + ChatColor.BOLD + "Psyche", ChatColor.GRAY + "Become a child of Psyche, the goddess of the soul.");
    private static final ItemStack hermes = ItemUtils.getItem(new ItemStack(Material.LEATHER_BOOTS), ChatColor.GOLD + "" + ChatColor.BOLD + "Hermes", ChatColor.GRAY + "Become a child of Hermes, the messenger god of Olympus.");
    private static final ItemStack hecate = ItemUtils.getItem(new ItemStack(Material.ENCHANTING_TABLE), ChatColor.GOLD + "" + ChatColor.BOLD + "Hecate", ChatColor.GRAY + "Become a child of Hecate, the goddess of magic.");
    private static final ItemStack artemis = ItemUtils.getItem(new ItemStack(Material.RABBIT_HIDE), ChatColor.GOLD + "" + ChatColor.BOLD + "Artemis", ChatColor.GRAY + "Join the Hunters of Artemis, a", ChatColor.GRAY + "group of hunters who hunt with Artemis, goddess of the hunt.");
    private static final ItemStack athena = ItemUtils.getItem(new ItemStack(Material.WRITABLE_BOOK), ChatColor.GOLD + "" + ChatColor.BOLD + "Athena", ChatColor.GRAY + "Become a child of Athena, goddess of wisdom.");
    private static final ItemStack ares = ItemUtils.getItem(new ItemStack(Material.STONE_SWORD), ChatColor.GOLD + "" + ChatColor.BOLD + "Ares", ChatColor.GRAY + "Become a child of Ares, the god of war.");
    private static final ItemStack dionysus = ItemUtils.getItem(new ItemStack(Material.HONEY_BOTTLE), ChatColor.GOLD + "" + ChatColor.BOLD + "Dionysus", ChatColor.GRAY + "Become a child of Dionysus, the god of wine.");
    private static final ItemStack hephaestus = ItemUtils.getItem(new ItemStack(Material.RAW_IRON), ChatColor.GOLD + "" + ChatColor.BOLD + "Hephaestus", ChatColor.GRAY + "Become a child of Hephaestus, the god of the forge.");
    private static final ItemStack aphrodite = ItemUtils.getItem(new ItemStack(Material.ROSE_BUSH), ChatColor.GOLD + "" + ChatColor.BOLD + "Aphrodite", ChatColor.GRAY + "Become a child of Aphrodite, the goddess of love.");
    private static final ItemStack demeter = ItemUtils.getItem(new ItemStack(Material.WHEAT), ChatColor.GOLD + "" + ChatColor.BOLD + "Demeter", ChatColor.GRAY + "Become a child of Demeter, goddess of the harvest.");
    private static final ItemStack apollo = ItemUtils.getItem(new ItemStack(Material.BOW), ChatColor.GOLD + "" + ChatColor.BOLD + "Apollo", ChatColor.GRAY + "Become a child of Apollo, the god of archery.");
    private static final ItemStack hestia = ItemUtils.getItem(new ItemStack(Material.CAMPFIRE), ChatColor.GOLD + "" + ChatColor.BOLD + "Hestia", ChatColor.GRAY + "Become a child of Hestia, goddess of the hearth.");
    private static final ItemStack arke = ItemUtils.getItem(new ItemStack(Material.PAPER), ChatColor.GOLD + "" + ChatColor.BOLD + "Arke", ChatColor.GRAY + "Become a child of Arke, messenger of the Titans.");
    private static final ItemStack khione = ItemUtils.getItem(new ItemStack(Material.SNOWBALL), ChatColor.GOLD + "" + ChatColor.BOLD + "Khione", ChatColor.GRAY + "Become a child of Khione, the goddess of snow.");
    private static final ItemStack back = ItemUtils.getItem(new ItemStack(Material.ARROW), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Back");
    private static final int zeusSlot = 12;
    private static final int poseidonSlot = 13;
    private static final int hadesSlot = 14;
    private static final int hypnosSlot = 19;
    private static final int khioneSlot = 25;
    private static final int irisSlot = 27;
    private static final int psycheSlot = 28;
    private static final int hermesSlot = 29;
    private static final int artemisSlot = 30;
    private static final int athenaSlot = 40;
    private static final int hephaestusSlot = 41;
    private static final int demeterSlot = 33;
    private static final int hestiaSlot = 34;
    private static final int arkeSlot = 35;
    private static final int hecateSlot = 38;
    private static final int aresSlot = 39;
    private static final int dionysusSlot = 31;
    private static final int aphroditeSlot = 42;
    private static final int apolloSlot = 32;
    private static final int backSlot = 45;

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9 * 6, invName);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " "));
        }
        inv.setItem(zeusSlot, zeus);
        inv.setItem(poseidonSlot, poseidon);
        inv.setItem(hadesSlot, hades);
        inv.setItem(hypnosSlot, comingSoon);
        inv.setItem(khioneSlot, comingSoon);
        inv.setItem(irisSlot, comingSoon);
        inv.setItem(psycheSlot, comingSoon);
        inv.setItem(hermesSlot, comingSoon);
        inv.setItem(artemisSlot, artemis);
        inv.setItem(athenaSlot, comingSoon);
        inv.setItem(hephaestusSlot, hephaestus);
        inv.setItem(demeterSlot, comingSoon);
        inv.setItem(hestiaSlot, comingSoon);
        inv.setItem(arkeSlot, comingSoon);
        inv.setItem(hecateSlot, comingSoon);
        inv.setItem(apolloSlot, apollo);
        inv.setItem(aresSlot, ares);
        inv.setItem(aphroditeSlot, comingSoon);
        inv.setItem(dionysusSlot, dionysus);
        inv.setItem(backSlot, back);
        player.openInventory(inv);
    }

    public static void clickedGUI(InventoryClickEvent event) {
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
        switch (event.getSlot()) {
            case zeusSlot:
                if (event.getWhoClicked().hasPermission("minerva.bigthree.zeus")) {
                    chooseHeritage(HeritageType.ZEUS, "child of Zeus", (Player) event.getWhoClicked());
                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You must have the Zeus rank to play as a child of Zeus!");
                }
                break;
            case poseidonSlot:
                if (event.getWhoClicked().hasPermission("minerva.bigthree.poseidon")) {
                    chooseHeritage(HeritageType.POSEIDON, "child of Poseidon", (Player) event.getWhoClicked());
                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You must have the Poseidon rank to play as a child of Poseidon!");
                }

                break;
            case hadesSlot:
                if (event.getWhoClicked().hasPermission("minerva.bigthree.hades")) {
                    chooseHeritage(HeritageType.HADES, "child of Hades", (Player) event.getWhoClicked());
                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You must have the Hades rank to play as a child of Hades!");
                }
                break;
            case hypnosSlot:
                //chooseHeritage(HeritageType.HYPNOS, "child of Hypnos", (Player) event.getWhoClicked());
                break;
            case khioneSlot:
                //chooseHeritage(HeritageType.KHIONE, "child of Khione", (Player) event.getWhoClicked());
                break;
            case irisSlot:
                //chooseHeritage(HeritageType.IRIS, "child of Iris", (Player) event.getWhoClicked());
                break;
            case psycheSlot:
                //chooseHeritage(HeritageType.PSYCHE_GREEK, "child of Psyche", (Player) event.getWhoClicked());
                break;
            case hermesSlot:
                //chooseHeritage(HeritageType.HERMES, "child of Hermes", (Player) event.getWhoClicked());
                break;
            case artemisSlot:
                chooseHeritage(HeritageType.ARTEMIS, "Huntress of Artemis", (Player) event.getWhoClicked());
                break;
            case athenaSlot:
                //chooseHeritage(HeritageType.ATHENA, "child of Athena", (Player) event.getWhoClicked());
                break;
            case hephaestusSlot:
                chooseHeritage(HeritageType.HEPHAESTUS, "child of Hephaestus", (Player) event.getWhoClicked());
                break;
            case demeterSlot:
                //chooseHeritage(HeritageType.DEMETER, "child of Demeter", (Player) event.getWhoClicked());
                break;
            case hestiaSlot:
                //chooseHeritage(HeritageType.HESTIA, "child of Hestia", (Player) event.getWhoClicked());
                break;
            case arkeSlot:
                //chooseHeritage(HeritageType.ARKE, "child of Arke", (Player) event.getWhoClicked());
                break;
            case hecateSlot:
                //chooseHeritage(HeritageType.HECATE, "child of Hecate", (Player) event.getWhoClicked());
                break;
            case apolloSlot:
                chooseHeritage(HeritageType.APOLLO_GREEK, "child of Apollo", (Player) event.getWhoClicked());
                break;
            case aresSlot:
                chooseHeritage(HeritageType.ARES, "child of Ares", (Player) event.getWhoClicked());
                break;
            case aphroditeSlot:
                // chooseHeritage(HeritageType.APHRODITE, "child of Aphrodite", (Player) event.getWhoClicked());
                break;
            case dionysusSlot:
                chooseHeritage(HeritageType.DIONYSUS, "child of Dionysus", (Player) event.getWhoClicked());
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
        SkillsGUI.openGUI(player);
    }
}
