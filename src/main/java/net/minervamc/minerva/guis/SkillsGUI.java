package net.minervamc.minerva.guis;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.types.SkillType;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SkillsGUI {
    public static final String invName = "View and upgrade your skills.";
    private static final int drachmaeCost = 50;
    private static final ItemStack rrrOnItem = ItemUtils.getItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE), ChatColor.GREEN + "" + ChatColor.BOLD + "[R-R-R]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack rlrOnItem = ItemUtils.getItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE), ChatColor.GREEN + "" + ChatColor.BOLD + "[R-L-R]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack rllOnItem = ItemUtils.getItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE), ChatColor.GREEN + "" + ChatColor.BOLD + "[R-L-L]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack rrlOnItem = ItemUtils.getItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE), ChatColor.GREEN + "" + ChatColor.BOLD + "[R-R-L]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack passiveOnItem = ItemUtils.getItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE), ChatColor.GREEN + "" + ChatColor.BOLD + "[PASSIVE]", ChatColor.GREEN + "Click to toggle this skill.");
   private static final ItemStack rrrOffItem = ItemUtils.getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), ChatColor.RED + "" + ChatColor.BOLD + "[R-R-R]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack rlrOffItem = ItemUtils.getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), ChatColor.RED + "" + ChatColor.BOLD + "[R-L-R]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack rllOffItem = ItemUtils.getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), ChatColor.RED + "" + ChatColor.BOLD + "[R-L-L]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack rrlOffItem = ItemUtils.getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), ChatColor.RED + "" + ChatColor.BOLD + "[R-R-L]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack passiveOffItem = ItemUtils.getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), ChatColor.RED + "" + ChatColor.BOLD + "[PASSIVE]", ChatColor.GREEN + "Click to toggle this skill.");
    private static final ItemStack resetItem = ItemUtils.getItem(new ItemStack(Material.NETHER_STAR), ChatColor.DARK_RED + "" + ChatColor.BOLD + "Reset skills and regain points spent.");
    private static final ItemStack back = ItemUtils.getItem(new ItemStack(Material.ARROW), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Back");
    private static final int rrrSlot = 2;
    private static final int rlrSlot = 11;
    private static final int rllSlot = 20;
    private static final int rrlSlot = 29;
    private static final int passiveSlot = 38;
    private static final int rrrToggleSlot = 1;
    private static final int rlrToggleSlot = 10;
    private static final int rllToggleSlot = 19;
    private static final int rrlToggleSlot = 28;
    private static final int passiveToggleSlot = 37;
    private static final int backSlot = 45;
    private static final int resetSlot = 53;
    private static final int[] levelItemLocations = {
            3, 4, 5, 6, 7,
            12, 13, 14, 15, 16,
            21, 22, 23, 24, 25,
            30, 31, 32, 33, 34,
            39, 40, 41, 42, 43
    };

    public static void openGUI(Player player) {
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());

        ItemStack rrrItem = stats.getSkillRRR().getItem();
        ItemStack rlrItem = stats.getSkillRLR().getItem();
        ItemStack rllItem = stats.getSkillRLL().getItem();
        ItemStack rrlItem = stats.getSkillRRL().getItem();
        ItemStack passiveItem = stats.getPassive().getItem();

        Inventory inv = Bukkit.createInventory(player, 9 * 6, invName + " you currently have " + stats.getPoints() + " points and your level cap is at " + stats.getMaxLevel());
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, ItemUtils.getItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " "));
        }
        inv.setItem(rrrSlot, rrrItem);
        inv.setItem(rlrSlot, rlrItem);
        inv.setItem(rllSlot, rllItem);
        inv.setItem(rrlSlot, rrlItem);
        inv.setItem(passiveSlot, passiveItem);
        if (stats.getRRRActive()) {
            inv.setItem(rrrToggleSlot, rrrOnItem);
        } else {
            inv.setItem(rrrToggleSlot, rrrOffItem);
        }
        if (stats.getRLRActive()) {
            inv.setItem(rlrToggleSlot, rlrOnItem);
        } else {
            inv.setItem(rlrToggleSlot, rlrOffItem);
        }
        if (stats.getRLLActive()) {
            inv.setItem(rllToggleSlot, rllOnItem);
        } else {
            inv.setItem(rllToggleSlot, rllOffItem);
        }
        if (stats.getRRLActive()) {
            inv.setItem(rrlToggleSlot, rrlOnItem);
        } else {
            inv.setItem(rrlToggleSlot, rrlOffItem);
        }
        if (stats.getPassiveActive()) {
            inv.setItem(passiveToggleSlot, passiveOnItem);
        } else {
            inv.setItem(passiveToggleSlot, passiveOffItem);
        }
        inv.setItem(resetSlot, resetItem);
        //inv.setItem(backSlot, back);


        List<ItemStack> levelItems = getAllLevelItems(stats);
        for (int i = 0; i < levelItemLocations.length; i++) {
            inv.setItem(levelItemLocations[i], levelItems.get(i));
        }

        player.openInventory(inv);
    }

    private static List<ItemStack> getAllLevelItems(PlayerStats stats) {
        List<ItemStack> levelItems = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Skill currentSkill = stats.getSkillRRR();
            int level = stats.getRRRLevel();
            switch (i) {
                case 1 -> {
                    level = stats.getRLRLevel();
                    currentSkill = stats.getSkillRLR();
                }
                case 2 -> {
                    level = stats.getRLLLevel();
                    currentSkill = stats.getSkillRLL();
                }
                case 3 -> {
                    level = stats.getRRLLevel();
                    currentSkill = stats.getSkillRRL();
                }
                case 4 -> {
                    level = stats.getPassiveLevel();
                    currentSkill = stats.getPassive();
                }
            }
            for (int j = 0; j < 5; j++) {
                Material type = null;
                String name = null;
                switch (level) {
                    case 1:
                        type = Material.RED_TERRACOTTA;
                        name = ChatColor.RED + "" + ChatColor.BOLD + "[Level 1]";
                        break;
                    case 2:
                        type = Material.ORANGE_TERRACOTTA;
                        name = ChatColor.GOLD + "" + ChatColor.BOLD + "[Level 2]";
                        break;
                    case 3:
                        type = Material.YELLOW_TERRACOTTA;
                        name = ChatColor.YELLOW + "" + ChatColor.BOLD + "[Level 3]";
                        break;
                    case 4:
                        type = Material.LIME_TERRACOTTA;
                        name = ChatColor.GREEN + "" + ChatColor.BOLD + "[Level 4]";
                        break;
                    case 5:
                        type = Material.GREEN_TERRACOTTA;
                        name = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Level 5]";
                        break;
                }
                if (j < level) {
                    levelItems.add(ItemUtils.getItem(new ItemStack(type), name, ChatColor.GRAY + currentSkill.getLevelDescription(j)));
                } else if (j == level) {
                    levelItems.add(ItemUtils.getItem(new ItemStack(Material.STONE_BUTTON), ChatColor.DARK_AQUA + "" + "Upgrade for 1 skill point to gain these perks:", ChatColor.GRAY + currentSkill.getLevelDescription(j)));
                } else {
                    levelItems.add(ItemUtils.getItem(new ItemStack(Material.POLISHED_BLACKSTONE_BUTTON), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "[Locked]"));
                }
            }
        }
        return levelItems;
    }

    public static void clickedGUI(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        switch (event.getSlot()) {
            // Switches
            case backSlot -> {
                /**
                AncestryGUI.openGUI(player);
                player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                return;**/
            }
            case resetSlot -> {
                stats.setRRRLevel(1);
                stats.setRLRLevel(1);
                stats.setRLLLevel(1);
                stats.setRRLLevel(1);
                stats.setPassiveLevel(1);
                stats.setRRRActive(true);
                stats.setRLRActive(true);
                stats.setRLLActive(true);
                stats.setRRLActive(true);
                stats.setPassiveActive(true);
                stats.setPoints(stats.getMaxPoints());
                stats.save();
                player.playSound(player, Sound.EVENT_RAID_HORN, 0.7f, 1f);
                openGUI(player);
            }
            // Toggles
            case rrrToggleSlot -> {
                stats.setRRRActive(!stats.getRRRActive());
                player.playSound(player, Sound.BLOCK_LAVA_POP, 1f, 1f);
            }
            case rlrToggleSlot -> {
                stats.setRLRActive(!stats.getRLRActive());
                player.playSound(player, Sound.BLOCK_LAVA_POP, 1f, 1f);
            }
            case rllToggleSlot -> {
                stats.setRLLActive(!stats.getRLLActive());
                player.playSound(player, Sound.BLOCK_LAVA_POP, 1f, 1f);
            }
            case rrlToggleSlot -> {
                stats.setRRLActive(!stats.getRRLActive());
                player.playSound(player, Sound.BLOCK_LAVA_POP, 1f, 1f);
            }
            case passiveToggleSlot -> {
                stats.setPassiveActive(!stats.getPassiveActive());
                player.playSound(player, Sound.BLOCK_LAVA_POP, 1f, 1f);
            }
            case 3 -> levelUp(SkillType.RRR, 1, player, stats);
            case 4 -> levelUp(SkillType.RRR, 2, player, stats);
            case 5 -> levelUp(SkillType.RRR, 3, player, stats);
            case 6 -> levelUp(SkillType.RRR, 4, player, stats);
            case 7 -> levelUp(SkillType.RRR, 5, player, stats);
            case 12 -> levelUp(SkillType.RLR, 1, player, stats);
            case 13 -> levelUp(SkillType.RLR, 2, player, stats);
            case 14 -> levelUp(SkillType.RLR, 3, player, stats);
            case 15 -> levelUp(SkillType.RLR, 4, player, stats);
            case 16 -> levelUp(SkillType.RLR, 5, player, stats);
            case 21 -> levelUp(SkillType.RLL, 1, player, stats);
            case 22 -> levelUp(SkillType.RLL, 2, player, stats);
            case 23 -> levelUp(SkillType.RLL, 3, player, stats);
            case 24 -> levelUp(SkillType.RLL, 4, player, stats);
            case 25 -> levelUp(SkillType.RLL, 5, player, stats);
            case 30 -> levelUp(SkillType.RRL, 1, player, stats);
            case 31 -> levelUp(SkillType.RRL, 2, player, stats);
            case 32 -> levelUp(SkillType.RRL, 3, player, stats);
            case 33 -> levelUp(SkillType.RRL, 4, player, stats);
            case 34 -> levelUp(SkillType.RRL, 5, player, stats);
            case 39 -> levelUp(SkillType.PASSIVE, 1, player, stats);
            case 40 -> levelUp(SkillType.PASSIVE, 2, player, stats);
            case 41 -> levelUp(SkillType.PASSIVE, 3, player, stats);
            case 42 -> levelUp(SkillType.PASSIVE, 4, player, stats);
            case 43 -> levelUp(SkillType.PASSIVE, 5, player, stats);
        }
        openGUI(player);
        ((Player) event.getWhoClicked()).updateInventory();
    }

    public static void levelUp(SkillType type, int level, Player player, PlayerStats stats) {
        if (level > stats.getMaxLevel()) return;
        switch (type) {
            case RRR -> {
                if (level != stats.getRRRLevel() + 1) return;
                stats.setRRRLevel(level);
            }
            case RLR -> {
                if (level != stats.getRLRLevel() + 1) return;
                stats.setRLRLevel(level);
            }
            case RLL -> {
                if (level != stats.getRLLLevel() + 1) return;
                stats.setRLLLevel(level);
            }
            case RRL -> {
                if (level != stats.getRRLLevel() + 1) return;
                stats.setRRLLevel(level);
            }
            case PASSIVE -> {
                if (level != stats.getPassiveLevel() + 1) return;
                stats.setPassiveLevel(level);
            }
        }
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }
 }