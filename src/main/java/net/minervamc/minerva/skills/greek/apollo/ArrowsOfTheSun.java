package net.minervamc.minerva.skills.greek.apollo;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArrowsOfTheSun extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {

    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> "?";
            case 2 -> "??";
            case 3 -> "???";
            case 4 -> "????";
            case 5 -> "?????";
            default -> "-";
        };
    }

    @Override
    public String toString() {
        return "arrowsOfTheSun";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SPECTRAL_ARROW), ChatColor.GOLD + "" + ChatColor.BOLD + "[Arrows of the Sun]", ChatColor.GRAY + "When you shoot an arrow, it has a chance of being an Arrow of the Sun,", ChatColor.GRAY + "dealing extra damage in a small radius of where the arrow hits.");
    }
}
