package net.minervamc.minerva.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
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
        if (CaptureTheFlag.inSameTeam(player, checkIsInParty)) return true;

        if (partyList(player) == null) return false;

        return partyList(player).contains(checkIsInParty);
    }

    public static void invitePlayerParty(Player partyLeader, Player addedPlayer) {
        partyLeader.sendMessage(ChatColor.YELLOW + "You invited " + addedPlayer.getName() + " to your party!");

        TextComponent message = new TextComponent("HERE");
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click here to join " + partyLeader.getName() + "'s party!")));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party join " + partyLeader.getName()));
        message.setBold(true);

        TextComponent firstPartMessage = new TextComponent("You have been invited to join " + partyLeader.getName() + "'s party! Click ");
        firstPartMessage.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        TextComponent lastPartMessage = new TextComponent(" to accept their invite!");
        lastPartMessage.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        firstPartMessage.addExtra(message);
        firstPartMessage.addExtra(lastPartMessage);

        addedPlayer.sendMessage(ChatColor.BLUE + "------------");
        addedPlayer.sendMessage(firstPartMessage);
        addedPlayer.sendMessage(ChatColor.BLUE + "------------");


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
        } else {
            //weren't invited
        }
    }

    public static void removePlayerParty(Player partyLeader, Player removedPlayer) {
        if (parties.getOrDefault(partyLeader, null) != null) {
            parties.get(partyLeader).remove(removedPlayer);
        }
    }

    public static void terminateParty(Player partyLeader) {
        if (partyList(partyLeader) == null) return;

        for (Player player : partyList(partyLeader)) {
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
