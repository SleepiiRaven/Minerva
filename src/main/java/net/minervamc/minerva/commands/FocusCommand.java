package net.minervamc.minerva.commands;

import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FocusCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull org.bukkit.command.Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player player) {
            SkillUtils.setFocus(player.getInventory().getItemInMainHand());
            return true;
        } else {
            commandSender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You can't execute that command if you're not a player!");
            return false;
        }
    }
}
