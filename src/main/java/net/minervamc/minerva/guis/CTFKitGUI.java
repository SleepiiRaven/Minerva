package net.minervamc.minerva.guis;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class CTFKitGUI extends Menu {
    private final int scoutIndex = 11;
    private final int attackerIndex = 13;
    private final int defenderIndex = 15;

    public CTFKitGUI() {
        super(27, Component.text("Kit Selection"));
        ItemStack blank = ItemCreator.createNameless(Material.BLACK_STAINED_GLASS_PANE);
        MenuUtil.fill(getInventory(), blank);
        ItemCreator scout = ItemCreator.get(Material.DIRT);
        scout.setName(Component.text("Scout", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        List<Component> scoutLore = List.of(Component.text("Using their speed", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("and informative items,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("they can gather critical", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("information about the", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("enemy and play far from the,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("fight, on enemy flank turf.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        scout.setLore(scoutLore);
        //scout.addAttribute(Attribute.GENERIC_ARMOR, 7.0, AttributeModifier.Operation.ADD_NUMBER);
        //scout.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        ItemCreator attacker = ItemCreator.get(Material.WOODEN_SWORD);
        attacker.setName(Component.text("Attacker", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        List<Component> attackerLore = List.of(Component.text("Equipped with extra", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("offensive utilities,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("an attacker kit is", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("used to attack the", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("enemy, steal the flag,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("and is crucial to win.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        attacker.setLore(attackerLore);
        attacker.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 7.0, AttributeModifier.Operation.ADD_NUMBER);
        attacker.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
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
            close(p);
        });
        setItem(attackerIndex, getAttacker(), (p, event) -> {
            close(p);
        });
        setItem(defenderIndex, getDefender(), (p, event) -> {
            close(p);
        });
    }

    private ItemStack getScout() {
        return ItemCreator.get(Material.LEATHER_BOOTS)
                .setName(TextContext.formatLegacy("&bScout", false))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Using their speed", false),
                        TextContext.formatLegacy("&7and informative items,", false),
                        TextContext.formatLegacy("&7they can gather critical", false),
                        TextContext.formatLegacy("&7information about the", false),
                        TextContext.formatLegacy("&7enemy and play far from the", false),
                        TextContext.formatLegacy("&7fight, on enemy flank turf.", false)
                ))
                .addAttribute(Attribute.GENERIC_ARMOR, 7.0, AttributeModifier.Operation.ADD_NUMBER)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
    }

    private ItemStack getAttacker() {
        return ItemCreator.get(Material.WOODEN_SWORD)
                .setName(TextContext.formatLegacy("&cAttacker", false))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Equipped with extra", false),
                        TextContext.formatLegacy("&7offensive utilities,", false),
                        TextContext.formatLegacy("&7an attacker kit is", false),
                        TextContext.formatLegacy("&7used to attack the", false),
                        TextContext.formatLegacy("&7enemy, steal the flag,", false),
                        TextContext.formatLegacy("&7and is crucial to win.", false)
                ))
                .addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 7.0, AttributeModifier.Operation.ADD_NUMBER)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
    }

    private ItemStack getDefender() {
        return ItemCreator.get(Material.SHIELD)
                .setName(TextContext.formatLegacy("&6Defender", false))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Using their protective", false),
                        TextContext.formatLegacy("&7utilities such as blocks", false),
                        TextContext.formatLegacy("&7to place, shields, traps,", false),
                        TextContext.formatLegacy("&7and more, the defender", false),
                        TextContext.formatLegacy("&7kit is one of the best", false),
                        TextContext.formatLegacy("&7kits for a team.", false)
                ))
                .build();
    }

}
