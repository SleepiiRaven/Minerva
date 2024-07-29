package net.minervamc.minerva.utils;

import net.kyori.adventure.text.Component;
import net.minervamc.minerva.api.text.Text;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SpellCastUtils {

    private static final Component FOCUSED = Text.formatLegacy("&aFocused", true);
    private static final Component UNFOCUSED = Text.formatLegacy("&cUnfocused", true);

    public static void focusSpellCast(ItemStack item) {
        if(isSpellCastFocused(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.SPELL_FOCUS, PersistentDataType.STRING, "");

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.remove(UNFOCUSED);
        lore.add(FOCUSED);
        meta.lore(lore);

        item.setItemMeta(meta);
    }

    public static void unfocusSpellCast(ItemStack item) {
        if(!item.hasItemMeta()) return;
        if(!isSpellCastFocused(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(Keys.SPELL_FOCUS);

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.remove(FOCUSED);
        lore.add(UNFOCUSED);
        meta.lore(lore);

        item.setItemMeta(meta);
    }

    public static void toggleFocus(ItemStack item) {
        if(isSpellCastFocused(item)) unfocusSpellCast(item);
        else focusSpellCast(item);
    }

    public static boolean isSpellCastFocused(ItemStack item) {
        if(!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.SPELL_FOCUS);
    }


}
