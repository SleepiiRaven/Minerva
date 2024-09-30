package net.minervamc.minerva;

import java.io.File;
import java.util.Objects;

import lombok.Getter;
import net.minervamc.minerva.commands.CtfCommand;
import net.minervamc.minerva.commands.PartyCommand;
import net.minervamc.minerva.commands.SkillModeToggle;
import net.minervamc.minerva.commands.SkillsCommand;
import net.minervamc.minerva.lib.Lib;
import net.minervamc.minerva.listeners.CtfListener;
import net.minervamc.minerva.listeners.PlayerListener;
import net.minervamc.minerva.listeners.RegionListener;
import net.minervamc.minerva.listeners.SkillListener;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.minigames.ctf.RegionManager;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minerva extends JavaPlugin {
    public static File dataFolder;
    public static NamespacedKey itemMessageKey;
    @Getter private static Minerva instance;
    @Getter private CooldownManager cdInstance;

    @Override
    public void onEnable() {
        cdInstance = new CooldownManager();
        dataFolder = getDataFolder();
        instance = this;
        itemMessageKey = new NamespacedKey(instance, "itemMessageKey");

        saveDefaultConfig();
        registerListeners();
        registerCommands();

        Lib.onEnable(); // Faceless start
        RegionManager.loadRegionsFromFile();
        CaptureTheFlag.loadDefaultsFromFile();
        RegionListener.register();
        //Faceless stop
    }

    @Override
    public void onDisable() {
        PlayerStats.saveAll();
        RegionManager.saveRegionsToFile(); // not really necessary but safer
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new SkillListener(), this);
        getServer().getPluginManager().registerEvents(new CtfListener(), this);
    }

    public void registerCommands() {
        Objects.requireNonNull(getCommand("mskills")).setExecutor(new SkillsCommand());
        Objects.requireNonNull(getCommand("skillmode")).setExecutor(new SkillModeToggle());
        Objects.requireNonNull(getCommand("party")).setExecutor(new PartyCommand());
        CtfCommand.register(this);
    }

}
