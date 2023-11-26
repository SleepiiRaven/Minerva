package net.minervamc.minerva.skills.greek.artemis;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
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
        return null;
    }
}
