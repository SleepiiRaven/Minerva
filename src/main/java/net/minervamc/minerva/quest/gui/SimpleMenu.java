package net.minervamc.minerva.quest.gui;

import net.kyori.adventure.text.Component;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import org.bukkit.Material;

/** A concrete, instantiable {@link Menu} for building one-off editor screens with a border and back button. */
public class SimpleMenu extends Menu {
    public SimpleMenu(int size, Component name) {
        super(size, name);
    }

    public void fillBorder() {
        MenuUtil.addBorders(getInventory(), Material.GRAY_STAINED_GLASS_PANE);
    }

    public void setBackButton(Runnable back) {
        setBackButton(back, getInventory().getSize() - 1);
    }

    public void setBackButton(Runnable back, int slot) {
        setItem(slot, ItemCreator.create(Component.text("← Back"), Material.ARROW), (p, e) -> {
            if (back != null) back.run();
        });
    }
}
