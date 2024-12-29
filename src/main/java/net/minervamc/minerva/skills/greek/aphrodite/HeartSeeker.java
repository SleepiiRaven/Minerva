package net.minervamc.minerva.skills.greek.aphrodite;

import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HeartSeeker extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "heartSeeker")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "heartSeeker", cooldown);
        cooldownAlarm(player, cooldown, "Heart Seeker");
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "heartSeeker";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
