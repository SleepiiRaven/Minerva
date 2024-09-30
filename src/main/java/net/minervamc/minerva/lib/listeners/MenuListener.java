package net.minervamc.minerva.lib.listeners;

import net.minervamc.minerva.lib.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {

    @EventHandler
    private void inventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Menu menu = Menu.getMenu(player);
        if (menu == null) return;
        event.setCancelled(true);

        if (event.getRawSlots().stream().anyMatch(slot -> slot >= player.getOpenInventory().getTopInventory().getSize())) {
            if (menu.getGeneralInvDragAction() != null) menu.getGeneralInvDragAction().drag(player, event);
        }else if (menu.getGeneralDragAction() != null) {
            menu.getGeneralDragAction().drag(player, event);
        }
    }

    @EventHandler
    private void inventoryClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        Menu menu = Menu.getMenu(p);
        if(menu == null || event.getClickedInventory() == null) return;
        event.setCancelled(true);

        if(event.getRawSlot() >= p.getOpenInventory().getTopInventory().getSize()){
            if(menu.getGeneralInvClickAction() != null) menu.getGeneralInvClickAction().click(p, event);
        }else if(menu.getGeneralClickAction() != null){
            menu.getGeneralClickAction().click(p, event);
        }

        Menu.MenuClick menuClick = menu.getAction(event.getRawSlot());
        if(menuClick != null) menuClick.click(p, event);
    }

    @EventHandler
    private void inventoryClose(InventoryCloseEvent event) {
        Player p = (Player) event.getPlayer();
        Menu menu = Menu.getMenu(p);
        if(menu != null) menu.remove();
    }
}