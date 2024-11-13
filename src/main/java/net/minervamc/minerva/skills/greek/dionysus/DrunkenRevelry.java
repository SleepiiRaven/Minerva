package net.minervamc.minerva.skills.greek.dionysus;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class DrunkenRevelry extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "drunkenRevelry";
    }

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(Color.GREEN);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Drunken Revelry]"));

        List<String> lores = new ArrayList<>();
        lores.add(ChatColor.translateAlternateColorCodes('&', ChatColor.GRAY + "Whenever you are poisoned, gain strength and regeneration."));

        meta.setLore(lores);
        item.setItemMeta(meta);
        return item;
    }
}
