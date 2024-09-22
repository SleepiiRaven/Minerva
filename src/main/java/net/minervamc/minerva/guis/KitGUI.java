package net.minervamc.minerva.guis;

import net.kyori.adventure.text.Component;
import net.minervamc.minerva.lib.menu.Menu;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KitGUI extends Menu {
    public KitGUI(int size, Component name) {
        super(size, name);
        ItemStack scout = new ItemStack(Material.LEATHER_BOOTS);
        ItemStack attacker = new ItemStack(Material.LEATHER_BOOTS);
        ItemStack defender = new ItemStack(Material.LEATHER_BOOTS);
        setItem(1, scout, (p, event) -> {

        });
    }
}
