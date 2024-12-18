package net.minervamc.minerva.listeners;

import java.util.Arrays;
import java.util.Objects;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.guis.GreekGodsGUI;
import net.minervamc.minerva.guis.MythicalCreaturesGUI;
import net.minervamc.minerva.guis.RomanGodsGUI;
import net.minervamc.minerva.guis.SkillsGUI;
import net.minervamc.minerva.guis.TitansGUI;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final Minerva plugin = Minerva.getInstance();
    CooldownManager cooldownManager = plugin.getCdInstance();

    // public static Map<Player, NPC> npcs = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (p.hasMetadata("NPC")) return;

        // This will not be implemented until Aphrodite is released.
//        // INITIALIZE NPC WITH PLAYER'S SKIN FOR QUICK LOADING
//        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, p.getName());
//        npc.getOrAddTrait(SkinTrait.class).setSkinName(p.getName());
//        npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
//        npc.spawn(p.getLocation());
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                npc.despawn();
//            }
//        }.runTaskLater(Minerva.getInstance(), 20L);
//        npcs.put(p, npc);

        cooldownManager.createContainer(p.getUniqueId());
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        if (pData.getLogoutLoc() == null) {
            pData.setLogoutLoc(p.getLocation());
        } else {
            p.teleport(pData.getLogoutLoc());
        }

        if (!Arrays.stream(pData.getInventory()).allMatch(Objects::isNull)) {
            p.getInventory().setStorageContents(pData.getInventory());
        }
        if (!Arrays.stream(pData.getArmor()).allMatch(Objects::isNull)) {
            p.getInventory().setArmorContents(pData.getArmor());
        }
        if (pData.getOffhand()[0] != null) {
            p.getInventory().setItemInOffHand(pData.getOffhand()[0]);
        }

        pData.setInventory(new ItemStack[36]);
        pData.setArmor(new ItemStack[4]);
        pData.setOffhand(new ItemStack[1]);
        pData.save();
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (p.hasMetadata("NPC")) return;

        Minerva.runPermRemoveCommand(p, "venturechat.blueteamchat");
        Minerva.runPermRemoveCommand(p, "venturechat.redteamchat");
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        if (CaptureTheFlag.isPlaying() && CaptureTheFlag.isInGame(p)) {
            pData.setLogoutLoc(CaptureTheFlag.getStartLoc().get(p.getUniqueId()));
        } else {
            pData.setLogoutLoc(p.getLocation());
        }
        pData.save();
    }

    @EventHandler
    public void playerLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("NPC")) return;

        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());

        if (playerStats.skillMode) {
            // PlayerInteractEvent doesn't work with LEFT_CLICK_BLOCK in adventure mode, so using this for that.
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click") ||
                event.getAnimationType() != PlayerAnimationType.ARM_SWING ||
                !SkillUtils.isFocus(player.getInventory().getItemInMainHand()))
                    return;
            long cooldown = 50;

            if (playerStats.skillTriggers.spellMode) {
                playerStats.skillTriggers.continueNormalSpell(Action.LEFT_CLICK_AIR, player.getInventory().getItemInMainHand().getType() == Material.BOW || player.getInventory().getItemInMainHand().getType() == Material.TRIDENT);
                cooldownManager.setCooldownFromNow(player.getUniqueId(), "Spell Click", cooldown);
                return;
            }

            if (player.getInventory().getItemInMainHand().getType() == Material.BOW || player.getInventory().getItemInMainHand().getType() == Material.TRIDENT) {
                playerStats.skillTriggers.enterSpellMode(player, true);
            }
        }
    }

    @EventHandler
    public void playerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        Action action = event.getAction();
        if (playerStats.skillMode) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click") || !(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) || !SkillUtils.isFocus(player.getInventory().getItemInMainHand()))
                return;
            long cooldown = 50;
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "Spell Click", cooldown);
            if (playerStats.skillTriggers.spellMode) {
                playerStats.skillTriggers.continueNormalSpell(action, player.getInventory().getItemInMainHand().getType() == Material.BOW || player.getInventory().getItemInMainHand().getType() == Material.TRIDENT);
                return;
            }

            if (player.getInventory().getItemInMainHand().getType() != Material.BOW || player.getInventory().getItemInMainHand().getType() == Material.TRIDENT) {
                playerStats.skillTriggers.enterSpellMode(player, false);
            }
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        switch (ChatColor.stripColor(event.getView().getTitle())) {
            case AncestryGUI.invName -> AncestryGUI.clickedGUI(event);
            case GreekGodsGUI.invName -> GreekGodsGUI.clickedGUI(event);
            case RomanGodsGUI.invName -> RomanGodsGUI.clickedGUI(event);
            case TitansGUI.invName -> TitansGUI.clickedGUI(event);
            case MythicalCreaturesGUI.invName -> MythicalCreaturesGUI.clickedGUI(event);
        }
        if (ChatColor.stripColor(event.getView().getTitle()).contains(SkillsGUI.invName)) {
            SkillsGUI.clickedGUI(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerDamagePlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() == event.getEntity()) return;
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player player) {
            if (Party.isPlayerInPlayerParty(damager, player)) event.setCancelled(true);
        }
    }
}
