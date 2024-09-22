package net.minervamc.minerva.minigames.ctf;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.minigames.Minigame;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CaptureTheFlag extends Minigame {
    public static boolean playing = false;
    public static boolean starting = false;

    private static final List<Player> queue = new ArrayList<>();
    private static final List<Player> inGame = new ArrayList<>();

    public static void addQueue(Player player) {
        queue.add(player);
        if(queue.size() > 4) start();
    }

    public static void removeQueue(Player player) {
        queue.remove(player);
    }

    public static boolean isInQueue(Player player) {
        return queue.contains(player);
    }

    public static void start() {
        if (playing || starting) return;
        starting = true;

        inGame.addAll(queue);
        queue.clear();

        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (count > 0) {
                    inGame.forEach(player -> {
                        player.sendTitle(count + "", "seconds before the game starts.", 0, 40, 0);


                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    });
                    count--;
                } else {
                    inGame.forEach(player -> {
                        player.showTitle(Title.title(Component.text("Game Started!", NamedTextColor.GREEN), Component.empty()));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    });
                    saveAndClearInventories(inGame);
                    playing = true;
                    starting = false;
                    this.cancel();
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0, 20);
    }

    public static void end() {
        inGame.forEach(player -> player.showTitle(Title.title(Component.text("Game Over!", NamedTextColor.RED), Component.empty())));
        loadInventories(inGame);
        inGame.clear();
        playing = false;
        starting = false;
    }
}