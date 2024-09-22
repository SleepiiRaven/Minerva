package net.minervamc.minerva.commands;

import net.kyori.adventure.text.Component;
import net.minervamc.minerva.lib.command.Command;
import net.minervamc.minerva.lib.command.CommandContext;
import net.minervamc.minerva.lib.command.CommandUser;
import net.minervamc.minerva.lib.command.ICommand;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CtfCommand extends Command {

    public CtfCommand() {
        super("ctf", "", "", List.of());
    }

    @ICommand(user = CommandUser.PLAYER)
    public void joinqueue(CommandContext context) {
        Player player = context.getPlayer();
        CaptureTheFlag.addQueue(player);

        assert player != null;
        player.sendMessage(Component.text("Added to ctf queue"));
    }

    @ICommand(user = CommandUser.PLAYER)
    public void leavequeue(CommandContext context) {
        Player player = context.getPlayer();
        CaptureTheFlag.removeQueue(player);

        assert player != null;
        player.sendMessage(Component.text("Removed from ctf queue"));
    }

    @ICommand(user = CommandUser.ALL)
    public void start(CommandContext context) {
        CaptureTheFlag.start();

    }

    @ICommand(user = CommandUser.ALL)
    public void end(CommandContext context) {
        CaptureTheFlag.end();

    }

    public static void register(JavaPlugin plugin) {
        Command.register(plugin, new CtfCommand());
    }
}