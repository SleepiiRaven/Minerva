package net.minervamc.minerva.minigames.ctf;

import java.time.Duration;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.Minigame;
import net.minervamc.minerva.utils.FastUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.slf4j.Logger;

public class CaptureTheFlag extends Minigame {
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    public static boolean playing = false;
    public static boolean starting = false;

    private static final List<Player> queue = new ArrayList<>();
    private static final List<Player> inGame = new ArrayList<>();
    private static final List<Player> blue = new ArrayList<>();
    private static final List<Player> red = new ArrayList<>();

    // Team stuff
    private static Scoreboard scoreboard;
    private static Team blueTeam;
    private static Team redTeam;

    public static void addQueue(Player player) {
        if(isInGame(player)) return;
        if (playing) {
            player.sendMessage(Component.text("Game has already started!"));
            return;
        }
        if(isInQueue(player)) {
            player.sendMessage(Component.text("Already in queue"));
            return;
        }
        queue.add(player);
        queue.forEach(p-> {
            if (p.equals(player)) return;
            p.sendActionBar(Component.text(player.getName() + " joined the queue!", NamedTextColor.GREEN));
        });
        player.sendMessage(Component.text("Added to ctf queue"));
        if(queue.size() > 4) start();
    }

    public static void removeQueue(Player player) {
        if(isInGame(player)) return;
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

    public static boolean isInGame(Player player) {
        return inGame.contains(player);
    }

    public static void start() {
        if (playing || starting) return;
        starting = true;

        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (count > 0) {
                    queue.forEach(player -> {
                        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                        Title title = Title.title(Component.text(count + "", NamedTextColor.GREEN),
                                Component.text("seconds before the game starts.", NamedTextColor.GREEN), times);

                        player.showTitle(title);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    });
                    count--;
                } else {
                    ScoreboardManager manager = Bukkit.getScoreboardManager();
                    scoreboard = manager.getNewScoreboard();

                    blueTeam = scoreboard.registerNewTeam("Blue");
                    blueTeam.color(NamedTextColor.BLUE);
                    blueTeam.setAllowFriendlyFire(false);

                    redTeam = scoreboard.registerNewTeam("Red");
                    redTeam.color(NamedTextColor.RED);
                    redTeam.setAllowFriendlyFire(false);

                    inGame.addAll(queue);
                    queue.clear();

                    saveAndClearInventories(inGame);
                    inGame.forEach(player -> {
                        player.showTitle(Title.title(Component.text("Game Started!", NamedTextColor.GREEN), Component.empty()));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        player.setGameMode(GameMode.ADVENTURE);
                        kits(player, "ctf");
                    });

                    // Put into Set for randomization
                    Set<Player> inGameSet = new HashSet<>(inGame);
                    int i = 0;
                    for (Player player : inGameSet) {
                        boolean randBool = false;
                        if (FastUtils.randomIntInRange(0, 1) == 0) randBool = true;
                        if ((i % 2 == 0) == randBool) {
                            blue.add(player);
                            blueTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage(
                                    Component.text("You are on the ")
                                            .append(Component.text("blue", NamedTextColor.BLUE, TextDecoration.BOLD))
                                            .append(Component.text(" team!", NamedTextColor.WHITE))
                            );
                            player.getInventory().addItem(blueFlagBreaker());
                        } else {
                            red.add(player);
                            redTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage(
                                    Component.text("You are on the ")
                                            .append(Component.text("red", NamedTextColor.RED, TextDecoration.BOLD))
                                            .append(Component.text(" team!", NamedTextColor.WHITE))
                            );
                            player.getInventory().addItem(redFlagBreaker());
                        }
                        i++;
                    }

                    inGameSet.clear();

                    // Add blue flag to random player in blue team's inventory
                    if (!blue.isEmpty()) blue.get(FastUtils.randomIntInRange(0, blue.size() - 1)).getInventory().addItem(blueFlag());

                    // Same for red team
                    if (!red.isEmpty()) red.get(FastUtils.randomIntInRange(0, red.size() - 1)).getInventory().addItem(redFlag());

                    playing = true;
                    starting = false;
                    this.cancel();
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0, 20);
    }

    private static ItemStack blueFlag() {
        ItemCreator blueFlagCr = ItemCreator.get(Material.BLUE_BANNER);
        blueFlagCr.setName(TextContext.format("Blue Flag", false).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
        return ItemCreator.getPlaceable(blueFlagCr.build(), Material.MYCELIUM);
    }

    private static ItemStack redFlag() {
        ItemCreator redFlagCr = ItemCreator.get(Material.RED_BANNER);
        redFlagCr.setName(TextContext.format("Red Flag", false).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        return ItemCreator.getPlaceable(redFlagCr.build(), Material.PODZOL);
    }

    private static ItemStack blueFlagBreaker() {
        ItemCreator blueFlagBreakerCr = ItemCreator.get(Material.WOODEN_AXE);
        blueFlagBreakerCr.setName(TextContext.format("Flag Breaker", false).color(NamedTextColor.GOLD));
        blueFlagBreakerCr.setLore(List.of(
                TextContext.formatLegacy("&7Use this to break", false),
                TextContext.formatLegacy("&7the &cred &7team's flag", false)
        ));
        return ItemCreator.getBreakable(blueFlagBreakerCr.build(), Material.RED_BANNER);
    }

    private static ItemStack redFlagBreaker() {
        ItemCreator redFlagBreakerCr = ItemCreator.get(Material.WOODEN_AXE);
        redFlagBreakerCr.setName(TextContext.format("Flag Breaker", false).color(NamedTextColor.GOLD));
        redFlagBreakerCr.setLore(List.of(
                TextContext.formatLegacy("&7Use this to break", false),
                TextContext.formatLegacy("&7the &1blue &7team's flag", false)
        ));
        return ItemCreator.getBreakable(redFlagBreakerCr.build(), Material.BLUE_BANNER);
    }

    public static void end() {
        if(!playing) return;
        inGame.forEach(player -> player.showTitle(Title.title(Component.text("Game Over!", NamedTextColor.RED), Component.empty())));
        LOGGER.info("Ended");
        loadInventories(new ArrayList<>(inGame));
        inGame.clear();
        blue.clear();
        red.clear();
        if (blueTeam != null && scoreboard.getTeam("Blue") != null) blueTeam.unregister();
        if (redTeam != null && scoreboard.getTeam("Red") != null) redTeam.unregister();
        playing = false;
        starting = false;
    }

    public static boolean inBlueTeam(Player player) {
        return blue.contains(player);
    }

    public static boolean isPlaying() {
        return isPlaying();
    }
}