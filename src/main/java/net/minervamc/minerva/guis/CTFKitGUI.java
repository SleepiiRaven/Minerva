package net.minervamc.minerva.guis;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CTFKitGUI extends Menu {
    public CTFKitGUI() {
        super(36, Component.text("Kit Selection"));
        ItemStack blank = ItemCreator.createNameless(Material.BLACK_STAINED_GLASS);
        MenuUtil.fill(getInventory(), blank);
        ItemCreator scout = ItemCreator.get(Material.LEATHER_BOOTS);
        scout.setName(Component.text("Scout", NamedTextColor.AQUA));
        List<Component> scoutLore = List.of(Component.text("Using their speed", NamedTextColor.GRAY),
            Component.text("and informative items,", NamedTextColor.GRAY),
            Component.text("they can gather critical", NamedTextColor.GRAY),
            Component.text("information about the", NamedTextColor.GRAY),
            Component.text("enemy and play far from the,", NamedTextColor.GRAY),
            Component.text("fight, on enemy flank turf.", NamedTextColor.GRAY));
        scout.setLore(scoutLore);
        ItemCreator attacker = ItemCreator.get(Material.WOODEN_SWORD);
        attacker.setName(Component.text("Attacker", NamedTextColor.RED));
        List<Component> attackerLore = List.of(Component.text("Equipped with extra", NamedTextColor.GRAY),
                Component.text("offensive utilities,", NamedTextColor.GRAY),
                Component.text("an attacker kit is", NamedTextColor.GRAY),
                Component.text("used to attack the", NamedTextColor.GRAY),
                Component.text("enemy, steal the flag,", NamedTextColor.GRAY),
                Component.text("and is crucial to win.", NamedTextColor.GRAY));
        attacker.setLore(attackerLore);
        ItemCreator defender = ItemCreator.get(Material.SHIELD);
        defender.setName(Component.text("Defender", NamedTextColor.GOLD));
        List<Component> defenderLore = List.of(Component.text("Using their protective", NamedTextColor.GRAY),
                Component.text("utilities such as blocks", NamedTextColor.GRAY),
                Component.text("to place, shields, traps,", NamedTextColor.GRAY),
                Component.text("and more, the defender", NamedTextColor.GRAY),
                Component.text("kit is one of the best", NamedTextColor.GRAY),
                Component.text("kits for a team.", NamedTextColor.GRAY));
        defender.setLore(defenderLore);
        setItem(11, scout.build(), (p, event) -> {
        });
        setItem(13, attacker.build(), (p, event) -> {
        });
        setItem(15, defender.build(), (p, event) -> {
        });
    }
}
