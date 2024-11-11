package net.minervamc.minerva.skills.greek.ares;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AresBlessing extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {

    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "aresBlessing";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ROTTEN_FLESH), ChatColor.BOLD + "" + ChatColor.DARK_RED + "Ares' Blessing", ChatColor.GRAY + "Scale your power in", ChatColor.GRAY + "any fight by gaining", ChatColor.GRAY + "a stackable burst of", ChatColor.GRAY + "Strength each time", ChatColor.GRAY + "you kill a living entity", ChatColor.GRAY + "(including your summons).");
    }
}
