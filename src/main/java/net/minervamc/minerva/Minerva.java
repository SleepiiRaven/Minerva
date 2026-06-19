package net.minervamc.minerva;

import java.io.File;
import java.util.Objects;
import lombok.Getter;
import net.minervamc.minerva.commands.CtfCommand;
import net.minervamc.minerva.commands.FocusCommand;
import net.minervamc.minerva.commands.NoCooldownCommand;
import net.minervamc.minerva.commands.PartyCommand;
import net.minervamc.minerva.commands.SkillModeToggle;
import net.minervamc.minerva.commands.SkillsCommand;
import net.minervamc.minerva.commands.UnfocusCommand;
import net.minervamc.minerva.lib.Lib;
import net.minervamc.minerva.listeners.CombatListener;
import net.minervamc.minerva.listeners.CtfListener;
import net.minervamc.minerva.listeners.PlayerListener;
import net.minervamc.minerva.listeners.RegionListener;
import net.minervamc.minerva.listeners.SkillListener;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.minigames.ctf.RegionManager;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.command.QuestsCommand;
import net.minervamc.minerva.quest.cutscene.CutsceneEngine;
import net.minervamc.minerva.quest.listeners.QuestListener;
import net.minervamc.minerva.quest.listeners.QuestNpcListener;
import net.minervamc.minerva.quest.npc.NpcRecorder;
import net.minervamc.minerva.quest.storage.QuestProgressStorage;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minerva extends JavaPlugin {
    public static File dataFolder;
    public static NamespacedKey itemMessageKey;
    @Getter private static Minerva instance;
    @Getter private CooldownManager cdInstance;

    public static void runChannelCommand(Player player, String channel) {
        Bukkit.dispatchCommand(player, channelCommand + channel);
    }

    public static void runPermCommand(Player player, String perm) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), permCommand1 + player.getName() + permCommand2 + perm + " true");
    }

    public static void runPermRemoveCommand(Player player, String perm) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), permCommand1 + player.getName() + permCommand2 + perm + " false");
    }

    public static String channelCommand = "ch "; // This is the command that is run from VentureChat that joins a channel for a player.
    public static String permCommand1 = "lp user "; // This is the command that is run from LuckPerms that gives a user a permission.
    public static String permCommand2 = " permission set ";

    @Override
    public void onEnable() {
        cdInstance = new CooldownManager();
        dataFolder = getDataFolder();
        instance = this;
        itemMessageKey = new NamespacedKey(instance, "itemMessageKey");

        saveDefaultConfig();
        QuestManager.detectCapabilities();
        registerListeners();
        registerCommands();
        net.minervamc.minerva.skills.HeritageTick.start();

        Lib.onEnable(); // Faceless start
        RegionManager.loadRegionsFromFile();
        CaptureTheFlag.loadDefaultsFromFile();
        RegionListener.register();
        //Faceless stop

        QuestManager.loadAll();
        getSLF4JLogger().info("Quest system enabled (Citizens={}, ProtocolLib={})",
                QuestManager.isCitizensEnabled(), QuestManager.isProtocolEnabled());

        if (QuestManager.isCitizensEnabled()) {
            int swept = net.minervamc.minerva.listeners.PlayerListener.sweepLeakedLoadingNpcs();
            if (swept > 0) getSLF4JLogger().info("Removed {} leaked '- Loading In' NPC(s) from a previous build.", swept);
        }
    }

    @Override
    public void onDisable() {
        CaptureTheFlag.stop("");
        PlayerStats.saveAll();
        PlayerStats.removeAllSummons();
        RegionManager.saveRegionsToFile(); // not really necessary but safer

        CutsceneEngine.endAll();
        NpcRecorder.cancelAll();
        QuestProgressStorage.saveAll();
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new SkillListener(), this);
        getServer().getPluginManager().registerEvents(new CtfListener(), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getServer().getPluginManager().registerEvents(new QuestListener(), this);
        if (QuestManager.isCitizensEnabled()) {
            getServer().getPluginManager().registerEvents(new QuestNpcListener(), this);
        }
    }

    public void registerCommands() {
        Objects.requireNonNull(getCommand("mskills")).setExecutor(new SkillsCommand());
        Objects.requireNonNull(getCommand("skillmode")).setExecutor(new SkillModeToggle());
        Objects.requireNonNull(getCommand("party")).setExecutor(new PartyCommand());
        CtfCommand.register(this);
        NoCooldownCommand.register(this);
        Objects.requireNonNull(getCommand("focus")).setExecutor(new FocusCommand());
        Objects.requireNonNull(getCommand("unfocus")).setExecutor(new UnfocusCommand());
        QuestsCommand.register(this);
    }


    /** EXPLANATION OF INNER WORKINGS
     * PlayerListener listens for left click or right click.
     * If it's a right click and all conditions are fulfilled, enters a "SkillMode" controlled in SkillTriggers
     * Then after "SkillMode" is entered, any two left/right clicks continue the chain and then are transported to their respective skill
     * PlayerStats saves all the stats/saveable stuff for players into a json file and uses PlayerStatsAdapter to convert.
     * CooldownManager has a big list of cooldowns
     * **/
}