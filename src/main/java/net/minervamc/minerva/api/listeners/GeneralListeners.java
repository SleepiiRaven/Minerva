package net.minervamc.minerva.api.listeners;

import net.minervamc.minerva.api.events.GlobalEventHandler;
import net.minervamc.minerva.api.menu.Menu;
import net.minervamc.minerva.api.menu.PaginatedMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GeneralListeners {
    private static final  GlobalEventHandler eventHandler = GlobalEventHandler.get();

    public static void register() {
        registerMenuListener();
        registerPaginatedMenuListener();
    }

    private static void registerMenuListener() {
        eventHandler.addListener(InventoryClickEvent.class, event-> {
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
        });
        eventHandler.addListener(InventoryDragEvent.class, event-> {
            Player player = (Player) event.getWhoClicked();
            Menu menu = Menu.getMenu(player);
            if (menu == null) return;
            event.setCancelled(true);

            if (event.getRawSlots().stream().anyMatch(slot -> slot >= player.getOpenInventory().getTopInventory().getSize())) {
                if (menu.getGeneralInvDragAction() != null) menu.getGeneralInvDragAction().drag(player, event);
            }else if (menu.getGeneralDragAction() != null) {
                menu.getGeneralDragAction().drag(player, event);
            }
        });
        eventHandler.addListener(InventoryCloseEvent.class, event-> {
            Player p = (Player) event.getPlayer();
            Menu menu = Menu.getMenu(p);
            if(menu != null) menu.remove();
        });
    }

    private static void registerPaginatedMenuListener() {
        eventHandler.addListener(InventoryClickEvent.class, event-> {
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
        });
        eventHandler.addListener(InventoryCloseEvent.class, event-> {
            if (!(event.getPlayer() instanceof Player player)) return;

            PaginatedMenu menu = PaginatedMenu.getOpenMenus().get(player.getUniqueId());
            if (menu != null && menu.getCurrentPage(player).equals(event.getInventory())) {
                menu.onMenuClose(player);
                menu.closeForPlayer(player);
            }
        });
        eventHandler.addListener(InventoryOpenEvent.class, event-> {
            if (!(event.getPlayer() instanceof Player player)) return;

            PaginatedMenu menu = PaginatedMenu.getOpenMenus().get(player.getUniqueId());
            if (menu != null && menu.getCurrentPage(player).equals(event.getInventory())) {
                menu.onMenuOpen(player);
            }
        });
    }
}
