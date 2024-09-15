package net.minervamc.minerva.commands;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.guis.SkillsGUI;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) { // If the argument length is zero... /mskills
            if (commandSender instanceof Player player) { // If the sender is a player
                AncestryGUI.openGUI(player);
            } else { // /mskills with no
                commandSender.sendMessage(ChatColor.RED + "Invalid command for non-player. Correct usage to open GUI for player that isn't you: /mskills [gui] [player]");
            }
            return true;
        } else if (args.length == 1 && commandSender.hasPermission("minerva.skills.opengui.others")) {
            if (Bukkit.getPlayer(args[0]) != null && Bukkit.getPlayer(args[0]).isOnline()) {
                AncestryGUI.openGUI(Bukkit.getPlayer(args[0]));
            } else {
                commandSender.sendMessage("Either that player isn't online right now or that player doesn't exist.");
            }
        } else if (args.length >= 3 && args[0].equals("set")) {
            // Right now it's /mskills set [something] [something] ...
            // We have to check if in that ... there's a player we want to set something for.
            Player player;
            String stat;
            String value;
            if (args.length == 4 && Bukkit.getPlayer(args[1]) != null) { // /mskills set [Player] [something] [something]
                if (Bukkit.getPlayer(args[1]).isOnline()) {
                    if (commandSender.hasPermission("minerva.skills.set.others")) {
                        player = Bukkit.getPlayer(args[1]);
                        stat = args[2];
                        value = args[3];
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "You do not have permission to set other players' stats.");
                        return true;
                    }
                } else {
                    commandSender.sendMessage(ChatColor.YELLOW + "That player isn't online.");
                    return true;
                }
            } else if (commandSender instanceof Player) {
                player = (Player) commandSender;
                stat = args[1];
                value = args[2];
            } else {
                commandSender.sendMessage(ChatColor.RED + "You can not run this command as you are not a player or the player you are running this command on does not exist. Correct syntax for setting other players' stats: /mskills set [player] [maxlevel/heritage] [value]");
                return true;
            }

            if (player == null) return false;
            // Now we know the player!
            if (stat.equalsIgnoreCase("maxlevel") && commandSender.hasPermission("minerva.skills.set")) { // /mskills set <player> maxLevel [value]
                int maxLevel = Integer.parseInt(value);
                if (1 <= maxLevel && maxLevel <= 5) {
                    PlayerStats.getStats(player.getUniqueId()).setMaxLevel(maxLevel);
                    player.sendMessage(ChatColor.YELLOW + "Your max level is now " + maxLevel + "!");
                } else {
                    commandSender.sendMessage(maxLevel + " is out of the range of 1 - 5.");
                }
            } else if (stat.equalsIgnoreCase("heritage")) {
                if (HeritageType.fromString(value) == HeritageType.NONE) {
                    return true;
                }
                PlayerStats.getStats(player.getUniqueId()).setHeritage(HeritageType.fromString(value));
                SkillUtils.setDefaultSkills(HeritageType.fromString(value), player);
                SkillsGUI.openGUI(player);
            }
        }
        return true;
    }
}
