package net.minervamc.minerva.listeners;

import java.util.Arrays;
import java.util.Objects;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.guis.GreekGodsGUI;
import net.minervamc.minerva.guis.MythicalCreaturesGUI;
import net.minervamc.minerva.guis.RomanGodsGUI;
import net.minervamc.minerva.guis.SkillsGUI;
import net.minervamc.minerva.guis.TitansGUI;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final Minerva plugin = Minerva.getInstance();
    CooldownManager cooldownManager = plugin.getCdInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        cooldownManager.createContainer(p.getUniqueId());
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
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
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        pData.save();
    }

    @EventHandler
    public void playerLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());

        if (playerStats.skillMode) {
            // PlayerInteractEvent doesn't work with LEFT_CLICK_BLOCK in adventure mode, so using this for that.
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click") || event.getAnimationType() != PlayerAnimationType.ARM_SWING || !ItemUtils.weapons.contains(player.getInventory().getItemInMainHand().getType()))
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
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        Action action = event.getAction();
        if (playerStats.skillMode) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click") || !(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) || !ItemUtils.weapons.contains(player.getInventory().getItemInMainHand().getType()))
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

    @EventHandler
    public void playerDamagePlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player player) {
            if (Party.isPlayerInPlayerParty(damager, player)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerPickUpItem(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (CaptureTheFlag.inBlueTeam(player)) {
            if (event.getBlock().getType() == Material.RED_BANNER) {
                event.setDropItems(false);

                ItemCreator flagCr = ItemCreator.get(Material.RED_BANNER);
                flagCr.setName(TextContext.format("Red Flag", false).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                player.getInventory().addItem(flagCr.build());

                player.sendMessage(ChatColor.RED + "You are carrying the red flag. Take it to your team's side to win!");
            }
        } else {
            if (event.getBlock().getType() == Material.BLUE_BANNER) {
                event.setDropItems(false);

                ItemCreator flagCr = ItemCreator.get(Material.BLUE_BANNER);
                flagCr.setName(TextContext.format("Blue Flag", false).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
                player.getInventory().addItem(flagCr.build());

                player.sendMessage(ChatColor.BLUE + "You are carrying the blue flag. Take it to your team's side to win!");
            }
        }
    }

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        if (CaptureTheFlag.inBlueTeam(player)) {
            if (event.getItemDrop().getItemStack().getType() == Material.BLUE_BANNER) {
                event.setCancelled(true);
            }
        } else {
            if (event.getItemDrop().getItemStack().getType() == Material.RED_BANNER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerTriggerTrap(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedBlock()) return;
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        for (Entity e : player.getNearbyEntities(1, 1, 1)) {
            if (e.getType() == EntityType.ARMOR_STAND && e.getScoreboardTags().contains("ctfTrap")) {
                CaptureTheFlag.triggerTrap(e, player);
                e.getPassengers().forEach(p -> {
                    e.removePassenger(p);
                    p.remove();
                });
                e.remove();
                return;
            }
        }
    }

    @EventHandler
    public void playerDisarmTrap(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        if (item.getItemMeta().itemName().toString().contains("Defuse")) {
            player.sendMessage("Holding defuse kit");
            RayTraceResult result = player.rayTraceBlocks(4);
            if (result == null) return;
            if (result.getHitBlock() == null) return;
            player.sendMessage("You are looking at block, defusing!");
            Location trapLoc = result.getHitBlock().getLocation();
            for (Entity entity : trapLoc.getNearbyEntities(1, 1, 1)) {
                if (entity.getType() == EntityType.ARMOR_STAND && entity.getScoreboardTags().contains("ctfTrap")) {
                    CaptureTheFlag.defuseTrap(entity, player);
                    entity.getPassengers().forEach(p -> {
                        entity.removePassenger(p);
                        p.remove();
                    });
                    entity.remove();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void playerSetTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isRightClick()) return;
        player.sendMessage("Is right click");
        if (!CaptureTheFlag.isPlaying()) return;
        player.sendMessage("Is playing");
        if (!CaptureTheFlag.isInGame(player)) return;
        player.sendMessage("Is in game");
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        player.sendMessage("rah not air");
        if (item.getItemMeta().itemName().toString().contains("Trap")) {
            player.sendMessage("Holding trap");
            RayTraceResult result = player.rayTraceBlocks(4);
            if (result == null) return;
            if (result.getHitBlock() == null || !result.getHitBlock().isSolid()) return;
            player.sendMessage("You are looking at block, placing!");
            Location trapLoc = result.getHitBlock().getLocation();
            for (Entity entity : trapLoc.getNearbyEntities(1, 1, 1)) {
                if(entity.getType() == EntityType.ARMOR_STAND && entity.getScoreboardTags().contains("ctfTrap")) {
                    return;
                }
            }
            item.setAmount(item.getAmount() - 1);
            ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(trapLoc.add(new Vector(0, 0.2, 0)), EntityType.ARMOR_STAND);
            armorStand.setSmall(true);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setCustomName("Land-Mine Trap");
            armorStand.setCustomNameVisible(false);
            armorStand.addScoreboardTag("ctfTrap");
            BlockDisplay blockDisplay = (BlockDisplay) player.getWorld().spawnEntity(armorStand.getEyeLocation(), EntityType.BLOCK_DISPLAY);
            armorStand.setPassenger(blockDisplay);
            blockDisplay.setBlock(Material.STONE_PRESSURE_PLATE.createBlockData());
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(0.2f, 0.2f, 0.2f),
                    new AxisAngle4f()
            ));
            blockDisplay.addScoreboardTag("ctfVisualTrap");
            CaptureTheFlag.addTrap(armorStand, player);
        }
    }
}
