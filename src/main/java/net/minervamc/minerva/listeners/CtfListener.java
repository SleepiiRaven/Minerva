package net.minervamc.minerva.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.region.Region2d;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.minigames.ctf.RegionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class CtfListener implements Listener {
    private static final Logger LOGGER = Minerva.getInstance().getSLF4JLogger();
    
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
                
                player.sendMessage(Component.text("You are carrying the red flag. Take it to your team's side to win!", NamedTextColor.RED));
            }
        } else {
            if (event.getBlock().getType() == Material.BLUE_BANNER) {
                event.setDropItems(false);

                ItemCreator flagCr = ItemCreator.get(Material.BLUE_BANNER);
                flagCr.setName(TextContext.format("Blue Flag", false).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
                player.getInventory().addItem(flagCr.build());
                
                player.sendMessage(Component.text("You are carrying the blue flag. Take it to your team's side to win!", NamedTextColor.BLUE));
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
                if (entity.getType() == EntityType.ARMOR_STAND && entity.getScoreboardTags().contains("ctfTrap")) {
                    return;
                }
            }
            item.setAmount(item.getAmount() - 1);
            ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(trapLoc.add(new Vector(0, 0.2, 0)), EntityType.ARMOR_STAND);
            armorStand.setSmall(true);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.customName(Component.text("Land-Mine Trap"));
            armorStand.setCustomNameVisible(false);
            armorStand.addScoreboardTag("ctfTrap");
            BlockDisplay blockDisplay = (BlockDisplay) player.getWorld().spawnEntity(armorStand.getEyeLocation(), EntityType.BLOCK_DISPLAY);
            armorStand.addPassenger(blockDisplay);
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

    @EventHandler
    public void playerMoveIntoRegion(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        LOGGER.info("In game");
        if (!event.hasChangedBlock()) return;
        LOGGER.info("Changed block");
        String regionOri = "";
        String regionAft = "";
        for (String regionName : RegionManager.listRegions()) {
            Region2d region = RegionManager.getRegion(regionName);
            if (region.contains(event.getFrom())) regionOri = regionName;
            if (region.contains(event.getTo())) regionAft = regionName;
        }
        if (regionAft.isEmpty()) return;
        LOGGER.info("Region is not empty!");
        if (regionOri.equals(regionAft)) return;
        LOGGER.info("Changed Region!");
        CaptureTheFlag.changedRegion(regionOri, regionAft, event);
    }

    @EventHandler
    public void playerShiftLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (!player.isSneaking()) return;
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;

        CaptureTheFlag.skillCast(player);
    }
}
