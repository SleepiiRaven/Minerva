package net.minervamc.minerva.lib.menu;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Menu {
    private static final Map<UUID, Menu> openMenus = new HashMap<>();
    private final Map<Integer, MenuClick> menuClickActions = new HashMap<>();
    @Setter @Getter private MenuClick generalClickAction;
    @Setter @Getter private MenuClick generalInvClickAction;
    @Setter @Getter private MenuDrag generalInvDragAction;
    @Setter @Getter private MenuDrag generalDragAction;
    @Setter @Getter private MenuOpen openAction;
    @Setter @Getter private MenuClose closeAction;
    @Setter @Getter private boolean stopClosing = false;
    @Getter public final UUID uuid;
    @Getter private final Inventory inventory;

    public Menu(int size, Component name) {
        uuid = UUID.randomUUID();
        inventory = Bukkit.createInventory(null, size, name);
    }

    public Menu(Component name, InventoryType type) {
        uuid = UUID.randomUUID();
        inventory = Bukkit.createInventory(null, type, name);
    }

    public static Menu getMenu(Player p) {
        return openMenus.getOrDefault(p.getUniqueId(), null);
    }

    public MenuClick getAction(int index) {
        return menuClickActions.getOrDefault(index, null);
    }

    public void setItem(int index, ItemStack item) {
        inventory.setItem(index, item);
    }

    public void setItem(int index, ItemStack item, MenuClick action) {
        inventory.setItem(index, item);
        if (action == null) menuClickActions.remove(index);
        else menuClickActions.put(index, action);
    }

    public void open(Player p) {
        p.openInventory(inventory);
        openMenus.put(p.getUniqueId(), this);
        if(openAction != null) openAction.open(p);
    }

    public void close(Player p) {
        p.closeInventory();
        openMenus.entrySet().removeIf(entry -> {
            if(!entry.getKey().equals(p.getUniqueId())) return false;
            if(closeAction != null) closeAction.close(p);

            return true;
        });
    }

    public void remove() {
        openMenus.entrySet().removeIf(entry -> {
            if (!entry.getValue().getUuid().equals(uuid)) return false;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) return false;
            if (closeAction != null) closeAction.close(p);

            return true;
        });
    }

    public interface MenuClick {
        void click(Player p, InventoryClickEvent event);
    }

    public interface MenuDrag {
        void drag(Player p, InventoryDragEvent event);
    }

    public interface MenuOpen {
        void open(Player p);
    }

    public interface MenuClose {
        void close(Player p);
    }
}
