package net.minervamc.minerva.commands;

import java.util.Objects;
import net.minervamc.minerva.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PartyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player commander)) return false;
        if (strings.length > 0) {
            if (strings[0].equals("list")) {
                StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Players in your party: ");
                if (Party.partyList(commander) == null) {
                    commander.sendMessage(ChatColor.YELLOW + "You are not currently in a party.");
                    return true;
                }
                for (Player player : Objects.requireNonNull(Party.partyList(commander))) {
                    if (Objects.requireNonNull(Party.partyList(commander)).get(Objects.requireNonNull(Party.partyList(commander)).size() - 1) == player) message.append(player.getName());
                    else message.append(player.getName()).append(", ");
                }
                commander.sendMessage(message.toString());
                return true;
            }
            else if (strings[0].equals("remove")) {
                if (strings.length == 2 && Bukkit.getPlayer(strings[1]) != null) {
                    if (Party.isInParty(commander) && Party.partyLeader(commander) == commander) {
                        if (commander == Bukkit.getPlayer(strings[1])) {
                            commander.sendMessage(ChatColor.RED + "You can't remove yourself from your own party!");
                            return true;
                        }
                        commander.sendMessage(ChatColor.RED + "You removed " + strings[1] + " from your party.");
                        Party.removePlayerParty(commander, Bukkit.getPlayer(strings[1]));
                        return true;
                    } else {
                        commander.sendMessage(ChatColor.RED + "You are either not in a party or you are not the leader of your current party.");
                    }
                }
            }
            else if (strings[0].equals("join")) {
                if (strings.length == 2 && Bukkit.getPlayer(strings[1]) != null && Objects.requireNonNull(Bukkit.getPlayer(strings[1])).isOnline()) {
                    if (commander == Bukkit.getPlayer(strings[1])) return false;
                    if (!Party.isInParty(commander)) {
                        Party.joinPlayerParty(Bukkit.getPlayer(strings[1]), commander);
                    } else {
                        commander.sendMessage(ChatColor.RED + "You are already in a party. To leave your current party, perform the command /party leave.");
                    }
                    return true;
                }
            }
            else if (strings[0].equals("leave")) {
                if (Party.isInParty(commander)) {
                    if (Party.partyLeader(commander) == commander) {
                        Party.terminateParty(commander);
                        return true;
                    }
                    if (Party.partyList(commander) != null ) {
                        for (Player player : Party.partyList(commander)) {
                            if (player != commander) player.sendMessage(ChatColor.RED + command.getName() + " left the your party!");
                        }
                    }
                    Party.removePlayerParty(Party.partyLeader(commander), commander);
                    commander.sendMessage(ChatColor.RED + "You left the party!");

                } else {
                    commander.sendMessage(ChatColor.RED + "You are not in a party.");
                }
                return true;
            }
            else if (strings[0].equals("disband")) {
                if (Party.isInParty(commander) && Party.partyLeader(commander) == commander) {
                    Party.terminateParty(commander);
                    return true;
                } else {
                    commander.sendMessage(ChatColor.RED + "You are not in a party or you are not the leader of the party.");
                }
            }
            else if (Bukkit.getPlayer(strings[0]) != null) {
                if (!Party.isInParty(commander) || Party.partyLeader(commander) == commander) {
                    Player player = Bukkit.getPlayer(strings[0]);
                    if (player == commander) {
                        commander.sendMessage(ChatColor.RED + "You can't invite yourself to your own party!");
                        return true;
                    }
                    Party.invitePlayerParty(commander, player);
                    return true;
                } else {
                    commander.sendMessage(ChatColor.RED + "You are not the party leader!");
                }
            }
        }
        return false;
    }
}
