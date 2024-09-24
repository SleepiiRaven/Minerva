package net.minervamc.minerva.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.lib.command.*;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.minigames.ctf.RegionManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

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

    @ICommand(user = CommandUser.ALL,
            permission = "ctf.forcestart")
    public void forcestart(CommandContext context) {
        CaptureTheFlag.start();
    }

    @ICommand(user = CommandUser.ALL,
            permission = "ctf.forcestop")
    public void forcestop(CommandContext context) {
        CaptureTheFlag.stop();
    }

    @ICommand(user = CommandUser.ALL,
            permission = "ctf.regionmode",
            tabCompleter = "regionTabCompleter")
    public void region(CommandContext context) {
        Player player = context.getPlayer();
        assert player != null;

        String[] args = context.args();
        // 0 = region
        // 1 = ?

        switch (args[1]) {
            case "selectmode" -> RegionManager.enterSelectMode(player);
            case "list" -> {
                List<String> regions = RegionManager.listRegions();
                if(regions.isEmpty()) {
                    player.sendMessage(Component.text("No available regions"));
                    return;
                }
                player.sendMessage(Component.text("Available regions: ", NamedTextColor.AQUA));
                regions.forEach(s -> player.sendMessage(Component.text("- " + s, NamedTextColor.GRAY)));
            }
            case "save" -> {
                if(args.length != 3) {
                    player.sendMessage(Component.text("Please provide a name!", NamedTextColor.RED));
                    return;
                }
                String name = args[2];
                RegionManager.saveRegion(player, name);
            }
        }
    }

    @ITabComplete(name = "regionTabCompleter")
    public List<String> regionTabCompleter(CommandContext context) {
        Player player = context.getPlayer();
        assert player != null;

        String[] args = context.args();
        if(args.length == 2) {
            return Stream.of("selectmode", "list", "save").filter(s -> s.startsWith(args[1])).toList();
        }
        return List.of();
    }

    public static void register(JavaPlugin plugin) {
        Command.register(plugin, new CtfCommand());
    }
}
