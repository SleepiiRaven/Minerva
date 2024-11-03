package net.minervamc.minerva.skills.greek.artemis;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class NimbleDash extends Skill {


    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int dashTime = 3; // Ticks
        int invulnerabilityTicks = 10; // Ticks
        int dashVelocityMultiplier = 5;
        long cooldown = 4000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "nimbleDash")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "nimbleDash", cooldown);
        cooldownAlarm(player, cooldown, "Nimble Dash");

        player.playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);
        player.setNoDamageTicks(invulnerabilityTicks);
        player.setVelocity(player.getLocation().getDirection().multiply(dashVelocityMultiplier).setY(Math.min(2, player.getLocation().getDirection().getY())));
        new BukkitRunnable() {
            final int ticks = 0;

            @Override
            public void run() {
                player.setVelocity(new Vector(0, 0, 0));
            }
        }.runTaskLater(Minerva.getInstance(), dashTime);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "nimbleDash";
    }

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta iM = item.getItemMeta();
        iM.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        iM.addItemFlags(ItemFlag.HIDE_DYE);
        ((LeatherArmorMeta) iM).setColor(Color.WHITE);
        item.setItemMeta(iM);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ChatColor.BOLD + "" + ChatColor.WHITE + "[Nimble Dash]"));

        List<String> lores = new ArrayList<>();
        lores.add(ChatColor.translateAlternateColorCodes('&', ChatColor.GRAY + "Using the athleticism of a Huntress, dash quickly in a chosen direction."));
        lores.add(ChatColor.translateAlternateColorCodes('&', ChatColor.GRAY + "After casting, you dodge all attacks for a small time period."));

        meta.setLore(lores);

        item.setItemMeta(meta);

        return item;
    }
}
