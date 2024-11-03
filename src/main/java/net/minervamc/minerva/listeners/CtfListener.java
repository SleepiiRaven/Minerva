package net.minervamc.minerva.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.region.Region2d;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.minigames.ctf.RegionManager;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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
            if (event.getBlock().getType() == Material.RED_BANNER || event.getBlock().getType() == Material.RED_WALL_BANNER) {
                event.setDropItems(false);

                ItemCreator flagCr = ItemCreator.get(Material.RED_BANNER);
                flagCr.setName(TextContext.format("Red Flag", false).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                //player.getInventory().addItem(flagCr.build());
                player.getInventory().setHelmet(flagCr.build()); // banner on head
                
                player.sendMessage(Component.text("You are carrying the red flag. Take it to your side and right click your team's flag to win!", NamedTextColor.RED));
                CaptureTheFlag.warnFlag(player, "red");
            }
        } else {
            if (event.getBlock().getType() == Material.BLUE_BANNER || event.getBlock().getType() == Material.BLUE_WALL_BANNER) {
                event.setDropItems(false);

                ItemCreator flagCr = ItemCreator.get(Material.BLUE_BANNER);
                flagCr.setName(TextContext.format("Blue Flag", false).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
                //player.getInventory().addItem(flagCr.build());
                player.getInventory().setHelmet(flagCr.build()); // banner on head
                
                player.sendMessage(Component.text("You are carrying the blue flag. Take it to your side and right click your team's flag to win!", NamedTextColor.BLUE));
                CaptureTheFlag.warnFlag(player, "blue");
            }
        }
    }

    @EventHandler
    public void playerRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        if (event.getClickedBlock() == null || player.getInventory().getHelmet() == null) return;
        if (CaptureTheFlag.inBlueTeam(player)) {
            if (event.getClickedBlock().getType() == Material.BLUE_BANNER || event.getClickedBlock().getType() == Material.BLUE_WALL_BANNER) {
                if (player.getInventory().getHelmet().getType() == Material.RED_BANNER) {
                    CaptureTheFlag.stop("blue");
                }
            }
        } else {
            if (event.getClickedBlock().getType() == Material.RED_BANNER || event.getClickedBlock().getType() == Material.RED_WALL_BANNER) {
                if (player.getInventory().getHelmet().getType() == Material.BLUE_BANNER) {
                    CaptureTheFlag.stop("red");
                }
            }
        }
    }

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void playerClickInventoryItem(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && CaptureTheFlag.isPlaying() && CaptureTheFlag.isInGame((Player)event.getWhoClicked())) {
            event.setCancelled(true);
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
            RayTraceResult result = player.rayTraceEntities(4);
            if (result == null) return;
            if (result.getHitEntity() == null) return;
            Entity entity = result.getHitEntity();
            if (entity.getType() == EntityType.ARMOR_STAND && entity.getScoreboardTags().contains("ctfTrap")) {
                CaptureTheFlag.defuseTrap(entity, player);
                entity.getNearbyEntities(0.1, 0.1, 0.1).forEach(p -> {
                    if (p instanceof BlockDisplay && p.getScoreboardTags().contains("ctfVisualTrap")) p.remove();
                });
                entity.remove();
            }
        }
    }

    @EventHandler
    public void playerSetTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isRightClick()) return;
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        if (item.getItemMeta().itemName().toString().strip().contains("Trap")) {
            List<Vector> linePoints = ParticleUtils.getLinePoints(player.getLocation().getDirection(), 4, 0.2);
            Location trapLoc = null;
            for (Vector v : linePoints) {
                if (player.getEyeLocation().add(v).getBlock().isSolid()) {
                    trapLoc = player.getEyeLocation().add(v).setDirection(new Vector(1, 0, 0));
                    break;
                }
            }
            if (trapLoc == null) return;
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
            BlockDisplay blockDisplay = (BlockDisplay) player.getWorld().spawnEntity(trapLoc, EntityType.BLOCK_DISPLAY);
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
        // check if player is on death cooldown
        CooldownManager cdInstance = Minerva.getInstance().getCdInstance();
        if (!cdInstance.isCooldownDone(player.getUniqueId(), "deathCD")) {
            event.setCancelled(true);
            return;
        }

//        // check if player is in water
//        if (player.isInWater()) {
//            if (!cdInstance.isCooldownDone(player.getUniqueId(), "waterDeathCTF")) return;
//
//            cdInstance.setCooldownFromNow(player.getUniqueId(), "waterDeathCTF", 200L);
//            deathReset(player, cdInstance);
//        }

        if (!event.hasChangedBlock()) return;
        String regionOri = "";
        String regionAft = "";
        for (String regionName : RegionManager.listRegions()) {
            Region2d region = RegionManager.getRegion(regionName);
            if (region.contains(event.getFrom())) regionOri = regionName;
            if (region.contains(event.getTo())) regionAft = regionName;
        }
        if (regionAft.isEmpty()) return;
        if (regionOri.equals(regionAft)) return;
        CaptureTheFlag.changedRegion(regionOri, regionAft, event);
    }

    @EventHandler
    public void playerShiftLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (!player.isSneaking()) return;
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;

        CooldownManager cdInstance = Minerva.getInstance().getCdInstance();
        if (!cdInstance.isCooldownDone(player.getUniqueId(), "ctfSkill")) {
            Skill.onCooldown(player);
            return;
        }
        CaptureTheFlag.skillCast(player, cdInstance);
    }

    public void deathReset(Player player, CooldownManager cdInstance) {
        if (player.getKiller() != null) {
            if (player.getKiller() instanceof Player killer) {
                killer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 2f, 1.5f);
                player.sendMessage(Component.text("You died to " + killer.getName() + "!", NamedTextColor.YELLOW));
                killer.sendMessage(Component.text("You killed " + player.getName() + "!", NamedTextColor.YELLOW));
            }
        }

        if (CaptureTheFlag.inBlueTeam(player)) {
            CaptureTheFlag.autoPlaceBanner("blue");
        } else {
            CaptureTheFlag.autoPlaceBanner("red");
        }
        if (CaptureTheFlag.hasBlueFlag(player)) {
            Location blueFlagLocation = CaptureTheFlag.blueFlagLocation;
            if (blueFlagLocation == null) throw new IllegalStateException(player + " died with the blue flag but it has no original location, how was it captured?");
            Material mat = Material.BLUE_BANNER;
            if (CaptureTheFlag.blueFlagWall) {
                mat = Material.BLUE_WALL_BANNER;
            }
            blueFlagLocation.getBlock().setType(mat);
            player.getInventory().setHelmet(null);
            player.sendMessage(Component.text("You lost the flag."));
        } else if (CaptureTheFlag.hasRedFlag(player)) {
            Location redFlagLocation = CaptureTheFlag.redFlagLocation;
            if(redFlagLocation == null) throw new IllegalStateException(player + " died with the red flag but it has no original location, how was it captured?");
            Material mat = Material.RED_BANNER;
            if (CaptureTheFlag.redFlagWall) {
                mat = Material.RED_WALL_BANNER;
            }
            redFlagLocation.getBlock().setType(mat);
            player.getInventory().setHelmet(null);
            player.sendMessage(Component.text("You lost the flag."));
        }

        CaptureTheFlag.tpSpawn(player);

        double deathTimer = (double) 18 / CaptureTheFlag.playerCount();
        int ticks = (int) (deathTimer * 20);
        int remainder = ticks % 20;
        new BukkitRunnable() {
            int seconds = (ticks - remainder)/20;
            @Override
            public void run() {
                Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
                Title title = Title.title(
                        Component.text("You Died!", NamedTextColor.RED),
                        Component.text("You will respawn in " + seconds + " seconds."), times
                );

                player.showTitle(title);
                player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 1f);
                if (seconds == 0) {
                    this.cancel();
                }
                seconds -= 1;
            }
        }.runTaskTimer(Minerva.getInstance(), remainder, 20);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 10));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 10));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, 10));
        cdInstance.setCooldownFromNow(player.getUniqueId(), "deathCD", (long) (deathTimer * 1000));
    }

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        deathReset(player, Minerva.getInstance().getCdInstance());
        event.setCancelled(true);
    }

    @EventHandler
    public void playerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        Block block = event.getBlock();

        switch (block.getType()) {
            case RED_BANNER -> CaptureTheFlag.redFlagLocation = block.getLocation();
            case RED_WALL_BANNER -> {
                CaptureTheFlag.redFlagLocation = block.getLocation();
                CaptureTheFlag.redFlagWall = true;
            }
            case BLUE_BANNER -> CaptureTheFlag.blueFlagLocation = block.getLocation();
            case BLUE_WALL_BANNER -> {
                CaptureTheFlag.blueFlagLocation = block.getLocation();
                CaptureTheFlag.blueFlagWall = true;
            }
            case BAMBOO_MOSAIC -> CaptureTheFlag.placeBlock(block.getLocation());
        }
    }

    @EventHandler
    public void playerTakeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        if (!player.getScoreboardTags().contains("ctfParryAbility")) return;

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);

        Entity damager = event.getDamager();
        if (!(damager instanceof LivingEntity lE)) return;
        lE.damage(event.getDamage() / 2);
        event.setCancelled(true);
    }

    @EventHandler
    public void playerLeaveEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (CaptureTheFlag.isInQueue(player)) {
            CaptureTheFlag.removeQueue(player);
        }

        if (!CaptureTheFlag.isPlaying()) return;
        if (!CaptureTheFlag.isInGame(player)) return;
        CaptureTheFlag.removeFromGame(player);
    }
}
