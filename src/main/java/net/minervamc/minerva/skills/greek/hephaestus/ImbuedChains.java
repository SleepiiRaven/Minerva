package net.minervamc.minerva.skills.greek.hephaestus;

import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ImbuedChains extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 17000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "imbuedChains")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "imbuedChains", cooldown);
        cooldownAlarm(player, cooldown, "Imbued Chains");
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "imbuedChains";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.create(Material.CHAIN);
    }
}
