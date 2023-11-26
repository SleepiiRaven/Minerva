package net.minervamc.minerva.commands;

import java.util.Arrays;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
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
        }
        if (args.length >= 3 && args[0].equals("set")) {
            if (args[1].equals("maxLevel") && commandSender instanceof Player player && player.hasPermission("minerva.skills.set")) {
                if (1 <= Integer.parseInt(args[2]) && Integer.parseInt(args[2]) <= 5) {
                    PlayerStats.getStats(player.getUniqueId()).setMaxLevel(Integer.parseInt(args[2]));
                }
            }
        }
        return true;
    }

    private void setSkills(Player player, Skill rrr, Skill rlr, Skill rll, Skill rrl, Skill passive, CommandSender commandSender, String name) {
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        playerStats.setRRRLevel(1);
        playerStats.setRLRLevel(1);
        playerStats.setRLLLevel(1);
        playerStats.setRRLLevel(1);
        playerStats.setPassiveLevel(1);
        playerStats.setRRRActive(true);
        playerStats.setRLRActive(true);
        playerStats.setRLLActive(true);
        playerStats.setRRLActive(true);
        playerStats.setPassiveActive(true);
        playerStats.setPoints(playerStats.getMaxPoints());
        playerStats.setSkillRRR(rrr);
        playerStats.setSkillRLR(rlr);
        playerStats.setSkillRLL(rll);
        playerStats.setSkillRRL(rrl);
        playerStats.setPassive(passive);
        playerStats.save();
    }
}
