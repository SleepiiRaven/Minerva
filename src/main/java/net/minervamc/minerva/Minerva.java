package net.minervamc.minerva;

import java.io.File;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.minervamc.minerva.commands.FocusCommand;
import net.minervamc.minerva.commands.PartyCommand;
import net.minervamc.minerva.commands.SkillModeToggle;
import net.minervamc.minerva.commands.SkillsCommand;
import net.minervamc.minerva.api.command.Command;
import net.minervamc.minerva.listeners.PlayerListener;
import net.minervamc.minerva.listeners.SkillListener;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class Minerva extends JavaPlugin {
    @Getter private static Minerva instance;
    @Getter private static Logger log;
    @Getter private CooldownManager cdInstance;
    @Getter private static File pluginFolder;

    @Override
    public void onEnable() {
        Minerva.instance = this;
        Minerva.log = this.getSLF4JLogger();
        Minerva.pluginFolder = this.getDataFolder();

        this.cdInstance = new CooldownManager();

        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        PlayerStats.saveAll();
    }

    @Override
    public @NotNull PluginCommand getCommand(@NotNull String name) {
        return Preconditions.checkNotNull(super.getCommand(name), "Command was null during registration.");
    }

    public void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        this.getServer().getPluginManager().registerEvents(new SkillListener(), this);
    }

    public void registerCommands() {
        this.getCommand("mskills").setExecutor(new SkillsCommand());
        this.getCommand("skillmode").setExecutor(new SkillModeToggle());
        this.getCommand("party").setExecutor(new PartyCommand());

        Command.register(this, new FocusCommand()); // Registering a custom command, no need to add in plugin.yml
    }

    private void registerPerms(PluginManager manager) {
        Permission minervaSkillsSet = new Permission("minerva.skills.set", "Allows you to set your own stats");
        Permission minervaSkillsSetOthers = new Permission("minerva.skills.set.others", "Allows you to set others' stats", PermissionDefault.OP);
        Permission minervaSkillsOpenGuiOthers = new Permission("minerva.skills.opengui.others", "Allows you to open the Minerva Skills GUI for others.", PermissionDefault.OP);
        Permission minervaBigThreeZeus = new Permission("minerva.bigthree.zeus", "Allows you to choose Zeus in the Skills GUI.");
        Permission minervaBigThreePoseidon = new Permission("minerva.bigthree.poseidon", "Allows you to choose Poseidon in the Skills GUI.");
        Permission minervaBigThreeHades = new Permission("minerva.bigthree.hades", "Allows you to choose Hades in the Skills GUI.");

        manager.addPermission(minervaSkillsSet);
        manager.addPermission(minervaSkillsSetOthers);
        manager.addPermission(minervaSkillsOpenGuiOthers);
        manager.addPermission(minervaBigThreeZeus);
        manager.addPermission(minervaBigThreePoseidon);
        manager.addPermission(minervaBigThreeHades);
    }
}
