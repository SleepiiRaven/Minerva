package net.minervamc.minerva.skills.greek.artemis;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HuntressAgility extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {

    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "huntressAgility";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.FEATHER), ChatColor.BOLD + "" + ChatColor.GRAY + "[Huntress' Agility]", ChatColor.GRAY + "With a Huntress's speed and agility, run quicker while holding a bow.");
    }
}
