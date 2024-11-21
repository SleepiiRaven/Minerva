package net.minervamc.minerva.skills.greek.hephaestus;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class GroundBreaker extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double damageSmolderMultiplier = 4;
        boolean hasSmolder = (getStacks(player, "smolder") > 0);
        int ticksToFall = 20;
        int initialHeight = 20;
        Color[] gradient = {
                Color.fromRGB(79, 28, 13),
                Color.fromRGB(128, 97, 80),
                Color.fromRGB(190, 184, 175),
                Color.fromRGB(87, 80, 71),
                Color.fromRGB(66, 55, 48)
        };
        Color[] gradientSmolder = {
                Color.fromRGB(223,184,119),
                Color.fromRGB(231,165,58),
                Color.fromRGB(207,128,0),
                Color.fromRGB(164,107,15),
                Color.fromRGB(122,82,19)
        };
        int stunTicks = 20;
        double distance = 12;
        long cooldown = 17000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "groundBreaker")) {
            onCooldown(player);
            return;
        }

        RayTraceResult result = player.rayTraceBlocks(20);
        Location loc;
        if (result == null) return;
        if (result.getHitBlock() == null) {
            if (result.getHitEntity() == null) {
                return;
            }
            loc = result.getHitEntity().getLocation();
        } else {
            loc = result.getHitBlock().getLocation();
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "groundBreaker", cooldown);
        cooldownAlarm(player, cooldown, "Ground Breaker");


        loc.setDirection(new Vector(1, 0, 0)).add(-2.5, 0, 2.5);

        BlockDisplay anvil = (BlockDisplay) player.getWorld().spawnEntity(loc.clone().add(0, initialHeight, 0), EntityType.BLOCK_DISPLAY);
        anvil.setBlock(Bukkit.createBlockData(Material.ANVIL));
        anvil.setTransformationMatrix(new Matrix4f().identity().scale(5, 5, 5));

        new BukkitRunnable() {
            boolean hitInitLoc = false;
            @Override
            public void run() {
                Location tpLoc = anvil.getLocation().subtract(0, ((double) initialHeight)/ticksToFall, 0);
                anvil.teleport(tpLoc);
                if (Math.abs(tpLoc.getY() - loc.getY()) < 0.01) {
                    hitInitLoc = true;
                }

                if (tpLoc.clone().add(-2.5, -1, 2.5).getBlock().getType().isSolid() && hitInitLoc) {
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "groundBreaker";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.create(Material.ANVIL);
    }
}
