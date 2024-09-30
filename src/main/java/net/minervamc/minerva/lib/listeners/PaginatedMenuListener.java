package net.minervamc.minerva.lib.listeners;

import net.minervamc.minerva.lib.menu.PaginatedMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PaginatedMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null) {
            PaginatedMenu menu = PaginatedMenu.getOpenMenus().get(player.getUniqueId());
            if (menu != null && menu.getCurrentPage(player).equals(clickedInventory)) {
                menu.handleClick(event);

                ItemStack itemStack = event.getCurrentItem();
                if(itemStack == null) return;
                if(itemStack.equals(PaginatedMenu.nextPage())) {
                    menu.nextPage(player);
                    event.setCancelled(true);
                }
                if(itemStack.equals(PaginatedMenu.prevPage())) {
                    menu.prevPage(player);
                    event.setCancelled(true);
                }
                if(itemStack.equals(PaginatedMenu.getFiller())) event.setCancelled(true);
                if(itemStack.equals(menu.pageCounter(player))) event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        PaginatedMenu menu = PaginatedMenu.getOpenMenus().get(player.getUniqueId());
        if (menu != null && menu.getCurrentPage(player).equals(event.getInventory())) {
            menu.onMenuClose(player);
            menu.closeForPlayer(player);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        PaginatedMenu menu = PaginatedMenu.getOpenMenus().get(player.getUniqueId());
        if (menu != null && menu.getCurrentPage(player).equals(event.getInventory())) {
            menu.onMenuOpen(player);
        }
    }
}
