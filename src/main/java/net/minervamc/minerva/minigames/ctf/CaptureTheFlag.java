package net.minervamc.minerva.minigames.ctf;

import java.time.Duration;
import java.util.*;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.Minigame;
import net.minervamc.minerva.utils.FastUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.slf4j.Logger;

public class CaptureTheFlag extends Minigame {
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    @Getter
    public static boolean playing = false;
    public static boolean starting = false;
    public static boolean preparePhase = false;

    private static final List<Player> queue = new ArrayList<>();
    private static final List<Player> inGame = new ArrayList<>();
    private static final List<Player> blue = new ArrayList<>();
    private static final List<Player> red = new ArrayList<>();

    private static final Map<Entity, Player> traps = new HashMap<>();
    private static final Map<Player, String> kits = new HashMap<>();

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
            if (p.equals(player)) {
                p.sendActionBar(Component.text("You joined the queue!", NamedTextColor.GREEN));
                return;
            }
            p.sendActionBar(Component.text(player.getName() + " joined the queue!", NamedTextColor.GREEN));
        });
        if(queue.size() > 4) start();
    }

    public static void removeQueue(Player player) {
        if(isInGame(player)) return;
        if(!isInQueue(player)) {
            player.sendMessage(Component.text("Not in queue"));
            return;
        }
        queue.remove(player);
        player.sendActionBar(Component.text("You left the queue!", NamedTextColor.RED));
        queue.forEach(p-> p.sendActionBar(Component.text(player.getName() + " left the queue!", NamedTextColor.RED)));
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
        preparePhase = true;

        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (count > 0) {
                    TextColor color = switch (count) {
                        case 1 -> TextColor.color(0xFF0024);
                        case 2 -> TextColor.color(0xFF4500);
                        case 3 -> TextColor.color(0xFFA500);
                        case 4 -> TextColor.color(0xFFFF00);
                        case 5 -> NamedTextColor.GREEN;
                        default -> throw new IllegalStateException("Unexpected value: " + count);
                    };
                    queue.forEach(player -> {
                        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                        Title title = Title.title(
                                Component.text(count + "", color),
                                Component.text("seconds before the game starts."), times
                        );

                        player.showTitle(title);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    });
                    count--;
                }
                else {
                    ScoreboardManager manager = Bukkit.getScoreboardManager();
                    scoreboard = manager.getNewScoreboard();

                    blueTeam = scoreboard.registerNewTeam("Blue");
                    blueTeam.color(NamedTextColor.BLUE);
                    blueTeam.setAllowFriendlyFire(false);

                    redTeam = scoreboard.registerNewTeam("Red");
                    redTeam.color(NamedTextColor.RED);
                    redTeam.setAllowFriendlyFire(false);

                    inGame.addAll(queue);
                    Collections.shuffle(inGame);
                    queue.clear();

                    saveAndClearInventories(inGame);
                    inGame.forEach(player -> {
                        player.showTitle(Title.title(Component.text("Game Started!", NamedTextColor.GREEN), Component.empty()));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        player.setGameMode(GameMode.ADVENTURE);
                        kits(player, "ctf");
                    });

                    // Put into Set for randomization
                    int i = 0;
                    for (Player player : inGame) {
                        boolean randBool = new Random().nextBoolean();
                        if ((i % 2 == 0) == randBool) {
                            blue.add(player);
                            blueTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage(
                                    Component.text("You are on the ")
                                            .append(Component.text("blue", NamedTextColor.BLUE, TextDecoration.BOLD))
                                            .append(Component.text(" team!"))
                            );
                            player.getInventory().addItem(blueFlagBreaker());
                        } else {
                            red.add(player);
                            redTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage(
                                    Component.text("You are on the ")
                                            .append(Component.text("red", NamedTextColor.RED, TextDecoration.BOLD))
                                            .append(Component.text(" team!"))
                            );
                            player.getInventory().addItem(redFlagBreaker());
                        }
                        i++;
                    }
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
        blueFlagBreakerCr.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        blueFlagBreakerCr.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return ItemCreator.getBreakable(blueFlagBreakerCr.build(), Material.RED_BANNER);
    }

    private static ItemStack redFlagBreaker() {
        ItemCreator redFlagBreakerCr = ItemCreator.get(Material.WOODEN_AXE);
        redFlagBreakerCr.setName(TextContext.format("Flag Breaker", false).color(NamedTextColor.GOLD));
        redFlagBreakerCr.setLore(List.of(
                TextContext.formatLegacy("&7Use this to break", false),
                TextContext.formatLegacy("&7the &1blue &7team's flag", false)
        ));
        redFlagBreakerCr.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        redFlagBreakerCr.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return ItemCreator.getBreakable(redFlagBreakerCr.build(), Material.BLUE_BANNER);
    }

    public static void stop(String winningTeam) {
        if(!playing) return;
        inGame.forEach(player -> {
            boolean isRed = red.contains(player);
            Component title;
            Component subtitle;
            if (winningTeam.equals("blue")) {
                subtitle = Component.text("The RED team won!", NamedTextColor.GOLD);
                if (!isRed) {
                    title = Component.text("You Won!", NamedTextColor.BLUE);
                } else {
                    title = Component.text("You Lost!", NamedTextColor.RED);
                }
            } else if (winningTeam.equals("red")) {
                subtitle = Component.text("The RED team won!", NamedTextColor.GOLD);
                if (isRed) {
                    title = Component.text("You Won!", NamedTextColor.RED);
                    // player.playSound(); //  0.8 0.8
                    // player.playSound(); // ENDER DRAGON HURT 0.3 0.7
                    // player.playSound(); // CREEPER PRIMED 0.4 0.6
                } else {
                    title = Component.text("You Lost!", NamedTextColor.BLUE);
                    // player.playSound(); // ANVIL 0.8 0.8
                    // player.playSound(); // ENDER DRAGON HURT 0.3 0.7
                    // player.playSound(); // CREEPER PRIMED 0.4 0.6
                }
            } else {
                subtitle = Component.text("The game was ended early", NamedTextColor.GOLD);
                title = Component.text("Game Over!", NamedTextColor.RED);
            }
            player.showTitle(Title.title(title, subtitle));
            GameMode gameMode = player.getPreviousGameMode() == null ? GameMode.SURVIVAL : player.getPreviousGameMode();
            player.setGameMode(gameMode);
        });
        //LOGGER.info("Ended");
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

    public static void addTrap(Entity trap, Player player) {
        player.sendMessage("placing trap");
        traps.put(trap, player);
    }

    public static void defuseTrap(Entity trap, Player player) {
        traps.remove(trap);
    }

    public static void triggerTrap(Entity trap, Player target) {
        Player setter = traps.getOrDefault(trap, null);
        if (inBlueTeam(setter) && inBlueTeam(target) || !inBlueTeam(setter) && !inBlueTeam(target)) {
            return;
        }
        target.sendMessage("You've been hit by a trap from " + setter.getName() + "!");
        trap.getWorld().createExplosion(trap, 3);
        traps.remove(trap);
        trap.getPassengers().forEach(pass -> {
            trap.removePassenger(pass);
            pass.remove();
        });
        trap.remove();
    }

    public static void changedRegion(String region1, String region2, PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (region2.contains("barrier") && preparePhase) {
            event.setCancelled(true);
        } else if (region2.contains("blue") && blue.contains(player)) {  // if a red team crosses over into blue territory
            if (!player.getInventory().contains(redFlag())) return;
            stop("blue");
        } else if (region2.contains("red") && red.contains(player)) {
            if (!player.getInventory().contains(blueFlag())) return;
            stop("red");
        }
    }

    public static void skillCast(Player player) {
        String kit = kits.getOrDefault(player, null);
        if (kit == null) return;
        switch (kit) {
            case "scout":
                scoutSkill(player);
                break;
            case "attacker":
                attackerSkill(player);
                break;
            case "defender":
                defenderSkill(player);
                break;
            default:
                LOGGER.error("Invalid kit: {}. Error in skillCast() function in CaptureTheFlag.java.", kit);
        }
    }

    private static void scoutSkill(Player player) {
        player.sendMessage("You are a scout and you are, in fact, casting a skill."); // Point towards banner
    }

    private static void attackerSkill(Player player) {
        player.sendMessage("You are an attacker and you are, in fact, casting a skill."); // Dash
        Vector dir = player.getLocation().getDirection();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.2f, 1.3f);
        dir.setY(1);
        player.setVelocity(dir.normalize().multiply(2));
    }

    private static void defenderSkill(Player player) {
        player.sendMessage("You are a defender and you are, in fact, casting a skill."); // Parry
    }

    public static void kitChoose(Player player, String kit) {
        kits.put(player, kit);
    }
}