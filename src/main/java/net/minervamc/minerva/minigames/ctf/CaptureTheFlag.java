package net.minervamc.minerva.minigames.ctf;

import java.time.Duration;
import java.util.*;

import fr.mrmicky.fastboard.FastBoard;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.storage.yaml.Config;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.Minigame;
import net.minervamc.minerva.utils.FastUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.slf4j.Logger;

public class CaptureTheFlag extends Minigame {
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    private static final Config regionConfig = new Config("ctf/spawns.yml");

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

    public static Location blueFlagLocation;
    public static Location redFlagLocation;
    private static Location blueSpawn;
    private static Location redSpawn;

    private static final HashMap<UUID, FastBoard> boards = new HashMap<>();
    private static BukkitTask scoreboardUpdater;
    private static int globalCountdown = 5;

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

    public static boolean hasFlag(Player player) {
        ItemStack flag = player.getInventory().getHelmet();
        if(flag == null) return false;
        return flag.getType().equals(Material.RED_BANNER) || flag.getType().equals(Material.BLUE_BANNER);
    }

    public static boolean hasRedFlag(Player player) {
        ItemStack flag = player.getInventory().getHelmet();
        if(flag == null) return false;
        return flag.getType().equals(Material.RED_BANNER);
    }

    public static boolean hasBlueFlag(Player player) {
        ItemStack flag = player.getInventory().getHelmet();
        if (flag == null) return false;
        return flag.getType().equals(Material.BLUE_BANNER);
    }

    public static void setSpawnPos(Location loc, String team) {
        if (team == null) return;
        if (team.equals("blue")) {
            blueSpawn = loc;
            try {
                regionConfig.set("spawn.blue.world", loc.getWorld().getName());
                regionConfig.set("spawn.blue.x", loc.getX());
                regionConfig.set("spawn.blue.y", loc.getY());
                regionConfig.set("spawn.blue.z", loc.getZ());
                regionConfig.set("spawn.blue.dir.x", loc.getDirection().getX());
                regionConfig.set("spawn.blue.dir.y", loc.getDirection().getY());
                regionConfig.set("spawn.blue.dir.z", loc.getDirection().getZ());
                regionConfig.save();
            } catch (Exception e){
                LOGGER.error("Error saving region '{}': {}", "blue", e.getMessage());
            } finally {
                LOGGER.info("Blue spawn saved!");
            }
        } else if (team.equals("red")) {
            redSpawn = loc;
            try {
                regionConfig.set("spawn.red.world", loc.getWorld().getName());
                regionConfig.set("spawn.red.x", loc.getX());
                regionConfig.set("spawn.red.y", loc.getY());
                regionConfig.set("spawn.red.z", loc.getZ());
                regionConfig.set("spawn.red.dir.x", loc.getDirection().getX());
                regionConfig.set("spawn.red.dir.y", loc.getDirection().getY());
                regionConfig.set("spawn.red.dir.z", loc.getDirection().getZ());
                regionConfig.save();
            } catch (Exception e){
                LOGGER.error("Error saving region '{}': {}", "red", e.getMessage());
            } finally {
                LOGGER.info("Red spawn saved!");
            }
        }
    }

    public static void loadSpawnsFromFile() {
        ConfigurationSection regionsSection = regionConfig.getConfig().getConfigurationSection("spawn");
        if (regionsSection == null) return;
        ConfigurationSection blue = regionsSection.getConfigurationSection("blue");
        ConfigurationSection red = regionsSection.getConfigurationSection("red");
        if (blue != null) {
            try {
                blueSpawn = new Location(Bukkit.getWorld(Objects.requireNonNull(blue.getString("world"))),
                    blue.getInt("x"),
                    blue.getInt("y"),
                    blue.getInt("z"))
                    .setDirection(new Vector(
                            (float) Objects.requireNonNull(blue.getConfigurationSection("dir")).getDouble("x"),
                            (float) Objects.requireNonNull(blue.getConfigurationSection("dir")).getDouble("y"),
                            (float) Objects.requireNonNull(blue.getConfigurationSection("dir")).getDouble("z")
                    ));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error loading region '{}': {}", "blue", e.getMessage());
            }
        }

        if (red != null) {
            try {
                redSpawn = new Location(Bukkit.getWorld(Objects.requireNonNull(red.getString("world"))),
                        red.getInt("x"),
                        red.getInt("y"),
                        red.getInt("z"))
                        .setDirection(new Vector(
                                (float) Objects.requireNonNull(red.getConfigurationSection("dir")).getDouble("x"),
                                (float) Objects.requireNonNull(red.getConfigurationSection("dir")).getDouble("y"),
                                (float) Objects.requireNonNull(red.getConfigurationSection("dir")).getDouble("z")
                        ));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error loading region '{}': {}", "red", e.getMessage());
            }
        }
    }

