package net.minervamc.minerva.commands;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillModeToggle implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            PlayerStats.getStats(player.getUniqueId()).toggleSkillMode();

            if (PlayerStats.getStats(player).isSkillMode()) player.sendMessage(ChatColor.YELLOW + "Skill mode is now on. You can now use your heritage's skills.");
            else player.sendMessage(ChatColor.YELLOW + "Skill mode is now off. You can no longer use your heritage's skills until you use this command again.");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "You must be a player to use this command.");
        return true;
    }
}
