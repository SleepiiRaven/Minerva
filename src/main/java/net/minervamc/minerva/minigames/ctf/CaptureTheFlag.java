package net.minervamc.minerva.minigames.ctf;

import java.util.*;

import net.minervamc.minerva.minigames.Minigame;
import org.bukkit.entity.Player;

public class CaptureTheFlag extends Minigame {
    public static boolean playing = false;

    private static final List<Player> queue = new ArrayList<>();
    private static final List<Player> inGame = new ArrayList<>();

    public static void addQueue(Player player) {
        queue.add(player);
    }

    public static void removeQueue(Player player) {
        queue.remove(player);
    }

    public static void start() {
        if (playing) return;
        playing = true;
        inGame.addAll(queue);
        queue.clear();

        saveAndClearInventories(inGame);
        inGame.forEach(player -> player.sendMessage("Game Started!"));
    }

    public static void end() {
        loadInventories(inGame);
        playing = false;
    }
}