    public static void start() {
        if (playing || starting) return;

        if (blueSpawn == null || redSpawn == null) {
            LOGGER.error("Fatal error! Set spawns before attempting to begin a game");
            return;
        }

        starting = true;
        preparePhase = true;

        scoreboardUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                if (starting) {
                    for (Player player : queue) {
                        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), k -> new FastBoard(player));
                        board.updateTitle(ChatColor.GOLD + "Capture the Flag");
                        board.updateLine(1, ChatColor.AQUA + "In Queue: ");
                        board.updateLine(2, ChatColor.YELLOW + "Players: " + queue.size());
                        board.updateLine(3, ChatColor.RED + "Starting in: " + globalCountdown);
                    }
                } else if (playing) {
                    for (Player player : inGame) {
                        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), k -> new FastBoard(player));
                        board.updateTitle(ChatColor.GOLD + "Capture the Flag");
                        board.updateLine(1, ChatColor.BLUE + "Blue Team: " + blue.size());
                        board.updateLine(2, ChatColor.RED + "Red Team: " + red.size());
                        board.updateLine(3, ChatColor.AQUA + "Players in Game: " + inGame.size());
                    }
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0, 10);

        new BukkitRunnable() {

            @Override
            public void run() {
                if (globalCountdown > 0) {
                    TextColor color = switch (globalCountdown) {
                        case 1 -> TextColor.color(0xFF0024);
                        case 2 -> TextColor.color(0xFF4500);
                        case 3 -> TextColor.color(0xFFA500);
                        case 4 -> TextColor.color(0xFFFF00);
                        case 5 -> NamedTextColor.GREEN;
                        default -> throw new IllegalStateException("Unexpected value: " + globalCountdown);
                    };
                    queue.forEach(player -> {
                        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                        Title title = Title.title(
                                Component.text(globalCountdown + "", color),
                                Component.text("seconds before the game starts."), times
                        );

                        player.showTitle(title);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    });
                    globalCountdown--;
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

                    boolean startWithBlue = new Random().nextBoolean();
                    int i = 0;
                    int blueRemainder = 0;
                    if (!startWithBlue) {
                        blueRemainder = 1;
                    }
                    for (Player player : inGame) {
                        if (i % 2 == blueRemainder) {
                            blue.add(player);
                            blueTeam.addEntry(player.getName());
                            player.setScoreboard(scoreboard);
                            player.sendMessage(
                                    Component.text("You are on the ")
                                            .append(Component.text("blue", NamedTextColor.BLUE, TextDecoration.BOLD))
                                            .append(Component.text(" team!"))
                            );
                            player.getInventory().addItem(blueFlagBreaker());
                            player.teleport(blueSpawn);
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
                            player.teleport(redSpawn);
                        }

                        player.showTitle(Title.title(Component.text("Game Started!", NamedTextColor.GREEN), Component.empty()));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        player.setGameMode(GameMode.ADVENTURE);
                        kits(player, "ctf");
                        i++;
                    }
                    // Add blue flag to random player in blue team's inventory
                    LOGGER.info(blue.size() + " " + red.size());
                    if (!blue.isEmpty()) {
                        int randomIndex = FastUtils.randomIntInRange(0, blue.size() - 1);
                        if (randomIndex >= 0 && randomIndex < blue.size()) {
                            blue.get(randomIndex).getInventory().addItem(blueFlag());
                        }
                    }

                    if (!red.isEmpty()) {
                        int randomIndex = FastUtils.randomIntInRange(0, red.size() - 1);
                        if (randomIndex >= 0 && randomIndex < red.size()) {
                            red.get(randomIndex).getInventory().addItem(redFlag());
                        }
                    }
                    playing = true;
                    starting = false;
                    this.cancel();
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                preparePhase = false;
                Bukkit.broadcast(Component.text("STARTING GAME"));
            }
        }.runTaskLater(Minerva.getInstance(), 200);
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
                    player.playSound(player, Sound.BLOCK_ANVIL_FALL, 0.8f, 0.8f);
                    player.playSound(player, Sound.ENTITY_ENDER_DRAGON_HURT, 0.3f, 0.7f);
                    player.playSound(player, Sound.ENTITY_CREEPER_PRIMED, 0.4f, 0.6f);
                }
            } else if (winningTeam.equals("red")) {
                subtitle = Component.text("The RED team won!", NamedTextColor.GOLD);
                if (isRed) {
                    title = Component.text("You Won!", NamedTextColor.RED);
                } else {
                    title = Component.text("You Lost!", NamedTextColor.BLUE);
                    player.playSound(player, Sound.BLOCK_ANVIL_FALL, 0.8f, 0.8f);
                    player.playSound(player, Sound.ENTITY_ENDER_DRAGON_HURT, 0.3f, 0.7f);
                    player.playSound(player, Sound.ENTITY_CREEPER_PRIMED, 0.4f, 0.6f);
                }
            } else {
                subtitle = Component.text("The game was ended early", NamedTextColor.GOLD);
                title = Component.text("Game Over!", NamedTextColor.RED);
            }
            player.showTitle(Title.title(title, subtitle));
            GameMode gameMode = player.getPreviousGameMode() == null ? GameMode.SURVIVAL : player.getPreviousGameMode();
            player.setGameMode(gameMode);

            // handle board?
            FastBoard board = boards.remove(player.getUniqueId());
            if (board != null) board.delete();
        });
        //LOGGER.info("Ended");
        loadInventories(new ArrayList<>(inGame));
        queue.clear();
        inGame.clear();
        blue.clear();
        red.clear();
        if (blueTeam != null && scoreboard.getTeam("Blue") != null) blueTeam.unregister();
        if (redTeam != null && scoreboard.getTeam("Red") != null) redTeam.unregister();
        playing = false;
        starting = false;
        preparePhase = false;
        traps.clear();
        kits.clear();
        blueFlagLocation = null;
        redFlagLocation = null;
        boards.clear();
        if(scoreboardUpdater != null) {
            scoreboardUpdater.cancel();
            scoreboardUpdater = null;
        }
    }

    public static boolean inBlueTeam(Player player) {
        return blue.contains(player);
    }

    public static void addTrap(Entity trap, Player player) {
        player.sendMessage("plecing trap");
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
            if (!hasRedFlag(player)) return;
            stop("blue");
        } else if (region2.contains("red") && red.contains(player)) {
            if (!hasBlueFlag(player)) return;
            stop("red");
        }
    }

    public static void skillCast(Player player, CooldownManager cdInstance) {
        String kit = kits.getOrDefault(player, null);
        if (kit == null) return;
        switch (kit) {
            case "scout":
                scoutSkill(player, cdInstance);
                break;
            case "attacker":
                attackerSkill(player, cdInstance);
                break;
            case "defender":
                defenderSkill(player, cdInstance);
                break;
            default: LOGGER.error("Invalid kit: " + kit + ". Error in skillCast() function in CaptureTheFlag.java.");
        }
    }

    private static void scoutSkill(Player player, CooldownManager cdInstance) {
        player.sendMessage("You are a scout and you are, in fact, casting a skill."); // Point towards banner
    }

    private static void attackerSkill(Player player, CooldownManager cdInstance) {
        long cd = 6000;

        cdInstance.setCooldownFromNow(player.getUniqueId(), "ctfSkill", cd);

        Vector dir = player.getLocation().getDirection();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.2f, 1.3f);
        player.setVelocity(dir.setY(1).normalize().multiply(2));

        if (player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER) == null) return;
        double currAttribute = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)).getBaseValue();
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)).setBaseValue(0);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)).setBaseValue(currAttribute);
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 5L, 5L);
    }

    private static void defenderSkill(Player player, CooldownManager cdInstance) {
        player.sendMessage("You are a defender and you are, in fact, casting a skill."); // Parry
    }

    public static void kitChoose(Player player, String kit) {
        kits.put(player, kit);
    }

    public static void tpSpawn(Player player) {
        kits.remove(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (blue.contains(player)) {
                    player.teleport(blueSpawn);
                } else {
                    player.teleport(redSpawn);
                }
                kits(player, "ctf");
            }
        }.runTaskLater(Minerva.getInstance(), 2L);
    }
}