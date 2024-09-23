package net.minervamc.minerva.commands;

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
    public void join(CommandContext context) {
        Player player = context.getPlayer();
        assert player != null;

        CaptureTheFlag.addQueue(player);
    }

    @ICommand(user = CommandUser.PLAYER)
    public void leave(CommandContext context) {
        Player player = context.getPlayer();
        assert player != null;

        CaptureTheFlag.removeQueue(player);
    }

    @ICommand(user = CommandUser.ALL)
    public void forcestart(CommandContext context) {
        CaptureTheFlag.start();
    }

    @ICommand(user = CommandUser.ALL)
    public void forcestop(CommandContext context) {
        CaptureTheFlag.end();
    }

    public static void register(JavaPlugin plugin) {
        Command.register(plugin, new CtfCommand());
    }
}
