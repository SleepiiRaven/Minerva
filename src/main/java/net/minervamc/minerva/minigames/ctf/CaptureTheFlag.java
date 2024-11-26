package net.minervamc.minerva.minigames.ctf;

import fr.mrmicky.fastboard.FastBoard;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.storage.yaml.Config;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.Minigame;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.slf4j.Logger;

public class CaptureTheFlag extends Minigame {
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();

    private static final Config spawnConfig = new Config("ctf/spawns.yml");
    private static final Config defaultFlagConfig = new Config("ctf/flags.yml");

    @Getter
    public static boolean playing = false;
    public static boolean starting = false;
    public static boolean preparePhase = false;

    private static final List<Player> queue = new ArrayList<>();
    private static final List<Player> inGame = new ArrayList<>();
    @Getter private static final List<Player> blue = new ArrayList<>();
    @Getter private static final List<Player> red = new ArrayList<>();

    private static final List<Location> blocks = new ArrayList<>();

    private static final Map<Entity, Player> traps = new HashMap<>();
    private static final Map<Player, String> kits = new HashMap<>();

    public static Location blueFlagLocation;
    public static Location redFlagLocation;
    public static boolean blueFlagWall = false;
    public static boolean redFlagWall = false;
    public static Location defaultBlueFlagPos;
    public static Location defaultRedFlagPos;
    private static final List<Location> blueSpawn = new ArrayList<>();
    private static final List<Location> redSpawn = new ArrayList<>();

    private static final HashMap<UUID, FastBoard> boards = new HashMap<>();
    @Getter private static final HashMap<UUID, Location> startLoc = new HashMap<>();
    private static BukkitTask scoreboardUpdater = null;
    private static int globalCountdown = 61;

    static BukkitTask startTimer;
    private static int startingTimerTicks = 0;

    // Team stuff
    private static Scoreboard scoreboard;
    private static Team blueTeam;
    private static Team redTeam;

    public static void placeBlock(Location location) {
        blocks.add(location);
    }

    public static int playerCount() {
        return inGame.size();
    }

    public static boolean inSameTeam(Player p1, Player p2) {
        return (blue.contains(p1) && blue.contains(p2)) || (red.contains(p1) && red.contains(p2));
    }

