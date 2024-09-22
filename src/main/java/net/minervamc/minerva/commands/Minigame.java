package net.minervamc.minerva.commands;

import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Minigame implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /minigame <minigame name> <player1>");
            return false;
        }

        if (args[0] == "ctf") {
            CaptureTheFlag.start(args[1].to);
            return true;
        }

        return true;
    }
}