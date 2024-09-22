package net.minervamc.minerva.lib;

import net.minervamc.minerva.lib.events.GlobalEventHandler;
import net.minervamc.minerva.lib.listeners.MenuListener;
import net.minervamc.minerva.lib.listeners.PaginatedMenuListener;

public class Lib {

    public static void onEnable() {
        // This loads all config files and stores it in memory and can be accessed via configManager with the path name
        // ConfigManager.getManager().load(plugin);
        GlobalEventHandler.get().addListener(new MenuListener()).addListener(new PaginatedMenuListener());
    }
}