    public static void addQueue(Player player) {
        if (isInGame(player)) return;
        if (playing) {
            player.sendMessage(Component.text("Game has already started!"));
            return;
        }
        if (isInQueue(player)) {
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

        if (queue.size() >= 12) {
            globalCountdown = Math.min(globalCountdown, 6);
        } else if (queue.size() >= 8) {
            globalCountdown = Math.min(globalCountdown, 11);
        } else if (queue.size() >= 6) {
            globalCountdown = Math.min(globalCountdown, 31);
        } else if (queue.size() >= 4) {
            globalCountdown = Math.min(globalCountdown, 61);
        }

        if (scoreboardUpdater == null) {
            scoreboardUpdater = new BukkitRunnable() {
                boolean removedLn4 = false;
                @Override
                public void run() {
                    if (playing) {
                        for (Player player : inGame) {
                            FastBoard board = boards.computeIfAbsent(player.getUniqueId(), k -> new FastBoard(player));
                            board.updateTitle(ChatColor.GOLD + "Capture the Flag");
                            board.updateLine(0, ChatColor.GRAY +    "+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+");
                            board.updateLine(1, ChatColor.BLUE + "Blue Team: " + blue.size());
                            board.updateLine(2, ChatColor.RED + "Red Team: " + red.size());
                            board.updateLine(3, ChatColor.AQUA + "Players in Game: " + inGame.size());
                            if (preparePhase) {
                                board.updateLine(4, ChatColor.RED + "Seconds Before Barrier Drops: " + (60 - startingTimerTicks));
                            } else if (!removedLn4 && startingTimerTicks >= 60) {
                                board.removeLine(4);
                                removedLn4 = true;
                            }
                        }
                        return;
                    }

                    if (queue.size() >= 4) {
                        starting = true;

                        if (globalCountdown > 0) {
                            globalCountdown--;
                        } else {
                            starting = false;
                            start();
                            return;
                        }
                    } else {
                        starting = false;
                        globalCountdown = 61;
                    }

                    for (Player player : queue) {
                        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), k -> new FastBoard(player));
                        if(starting) {
                            board.updateLine(3, ChatColor.RED + "Starting in: " + globalCountdown);
                            TextColor color = switch (globalCountdown) {
                                case 1 -> TextColor.color(0xFF0024);
                                case 2 -> TextColor.color(0xFF4500);
                                case 3 -> TextColor.color(0xFFA500);
                                case 4 -> TextColor.color(0xFFFF00);
                                default -> NamedTextColor.GREEN;
                            };

                            board.updateLine(1, ChatColor.AQUA + "In Queue: " + ChatColor.YELLOW + queue.size());

                            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                            Title title = Title.title(
                                    Component.text(globalCountdown + "", color),
                                    Component.text("seconds before the game starts."), times
                            );

                            player.showTitle(title);
                            if (globalCountdown < 6) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                            }
                        } else {
                            board.updateTitle(ChatColor.GOLD + "Capture the Flag");
                            board.updateLine(0, ChatColor.GRAY +"+=+=+=+=+=+=+=+=+=+=+");
                            board.updateLine(1, ChatColor.AQUA + "In Queue: " + ChatColor.YELLOW + queue.size());
                            board.updateLine(3, ChatColor.RED + "Waiting for more players...");
                        }
                    }
                }
            }.runTaskTimer(Minerva.getInstance(), 0, 20l);
        }
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

        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) board.delete();
        if(queue.isEmpty()) {
            boards.clear();
            if(scoreboardUpdater != null) {
                scoreboardUpdater.cancel();
                scoreboardUpdater = null;
            }
        }
    }

    public static boolean isInQueue(Player player) {
        return queue.contains(player);
    }

    public static boolean isInGame(Player player) {
        return inGame.contains(player);
    }

    public static void removeFromGame(Player player) {
        if (inBlueTeam(player)) {
            blue.remove(player);
        } else {
            red.remove(player);
        }

        PlayerStats.getStats(player.getUniqueId()).save();

        startLoc.remove(player.getUniqueId());
        inGame.remove(player);
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

    public static void setDefaultFlagPos(Location loc, String team) {
        if (team == null) return;
        if (team.equals("blue")) {
            defaultBlueFlagPos = loc;
            try {
                defaultFlagConfig.set("blue.world", loc.getWorld().getName());
                defaultFlagConfig.set("blue.x", loc.getX());
                defaultFlagConfig.set("blue.y", loc.getY());
                defaultFlagConfig.set("blue.z", loc.getZ());
            } catch (Exception e) {
                LOGGER.error("Error saving blue flag default location. ERROR: {}", e.getMessage());
            } finally {
                LOGGER.info("Blue flag default location saved!");
            }
        } else if (team.equals("red")) {
            defaultRedFlagPos = loc;
            try {
                defaultFlagConfig.set("red.world", loc.getWorld().getName());
                defaultFlagConfig.set("red.x", loc.getX());
                defaultFlagConfig.set("red.y", loc.getY());
                defaultFlagConfig.set("red.z", loc.getZ());
            } catch (Exception e) {
                LOGGER.error("Error saving red flag default location. ERROR: {}", e.getMessage());
            } finally {
                LOGGER.info("Red flag default location saved!");
            }
        }
    }

    public static void setSpawnPos(Location loc, String team, String name) {
        if (team == null) return;
        if (team.equals("blue")) {
            blueSpawn.add(loc);
            try {
                spawnConfig.set("spawn.blue." + name + ".world", loc.getWorld().getName());
                spawnConfig.set("spawn.blue." + name + ".x", loc.getX());
                spawnConfig.set("spawn.blue." + name + ".y", loc.getY());
                spawnConfig.set("spawn.blue." + name + ".z", loc.getZ());
                spawnConfig.set("spawn.blue." + name + ".dir.x", loc.getDirection().getX());
                spawnConfig.set("spawn.blue." + name + ".dir.y", loc.getDirection().getY());
                spawnConfig.set("spawn.blue." + name + ".dir.z", loc.getDirection().getZ());
                spawnConfig.save();
            } catch (Exception e){
                LOGGER.error("Error saving region '{}': {}", "blue", e.getMessage());
            } finally {
                LOGGER.info("Blue spawn saved!");
            }
        } else if (team.equals("red")) {
            redSpawn.add(loc);
            try {
                spawnConfig.set("spawn.red." + name + ".world", loc.getWorld().getName());
                spawnConfig.set("spawn.red." + name + ".x", loc.getX());
                spawnConfig.set("spawn.red." + name + ".y", loc.getY());
                spawnConfig.set("spawn.red." + name + ".z", loc.getZ());
                spawnConfig.set("spawn.red." + name + ".dir.x", loc.getDirection().getX());
                spawnConfig.set("spawn.red." + name + ".dir.y", loc.getDirection().getY());
                spawnConfig.set("spawn.red." + name + ".dir.z", loc.getDirection().getZ());
                spawnConfig.save();
            } catch (Exception e){
                LOGGER.error("Error saving region '{}': {}", "red", e.getMessage());
            } finally {
                LOGGER.info("Red spawn saved!");
            }
        }
    }

    public static void loadDefaultsFromFile() {
        ConfigurationSection spawnSection = spawnConfig.getConfig().getConfigurationSection("spawn");
        ConfigurationSection blueFlagSection = defaultFlagConfig.getConfig().getConfigurationSection("blue");
        ConfigurationSection redFlagSection = defaultFlagConfig.getConfig().getConfigurationSection("red");
        if (spawnSection == null) return;
        ConfigurationSection blue = spawnSection.getConfigurationSection("blue");
        ConfigurationSection red = spawnSection.getConfigurationSection("red");
        if (blue != null) {
            for (String regionName : blue.getKeys(false)) {
                ConfigurationSection section = blue.getConfigurationSection(regionName);

                if (section != null) {
                    try {
                        blueSpawn.add(new Location(Bukkit.getWorld(Objects.requireNonNull(section.getString("world"))),
                                section.getInt("x"),
                                section.getInt("y"),
                                section.getInt("z"))
                                .setDirection(new Vector(
                                        (float) Objects.requireNonNull(section.getConfigurationSection("dir")).getDouble("x"),
                                        (float) Objects.requireNonNull(section.getConfigurationSection("dir")).getDouble("y"),
                                        (float) Objects.requireNonNull(section.getConfigurationSection("dir")).getDouble("z")
                                )));
                    } catch (NullPointerException e) {
                        LOGGER.error("Error loading region '{}': {}", "blue", e.getMessage());
                    }
                }
            }
        }

        if (red != null) {
            for (String regionName : red.getKeys(false)) {
                ConfigurationSection section = red.getConfigurationSection(regionName);

                if (section != null) {
                    try {
                        redSpawn.add(new Location(Bukkit.getWorld(Objects.requireNonNull(section.getString("world"))),
                                section.getInt("x"),
                                section.getInt("y"),
                                section.getInt("z"))
                                .setDirection(new Vector(
                                        (float) Objects.requireNonNull(section.getConfigurationSection("dir")).getDouble("x"),
                                        (float) Objects.requireNonNull(section.getConfigurationSection("dir")).getDouble("y"),
                                        (float) Objects.requireNonNull(section.getConfigurationSection("dir")).getDouble("z")
                                )));
                    } catch (NullPointerException e) {
                        LOGGER.error("Error loading region '{}': {}", "red", e.getMessage());
                    }
                }
            }
        }

        if (blueFlagSection != null) {
            try {
                defaultBlueFlagPos = new Location(Bukkit.getWorld(Objects.requireNonNull(blueFlagSection.getString("world"))),
                        blueFlagSection.getInt("x"),
                        blueFlagSection.getInt("y"),
                        blueFlagSection.getInt("z"));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error loading default flag location '{}': {}", "blue", e.getMessage());
            }
        }

        if (redFlagSection != null) {
            try {
                defaultRedFlagPos = new Location(Bukkit.getWorld(Objects.requireNonNull(redFlagSection.getString("world"))),
                        redFlagSection.getInt("x"),
                        redFlagSection.getInt("y"),
                        redFlagSection.getInt("z"));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error loading default flag location '{}': {}", "red", e.getMessage());
            }
        }
    }

    public static void start() {
        if (playing || starting) return;

        if (blueSpawn.isEmpty() || redSpawn.isEmpty() || defaultBlueFlagPos == null || defaultRedFlagPos == null) {
            LOGGER.info(blueSpawn.toString());
            LOGGER.info(redSpawn.toString());
            LOGGER.info(defaultBlueFlagPos.toString());
            LOGGER.info(defaultRedFlagPos.toString());
            LOGGER.error("Fatal error! Set blue and red spawns as well as blue and red default flag locations before attempting to begin a game");
            return;
        }

        preparePhase = true;
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

        startTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (startingTimerTicks <= 54) {
                    startingTimerTicks++;
                    return;
                }

                Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                if (startingTimerTicks == 60) {
                    autoPlaceBanner("blue");
                    autoPlaceBanner("red");
                    preparePhase = false;
                    Title title = Title.title(
                            Component.text("Go!"),
                            Component.text("the barriers have lifted."), times
                    );
                    for (Player player : inGame) {
                        player.showTitle(title);
                        player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1, 1);
                    }
                    this.cancel();
                } else {
                    TextColor color = switch (60 - startingTimerTicks) {
                        case 1 -> TextColor.color(0xFF0024);
                        case 2 -> TextColor.color(0xFFA500);
                        case 3 -> NamedTextColor.GREEN;
                        default -> NamedTextColor.WHITE;
                    };

                    Title title = Title.title(
                            Component.text((60 - startingTimerTicks) + "", color),
                            Component.text("seconds before the barrier drops."), times
                    );

                    for (Player player : inGame) {
                        player.showTitle(title);
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                }

                startingTimerTicks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 20L);

        boolean startWithBlue = new Random().nextBoolean();
        int i = 0;
        int blueRemainder = 0;
        if (!startWithBlue) {
            blueRemainder = 1;
        }
        for (Player player : inGame) {
            startLoc.put(player.getUniqueId(), player.getLocation());
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.clearActivePotionEffects();

            player.sendMessage(Component.text("The barrier will drop soon. You have 60 seconds to prepare. Used items will ", NamedTextColor.GOLD).append(Component.text("NOT", NamedTextColor.RED).decorate(TextDecoration.BOLD)).append(Component.text(" replenish after death", NamedTextColor.GOLD)));

            Random random = new Random();
            random.setSeed(System.currentTimeMillis() + random.nextInt(1000));
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
                int size = blueSpawn.size();
                int randInt = (size <= 1) ? 0 : random.nextInt(size);
                player.teleport(blueSpawn.get(randInt));
                Minerva.runPermCommand(player, "venturechat.blueteamchat");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Minerva.runChannelCommand(player, "blueteam");
                    }
                }.runTaskLater(Minerva.getInstance(), 5L);
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
                int size = redSpawn.size();
                int randInt = (size <= 1) ? 0 : random.nextInt(size);
                player.teleport(redSpawn.get(randInt));
                Minerva.runPermCommand(player, "venturechat.redteamchat");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Minerva.runChannelCommand(player, "redteam");
                    }
                }.runTaskLater(Minerva.getInstance(), 5L);
            }

            player.showTitle(Title.title(Component.text("Game Started!", NamedTextColor.GREEN), Component.empty()));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            player.setGameMode(GameMode.ADVENTURE);
            kits(player, "ctf");
            i++;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();
                random.setSeed(System.currentTimeMillis() + random.nextInt(1000));
                // Add blue flag to random player in blue team's inventory
                if (!blue.isEmpty()) {
                    int randomIndex;
                    if (blue.size() <= 1) {
                        randomIndex = 0;
                    } else {
                        randomIndex = random.nextInt(blue.size());
                    }
                    if (randomIndex >= 0 && randomIndex < blue.size()) {
                        blue.get(randomIndex).getInventory().addItem(blueFlag());
                        blue.get(randomIndex).sendMessage(Component.text("You have been given the flag for your team! Place it on Mycelium (the purple mushroom dirt stuff) and defend it (with your life)", NamedTextColor.YELLOW));
                    }
                }

                if (!red.isEmpty()) {
                    int randomIndex;
                    if (red.size() <= 1) {
                        randomIndex = 0;
                    } else {
                        randomIndex = random.nextInt(red.size());
                    }
                    if (randomIndex >= 0 && randomIndex < red.size()) {
                        red.get(randomIndex).getInventory().addItem(redFlag());
                        red.get(randomIndex).sendMessage(Component.text("You have been given the flag for your team! Place it on Podzol (the dark brown grass) and defend it (with your life)"));
                    }
                }
            }
        }.runTaskLater(Minerva.getInstance(), 1L);

        playing = true;
    }

    public static void autoPlaceBanner(String team) {
        if (!preparePhase) return;
        switch (team) {
            case "red":
                boolean placeRedFlag = false;
                for (Player player : red) {
                    for (ItemStack itemStack : player.getInventory().getContents()) {
                        if (itemStack == null) continue;
                        if (itemStack.equals(redFlag())) {
                            player.getInventory().removeItem(redFlag());
                            placeRedFlag = true;
                        }
                    }
                }
                if (placeRedFlag) {
                    defaultRedFlagPos.getBlock().setType(Material.RED_BANNER);
                    redFlagLocation = defaultRedFlagPos;
                }
            case "blue":
                boolean placeBlueFlag = false;
                for (Player player : blue) {
                    for (ItemStack itemStack : player.getInventory().getContents()) {
                        if (itemStack == null) continue;
                        if (itemStack.equals(blueFlag())) {
                            player.getInventory().removeItem(blueFlag());
                            placeBlueFlag = true;
                        }
                    }
                }
                if (placeBlueFlag) {
                    defaultBlueFlagPos.getBlock().setType(Material.BLUE_BANNER);
                    blueFlagLocation = defaultBlueFlagPos;
                }
                break;
        }
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
        blueFlagBreakerCr.setName(TextContext.format("Breaker", false).color(NamedTextColor.GOLD));
        blueFlagBreakerCr.setLore(List.of(
                TextContext.formatLegacy("&7Use this to break", false),
                TextContext.formatLegacy("&7the &cred &7team's flag", false),
                TextContext.formatLegacy("&7as well as blocks placed", false),
                TextContext.formatLegacy("&7by either team.", false)
        ));
        blueFlagBreakerCr.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        blueFlagBreakerCr.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return ItemCreator.getBreakable(blueFlagBreakerCr.build(), Material.RED_BANNER, Material.RED_WALL_BANNER, Material.BAMBOO_MOSAIC);
    }

    private static ItemStack redFlagBreaker() {
        ItemCreator redFlagBreakerCr = ItemCreator.get(Material.WOODEN_AXE);
        redFlagBreakerCr.setName(TextContext.format("Breaker", false).color(NamedTextColor.GOLD));
        redFlagBreakerCr.setLore(List.of(
                TextContext.formatLegacy("&7Use this to break", false),
                TextContext.formatLegacy("&7the &1blue &7team's flag", false),
                TextContext.formatLegacy("&7as well as blocks placed", false),
                TextContext.formatLegacy("&7by either team.", false)
        ));
        redFlagBreakerCr.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        redFlagBreakerCr.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return ItemCreator.getBreakable(redFlagBreakerCr.build(), Material.BLUE_BANNER, Material.BLUE_WALL_BANNER, Material.BAMBOO_MOSAIC);
    }

    public static void stop(String winningTeam) {
        if(!playing) return;
        startTimer.cancel();
        startingTimerTicks = 0;

        inGame.forEach(player -> {
            player.getActivePotionEffects().clear();
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

            player.teleport(startLoc.get(player.getUniqueId()));

            Minerva.runPermRemoveCommand(player, "venturechat.blueteamchat");
            Minerva.runPermRemoveCommand(player, "venturechat.redteamchat");

            // handle board?
            FastBoard board = boards.remove(player.getUniqueId());
            if (board != null) board.delete();
        });
        startLoc.clear();
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
        blueFlagWall = false;
        redFlagWall = false;

        for (Location block : blocks) {
            block.getBlock().setType(Material.AIR);
        }

        for (Entity trap : traps.keySet()) {
            trap.getNearbyEntities(0.1, 0.1, 0.1).forEach(p -> {
                if (p instanceof BlockDisplay && p.getScoreboardTags().contains("ctfVisualTrap")) p.remove();
            });
            trap.remove();
        }
        traps.clear();
        kits.clear();

        if (blueFlagLocation != null) {
            blueFlagLocation.getBlock().breakNaturally();
            for (Entity entity : blueFlagLocation.getNearbyEntities(1, 1, 1)) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
        }
        if (redFlagLocation != null) {
            redFlagLocation.getBlock().breakNaturally();
            for (Entity entity : redFlagLocation.getNearbyEntities(1, 1, 1)) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
        }
        blueFlagLocation = null;
        redFlagLocation = null;
        boards.clear();
        globalCountdown = 6;

        if(scoreboardUpdater != null) {
            scoreboardUpdater.cancel();
            scoreboardUpdater = null;
        }
    }

    public static boolean inBlueTeam(Player player) {
        return blue.contains(player);
    }

    public static void addTrap(Entity trap, Player player) {
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
        trap.getWorld().spawnParticle(Particle.EXPLOSION, trap.getLocation(), 3, 0, 0, 0, 0);
        for (Entity e : trap.getNearbyEntities(3, 3, 3)) {
            if (!(e instanceof LivingEntity lE)) continue;
            lE.damage(5);
            Vector kb = ParticleUtils.getDirection(trap.getLocation(), lE.getLocation()).normalize().multiply(1);
            Skill.knockback(lE, kb);
    }
        traps.remove(trap);
        trap.getNearbyEntities(0.1, 0.1, 0.1).forEach(p -> {
            if (p instanceof BlockDisplay && p.getScoreboardTags().contains("ctfVisualTrap")) p.remove();
        });
        trap.remove();
    }

    public static void changedRegion(String region1, String region2, PlayerMoveEvent event) {
        if (region2.contains("barrier") && preparePhase) {
            event.setCancelled(true);
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
        long cd = 20000;

        cdInstance.setCooldownFromNow(player.getUniqueId(), "ctfSkill", cd);

        Vector direction = null;
        if (blueFlagLocation == null || redFlagLocation == null) {
            LOGGER.error("A flag location is null");
            return;
        }

        boolean blueFlagInPlace = (blueFlagLocation.getBlock().getType() == Material.BLUE_WALL_BANNER ||
                blueFlagLocation.getBlock().getType() == Material.BLUE_BANNER);
        Player blueFlagHolder = null;
        Player redFlagHolder = null;
        if (!blueFlagInPlace) {
            for (Player p : red) {
                if (hasBlueFlag(p)) {
                    blueFlagHolder = p;
                }
            }
        }
        boolean redFlagInPlace = (redFlagLocation.getBlock().getType() == Material.RED_WALL_BANNER ||
                redFlagLocation.getBlock().getType() == Material.RED_BANNER);
        if (!redFlagInPlace) {
            for (Player p : blue) {
                if (hasRedFlag(p)) {
                    redFlagHolder = p;
                }
            }
        }

        if (blue.contains(player)) {
            direction = (redFlagInPlace)
                ? ParticleUtils.getDirection(player.getEyeLocation(), redFlagLocation)
                : (blueFlagInPlace || blueFlagHolder == null)
                    ? ParticleUtils.getDirection(player.getEyeLocation(), blueFlagLocation)
                    : ParticleUtils.getDirection(player.getEyeLocation(), blueFlagHolder.getEyeLocation());
        } else if (red.contains(player)) {
            direction = (blueFlagInPlace)
                    ? ParticleUtils.getDirection(player.getEyeLocation(), blueFlagLocation)
                    : (redFlagInPlace || redFlagHolder == null)
                    ? ParticleUtils.getDirection(player.getEyeLocation(), redFlagLocation)
                    : ParticleUtils.getDirection(player.getEyeLocation(), redFlagHolder.getEyeLocation());
        }

        if (direction == null) return;
        for (Vector linePoint : ParticleUtils.getLinePoints(new Vector(), direction.normalize().multiply(10), 0.5)) {
            Location particleLoc = player.getEyeLocation().add(linePoint);
            player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 0, 0, 0, 0, 0);
        }
    }

    private static void attackerSkill(Player player, CooldownManager cdInstance) {
        long cd = 6000;

        cdInstance.setCooldownFromNow(player.getUniqueId(), "ctfSkill", cd);

        Vector dir = player.getLocation().getDirection();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.2f, 1.3f);
        player.setVelocity(dir.setY(1).normalize().multiply(1.5));

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
        int duration = 20; // 1 second in ticks
        long cd = 15000;

        cdInstance.setCooldownFromNow(player.getUniqueId(), "ctfSkill", cd);

        player.addScoreboardTag("ctfParryAbility");

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 1));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 2f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks * 5 >= duration) {
                    player.removeScoreboardTag("ctfParryAbility");
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 0.5f);
                    this.cancel();
                } else {
                    List<Vector> shield = ParticleUtils.getCylinderPoints(1, 3);
                    for (Vector vec : shield) {
                        player.getWorld().spawnParticle(Particle.FIREWORK, player.getEyeLocation().add(vec), 0, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 5L);
    }

    public static void kitChoose(Player player, String kit) {
        kits.put(player, kit);
    }

    public static void tpSpawn(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.clearActivePotionEffects();
        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), "ctfSkill", 0L);
        new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();
                random.setSeed(System.currentTimeMillis() + random.nextInt(1000));
                if (blue.contains(player)) {
                    int size = blueSpawn.size();
                    int randInt = (size == 1) ? 0 : random.nextInt(size);
                    player.teleport(blueSpawn.get(randInt));
                } else {
                    int size = redSpawn.size();
                    int randInt = (size == 1) ? 0 : random.nextInt(size);
                    player.teleport(redSpawn.get(randInt));
                }
            }
        }.runTaskLater(Minerva.getInstance(), 2L);
    }

    public static void warnFlag(Player flagStealer, String team) {
        List<Player> players;
        if (team == "blue") {
            players = blue;
        } else {
            players = red;
        }

        Component title = Component.text("Flag Stolen!", NamedTextColor.GOLD);
        Component subtitle = Component.text(flagStealer.getName() + " has taken the flag!", NamedTextColor.RED);

        for (Player player : players) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 2f, 1f);
            player.showTitle(Title.title(title, subtitle));
        }
    }
}