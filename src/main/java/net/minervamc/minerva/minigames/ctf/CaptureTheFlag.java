package net.minervamc.minerva.minigames.ctf;

import java.util.List;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.minigames.Minigame;
import org.bukkit.entity.Player;

public class CaptureTheFlag extends Minigame {
    public static boolean playing = false;

    public static void start(List<Player> players) {
        saveAndClearInventories(players);
        playing = true;
    }

    public static void end(List<Player> players) {
        loadInventories(players);
        playing = false;
    }
}
