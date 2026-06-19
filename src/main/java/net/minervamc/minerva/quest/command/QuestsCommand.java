package net.minervamc.minerva.quest.command;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.lib.command.Command;
import net.minervamc.minerva.lib.command.CommandContext;
import net.minervamc.minerva.lib.command.CommandUser;
import net.minervamc.minerva.lib.command.ICommand;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.gui.QuestJournalMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The single quest command (alias {@code /quest}). Player-facing:
 * <ul>
 *     <li>{@code /quests} — open your quest journal,</li>
 *     <li>{@code /quests start <id>} — start a quest (respecting prerequisites); admins wire this to a
 *         Citizens NPC's command trait so talking to the NPC starts the quest.</li>
 * </ul>
 * Admin creation lives under {@code /quests creator} (permission {@code minerva.quest.admin}):
 * {@code creator} opens the builder, {@code creator record} stops the active recording,
 * {@code creator start <id> [player]} force-starts, {@code creator reload} reloads definitions.
 */
public class QuestsCommand extends Command {
    private static final String ADMIN = "minerva.quest.admin";

    public QuestsCommand() {
        super("quests", "Quests and the quest creator", "/quests", List.of("quest", "journal"));
    }

    @ICommand(noArgs = true, user = CommandUser.PLAYER)
    public void open(CommandContext context) {
        new QuestJournalMenu(context.getPlayer()).open(context.getPlayer());
    }

    @ICommand(name = "start", user = CommandUser.PLAYER)
    public void start(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.args();
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /quests start <id>", NamedTextColor.RED));
            return;
        }
        QuestManager.startQuest(player, args[1]);
    }

    @ICommand(name = "creator", user = CommandUser.PLAYER, permission = ADMIN,
            permissionMessage = "&cYou don't have permission to use the quest creator.")
    public void creator(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.args();
        String sub = args.length >= 2 ? args[1].toLowerCase() : "";
        switch (sub) {
            case "record" -> {
                if (QuestManager.isRecordingNpc(player)) {
                    QuestManager.finishNpcRecording(player);
                } else if (QuestManager.isEditingCamera(player)) {
                    QuestManager.finishCameraEdit(player);
                } else {
                    player.sendMessage(Component.text("Nothing is recording.", NamedTextColor.YELLOW));
                }
            }
            case "start" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /quests creator start <id> [player]", NamedTextColor.RED));
                    return;
                }
                Player target = args.length >= 4 ? Bukkit.getPlayer(args[3]) : player;
                if (target == null) {
                    player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return;
                }
                QuestManager.startQuest(target, args[2]);
            }
            case "reload" -> {
                QuestManager.loadAll();
                player.sendMessage(Component.text("Reloaded quest definitions.", NamedTextColor.GREEN));
            }
            default -> QuestManager.openBuilder(player);
        }
    }

    public static void register(JavaPlugin plugin) {
        Command.register(plugin, new QuestsCommand());
    }
}
