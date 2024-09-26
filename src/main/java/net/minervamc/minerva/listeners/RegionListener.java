package net.minervamc.minerva.listeners;

import net.minervamc.minerva.lib.events.GlobalEventHandler;
import net.minervamc.minerva.minigames.ctf.RegionManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class RegionListener {
    private static final GlobalEventHandler eventHandler = GlobalEventHandler.get();

    public static void register() {
        GlobalEventHandler.get().addListener(PlayerInteractEvent.class, event -> {
            Player player = event.getPlayer();
            if(!RegionManager.isSelectMode(player)) return;

            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK -> {
                    Block block = event.getClickedBlock();
                    if(block == null) return;
                    RegionManager.setPos1(player, block.getLocation());
                    event.setCancelled(true);
                }
                case RIGHT_CLICK_BLOCK -> {
                    Block block = event.getClickedBlock();
                    if(block == null) return;
                    RegionManager.setPos2(player, block.getLocation());
                    event.setCancelled(true);
                }
            }
        });
    }
}
