package net.minervamc.minerva.skills.greek.aphrodite;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BlindingLove extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {

    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "blindingLove";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
