package net.minervamc.minerva.minigames.ctf;

import java.time.Duration;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.Minigame;
import net.minervamc.minerva.utils.FastUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CaptureTheFlag extends Minigame {
    public static boolean playing = false;
    public static boolean starting = false;

    private static final List<Player> queue = new ArrayList<>();
    private static final List<Player> inGame = new ArrayList<>();
    private static List<Player> inGameTemp = new ArrayList<>();
    private static final List<Player> blue = new ArrayList<>();
    private static final List<Player> red = new ArrayList<>();

    public static void addQueue(Player player) {
        if (playing || starting) {
            player.sendMessage(Component.text("Game has already started!"));
            return;
        }
        if(isInQueue(player)) {
            player.sendMessage(Component.text("Already in queue"));
            return;
        }
        queue.add(player);
        player.sendMessage(Component.text("Added to ctf queue"));
        if(queue.size() > 4) start();
    }

    public static void removeQueue(Player player) {
        if (playing || starting) {
            player.sendMessage(Component.text("Game has already started!"));
            return;
        }
        if(!isInQueue(player)) {
            player.sendMessage(Component.text("Not in queue"));
            return;
        }
        queue.remove(player);
        player.sendMessage(Component.text("Removed from ctf queue"));
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
                        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                        Title title = Title.title(Component.text(count + ""), Component.text("seconds before the game starts."), times);

                        player.showTitle(title);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    });
                    count--;
                } else {
                    saveAndClearInventories(inGame);
                    inGame.forEach(player -> {
                        player.showTitle(Title.title(Component.text("Game Started!", NamedTextColor.GREEN), Component.empty()));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        kits(player, "ctf");
                    });

                    float fHalf = count / (float) 2.0;
                    if (fHalf % 1 == 0) {
                        fHalf += 0.5;
                    }
                    int half = Math.round(fHalf);
                    inGameTemp = inGame;
                    for (int i = 0; i < half; i++) {
                        int random = FastUtils.randomIntInRange(0, inGameTemp.size() - 1);
                        Player randomPlayer = inGameTemp.get(random);
                        blue.add(randomPlayer);
                        inGameTemp.remove(random);
                    }
                    while (!inGameTemp.isEmpty()) {
                        int random = FastUtils.randomIntInRange(0, inGameTemp.size() - 1);
                        Player randomPlayer = inGameTemp.get(random);
                        red.add(randomPlayer);
                        inGameTemp.remove(random);
                    }

                    if (!blue.isEmpty()) {
                        int random = FastUtils.randomIntInRange(0, blue.size() - 1);
                        Player randomBlue = blue.get(random);
                        ItemCreator blue_flag = ItemCreator.get(new ItemStack(Material.BLUE_BANNER));
                        blue_flag.setName(Component.text("Blue Flag", NamedTextColor.DARK_BLUE));
                        randomBlue.getInventory().addItem(ItemCreator.getPlaceable(blue_flag.build(), Material.MYCELIUM));
                    }
                    if (!red.isEmpty()) {
                        int random = FastUtils.randomIntInRange(0, red.size() - 1);
                        Player randomRed = red.get(random);
                        ItemCreator red_flag = ItemCreator.get(new ItemStack(Material.RED_BANNER));
                        red_flag.setName(Component.text("Red Flag", NamedTextColor.RED));
                        randomRed.getInventory().addItem(ItemCreator.getPlaceable(red_flag.build(), Material.PODZOL));
                    }

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
        blue.clear();
        red.clear();
        inGameTemp.clear();
        playing = false;
        starting = false;
    }
}