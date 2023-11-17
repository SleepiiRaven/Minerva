package net.minervamc.minerva.skills;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DefaultSkill extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        player.sendMessage(ChatColor.RED + "You do not have a skill bound to this slot.");
    }

    @Override
    public String getLevelDescription(int level) {
        return "YOU SHOULD NOT BE SEEING THIS!";
    }

    @Override
    public String toString() {
        return "default";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BARRIER), ChatColor.RED + "" + ChatColor.BOLD + "[ERROR]", ChatColor.RED + "Please report this to a moderator.");
    }
}
