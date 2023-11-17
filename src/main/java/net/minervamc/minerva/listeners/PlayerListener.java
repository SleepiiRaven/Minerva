package net.minervamc.minerva.listeners;

import com.google.common.eventbus.DeadEvent;
import java.util.Objects;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.guis.GreekGodsGUI;
import net.minervamc.minerva.guis.MythicalCreaturesGUI;
import net.minervamc.minerva.guis.RomanGodsGUI;
import net.minervamc.minerva.guis.SkillsGUI;
import net.minervamc.minerva.guis.TitansGUI;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.PlayerStatsAdapter;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final Minerva plugin = Minerva.getInstance();
    CooldownManager cooldownManager = plugin.getCdInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        cooldownManager.createContainer(p.getUniqueId());
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        pData.save();
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        pData.save();
    }

    @EventHandler
    public void playerLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        // PlayerInteractEvent doesn't work with LEFT_CLICK_BLOCK in adventure mode, so using this for that.
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click") || event.getAnimationType() != PlayerAnimationType.ARM_SWING || !ItemUtils.weapons.contains(player.getInventory().getItemInMainHand().getType()))
            return;
        long cooldown = 10;
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        if (playerStats.skillTriggers.spellMode) {
            playerStats.skillTriggers.continueNormalSpell(Action.LEFT_CLICK_AIR);
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "Spell Click", cooldown);
        }
    }

    @EventHandler
    public void playerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click") || action != Action.RIGHT_CLICK_AIR || !ItemUtils.weapons.contains(player.getInventory().getItemInMainHand().getType()))
            return;
        long cooldown = (10);
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "Spell Click", cooldown);
        if (playerStats.skillTriggers.spellMode) {
            playerStats.skillTriggers.continueNormalSpell(action);
            return;
        }

        playerStats.skillTriggers.enterSpellMode(player);
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        switch (ChatColor.stripColor(event.getView().getTitle())) {
            case SkillsGUI.invName:
                SkillsGUI.clickedGUI(event);
                break;
            case AncestryGUI.invName:
                AncestryGUI.clickedGUI(event);
                break;
            case GreekGodsGUI.invName:
                GreekGodsGUI.clickedGUI(event);
                break;
            case RomanGodsGUI.invName:
                RomanGodsGUI.clickedGUI(event);
                break;
            case TitansGUI.invName:
                TitansGUI.clickedGUI(event);
                break;
            case MythicalCreaturesGUI.invName:
                MythicalCreaturesGUI.clickedGUI(event);
                break;
        }
    }

    @EventHandler
    public void playerOpenInventory(InventoryOpenEvent event) {
        ItemUtils.resetItemDisplayName(event.getPlayer().getItemInHand());
    }

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent event) {
        ItemUtils.resetItemDisplayName(event.getItemDrop().getItemStack());
    }

    @EventHandler
    public void playerSwapSlots(PlayerItemHeldEvent event) {
        ItemStack previousItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        if (previousItem != null) ItemUtils.resetItemDisplayName(previousItem);
    }
}
