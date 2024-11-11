package net.minervamc.minerva.commands;

import java.util.List;
import net.minervamc.minerva.lib.command.Command;
import net.minervamc.minerva.lib.command.CommandContext;
import net.minervamc.minerva.lib.command.CommandUser;
import net.minervamc.minerva.lib.command.ICommand;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class FocusCommand extends Command {
    public FocusCommand() {
        super("focus", "", "", List.of());
    }

    @ICommand(user = CommandUser.PLAYER)
    public void set(CommandContext context) {
        assert context.getPlayer() != null;
        SkillUtils.setFocus(context.getPlayer().getInventory().getItemInMainHand());
    }

    @ICommand(user = CommandUser.PLAYER)
    public void remove(CommandContext context) {
        assert context.getPlayer() != null;
        SkillUtils.removeFocus(context.getPlayer().getInventory().getItemInMainHand());
    }

    public static void register(JavaPlugin plugin) {
        Command.register(plugin, new FocusCommand());
    }
}
