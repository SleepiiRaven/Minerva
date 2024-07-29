package net.minervamc.minerva.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Party {
    private static final Map<Player, List<Player>> parties = new HashMap<>();
    private static final Map<Player, List<Player>> invites = new HashMap<>();

    public static void createParty(Player player) {
        parties.put(player, new ArrayList<>());
        parties.get(player).add(player);
    }

    public static boolean isPlayerInPlayerParty(Player player, Player checkIsInParty) {
        List<Player> partyList = partyList(player);
        if (partyList == null || partyList.isEmpty()) return false;

        return partyList.contains(checkIsInParty);
    }

    public static void invitePlayerParty(Player partyLeader, Player addedPlayer) {
        partyLeader.sendMessage(ChatColor.YELLOW + "You invited " + addedPlayer.getName() + " to your party!");


        Component message = Component.text("HERE")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party join " + partyLeader.getName()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click here to join " + partyLeader.getName() + "'s party!")))
                .decorate(TextDecoration.BOLD);

        Component firstPartMessage = Component.text("You have been invited to join " + partyLeader.getName() + "'s party! Click ", NamedTextColor.YELLOW);
        Component lastPartMessage = Component.text(" to accept their invite!");
        firstPartMessage = firstPartMessage.append(message).append(lastPartMessage);

        addedPlayer.sendMessage(Component.text("------------", NamedTextColor.BLUE));
        addedPlayer.sendMessage(firstPartMessage);
        addedPlayer.sendMessage(Component.text("------------", NamedTextColor.BLUE));

        if (invites.getOrDefault(partyLeader, null) != null) {
            if (!(parties.get(partyLeader) != null && parties.get(partyLeader).contains(addedPlayer))) {
                invites.get(partyLeader).add(addedPlayer);
            }
        } else {
            invites.put(partyLeader, new ArrayList<>());
            invites.get(partyLeader).add(addedPlayer);
        }
    }

    public static void joinPlayerParty(Player partyLeader, Player addedPlayer) {
        if (invites.get(partyLeader).contains(addedPlayer)) {
            addedPlayer.sendMessage(ChatColor.YELLOW + "You joined " + partyLeader.getName() + "'s party!");
            partyLeader.sendMessage(ChatColor.YELLOW + addedPlayer.getName() + " joined your party!");

            List<Player> party = parties.getOrDefault(partyLeader, null);
            if (party != null && !party.contains(addedPlayer)) {
                parties.get(partyLeader).add(addedPlayer);
            } else if (party == null) {
                createParty(partyLeader);
                parties.get(partyLeader).add(addedPlayer);
            }
        }
    }

    public static void removePlayerParty(Player partyLeader, Player removedPlayer) {
        if (parties.getOrDefault(partyLeader, null) != null) {
            parties.get(partyLeader).remove(removedPlayer);
        }
    }

    public static void terminateParty(Player partyLeader) {
        List<Player> partyList = partyList(partyLeader);
        if(partyList == null || partyList.isEmpty()) return;

        for (Player player : partyList) {
            player.sendMessage(ChatColor.RED + "The party has been disbanded.");
        }
        parties.remove(partyLeader);
    }

    public static List<Player> partyList(Player partyMember) {
        for (List<Player> players : parties.values()) {
            if (players.contains(partyMember)) {
                return players;
            }
        }

        return null;
    }

    public static Player partyLeader(Player partyMember) {
        for (Player player : parties.keySet()) {
            if (parties.get(player).contains(partyMember)) {
                return player;
            }
        }
        return null;
    }

    public static boolean isInParty(Player player) {
        for (List<Player> party : parties.values()) {
            if (party.contains(player)) return true;
        }
        return false;
    }
}
