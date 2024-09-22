package net.minervamc.minerva.guis;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class CTFKitGUI extends Menu {
    public CTFKitGUI() {
        super(36, Component.text("Kit Selection"));
        ItemStack blank = ItemCreator.createNameless(Material.BLACK_STAINED_GLASS);
        MenuUtil.fill(getInventory(), blank);
        ItemCreator scout = ItemCreator.get(Material.LEATHER_BOOTS);
        scout.setName(Component.text("Scout", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        List<Component> scoutLore = List.of(Component.text("Using their speed", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("and informative items,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("they can gather critical", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("information about the", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("enemy and play far from the,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("fight, on enemy flank turf.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        scout.setLore(scoutLore);

        scout.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        scout.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        ItemCreator attacker = ItemCreator.get(Material.WOODEN_SWORD);
        attacker.setName(Component.text("Attacker", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        List<Component> attackerLore = List.of(Component.text("Equipped with extra", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("offensive utilities,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("an attacker kit is", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("used to attack the", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("enemy, steal the flag,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("and is crucial to win.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        attacker.setLore(attackerLore);

        attacker.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        attacker.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        ItemCreator defender = ItemCreator.get(Material.SHIELD);
        defender.setName(Component.text("Defender", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> defenderLore = List.of(Component.text("Using their protective", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("utilities such as blocks", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("to place, shields, traps,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("and more, the defender", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("kit is one of the best", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("kits for a team.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        defender.setLore(defenderLore);
        setItem(11, scout.build(), (p, event) -> {
        });
        setItem(13, attacker.build(), (p, event) -> {
        });
        setItem(15, defender.build(), (p, event) -> {
        });
    }
}
