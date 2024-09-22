package net.minervamc.minerva;

import java.io.File;

import lombok.Getter;
import net.minervamc.minerva.commands.PartyCommand;
import net.minervamc.minerva.commands.SkillModeToggle;
import net.minervamc.minerva.commands.SkillsCommand;
import net.minervamc.minerva.lib.Lib;
import net.minervamc.minerva.listeners.PlayerListener;
import net.minervamc.minerva.listeners.SkillListener;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minerva extends JavaPlugin {
    public static File dataFolder;
    public static NamespacedKey itemMessageKey;
    @Getter
    private static Minerva instance;
    @Getter
    private CooldownManager cdInstance;

    @Override
    public void onEnable() {
        cdInstance = new CooldownManager();
        dataFolder = getDataFolder();
        instance = this;
        itemMessageKey = new NamespacedKey(instance, "itemMessageKey");

        saveDefaultConfig();
        registerListeners();
        registerCommands();

        Lib.onEnable(); // Faceless
    }

    @Override
    public void onDisable() {
        PlayerStats.saveAll();
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new SkillListener(), this);
    }

    public void registerCommands() {
        getCommand("mskills").setExecutor(new SkillsCommand());
        getCommand("skillmode").setExecutor(new SkillModeToggle());
        getCommand("party").setExecutor(new PartyCommand());
    }

}
