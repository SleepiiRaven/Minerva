package net.minervamc.minerva.commands;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.command.Command;
import net.minervamc.minerva.lib.command.CommandContext;
import net.minervamc.minerva.lib.command.CommandUser;
import net.minervamc.minerva.lib.command.ICommand;
import org.bukkit.plugin.java.JavaPlugin;

public class NoCooldownCommand extends Command {
    public NoCooldownCommand() {
        super("cooldown", "", "", List.of());
    }


    @ICommand(user = CommandUser.PLAYER)
    public void off(CommandContext context) {
        Minerva.getInstance().getCdInstance().disableCooldowns(context.getPlayer().getUniqueId());
    }

    @ICommand(user = CommandUser.PLAYER)
    public void on(CommandContext context) {
        Minerva.getInstance().getCdInstance().enableCooldowns(context.getPlayer().getUniqueId());
    }


    public static void register(JavaPlugin plugin) {
        Command.register(plugin, new NoCooldownCommand());
    }
}
