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
    private static Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

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

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        blueTeam = scoreboard.registerNewTeam("Blue");
        LOGGER.info("Team created blue");
        blueTeam.color(NamedTextColor.BLUE);
        blueTeam.setAllowFriendlyFire(false);

        redTeam = scoreboard.registerNewTeam("Red");
        LOGGER.info("Team created red");
        redTeam.color(NamedTextColor.RED);
        redTeam.setAllowFriendlyFire(false);

        LOGGER.info("All teams {}", scoreboard.getTeams().stream().map(Team::getName).toList());

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
                        player.setGameMode(GameMode.ADVENTURE);
                        kits(player, "ctf");
                    });

                    ItemCreator blueFlagCr = ItemCreator.get(Material.BLUE_BANNER);
                    blueFlagCr.setName(Component.text("Blue Flag", NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
                    ItemStack blueFlag = ItemCreator.getPlaceable(blueFlagCr.build(), Material.MYCELIUM);

                    ItemCreator redFlagCr = ItemCreator.get(Material.RED_BANNER);
                    redFlagCr.setName(Component.text("Red Flag", NamedTextColor.RED).decorate(TextDecoration.BOLD));
                    ItemStack redFlag = ItemCreator.getPlaceable(redFlagCr.build(), Material.PODZOL);

                    ItemCreator blueFlagBreakerCr = ItemCreator.get(Material.WOODEN_AXE);
                    ItemCreator redFlagBreakerCr = ItemCreator.get(Material.WOODEN_AXE);
                    blueFlagBreakerCr.setName(Component.text("Flag Breaker", NamedTextColor.GOLD));
                    blueFlagBreakerCr.setLore(List.of(
                            TextContext.formatLegacy("&7Use this to break", false),
                            TextContext.formatLegacy("&7the &cred &7team's flag", false)
                    ));
                    redFlagBreakerCr.setName(Component.text("Flag Breaker", NamedTextColor.GOLD));
                    redFlagBreakerCr.setLore(List.of(
                            TextContext.formatLegacy("&7Use this to break", false),
                            TextContext.formatLegacy("&7the &1blue &7team's flag", false)
                    ));
                    // Flag breaker for BLUE team to break RED flag
                    ItemStack blueFlagBreaker = ItemCreator.getBreakable(blueFlagBreakerCr.build(), Material.RED_BANNER);
                    ItemStack redFlagBreaker = ItemCreator.getBreakable(redFlagBreakerCr.build(), Material.BLUE_BANNER);

                    // Put into Set for randomization
                    Set<Player> inGameSet = new HashSet<>(inGame);
                    int i = 0;
                    for (Player player : inGameSet) {
                        if (i % 2 == 0) {
                            blue.add(player);
                            blueTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage("You are on the " + ChatColor.BLUE + "" + ChatColor.BOLD + "blue" + ChatColor.RESET + " team!");
                            player.getInventory().addItem(blueFlagBreaker);
                        } else {
                            red.add(player);
                            redTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage("You are on the " + ChatColor.RED + "" + ChatColor.BOLD + "red" + ChatColor.RESET + " team!");
                            player.getInventory().addItem(redFlagBreaker);
                        }
                        inGame.remove(player);
                        i++;
                    }

                    // Add blue flag to random player in blue team's inventory
                    if (!blue.isEmpty()) {
                        blue.get(FastUtils.randomIntInRange(0, blue.size() - 1)).getInventory().addItem(blueFlag);
                    }

                    // Same for red team
                    if (!red.isEmpty()) {
                        red.get(FastUtils.randomIntInRange(0, red.size() - 1)).getInventory().addItem(redFlag);
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
        if (blueTeam != null) blueTeam.unregister();
        if (redTeam != null) redTeam.unregister();
        playing = false;
        starting = false;
    }
}