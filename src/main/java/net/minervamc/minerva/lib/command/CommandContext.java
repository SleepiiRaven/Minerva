package net.minervamc.minerva.lib.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public record CommandContext(CommandSender sender, Command command, String[] args) {
    @Nullable
    public Player getSenderAsPlayer() {
        if (sender instanceof Player) return (Player) sender;
        else return null;
    }

    public boolean hasArgs() {
        return args.length > 0;
    }
}