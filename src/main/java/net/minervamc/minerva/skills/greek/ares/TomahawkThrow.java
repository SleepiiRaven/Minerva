package net.minervamc.minerva.skills.greek.ares;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class TomahawkThrow extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double speed = 0.4; // Modify this to your liking :)
        final int ROTATE = 90;
        int rotationSpeed = 40;
        int duration = 60;

//        double rotate = 90;
        // Effect before loop
        ArmorStand axe = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        axe.setInvulnerable(true);
        axe.setInvisible(true);
        axe.setSmall(true);
        axe.setGravity(false);
        // Set its mainhand to true here as well and rotate the arm
        //axe.setRightArmPose(new EulerAngle(Math.toRadians(rotate), 0, 0));
        axe.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
        new BukkitRunnable() {
            // These variables get set at the start of the runnable and don't get changed
            final Location location = player.getLocation();
            final Vector direction = player.getLocation().getDirection();

            int ticks = 0;


            @Override
            public void run()
            {
                if(ticks>=duration){
                    this.cancel();
                    axe.remove();
                }
                //rotation speed of the axe
                int rotation = ROTATE+(ticks*rotationSpeed);
                axe.setRightArmPose(new EulerAngle(Math.toRadians(rotation), 0, 0));

                for (Entity entityAxe : axe.getLocation().getNearbyEntities(1, 1, 1)) {
                    if (entityAxe instanceof LivingEntity livingEntity && livingEntity != axe && livingEntity != player) {
                        livingEntity.damage(15);
                        Bukkit.getConsoleSender().sendMessage("tomahawk place hit entity");
                        this.cancel();
                        axe.remove();
                    }
                }
                // speed of the projectile
                Location newAxeLocation = location.clone().add(direction.clone().multiply(ticks * (speed *2)));
                if (newAxeLocation.getBlock().getType().isSolid()) {
                    Bukkit.getConsoleSender().sendMessage("solid block that s crazy");
                    this.cancel();
                    axe.remove();
                    return;
                }
                axe.teleport(newAxeLocation);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 2L);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "tomahawkThrow";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.IRON_AXE), ChatColor.BOLD + "" + ChatColor.RED + "Tomahawk Throw", ChatColor.GRAY + "You throw a tomahawk with all your might,", ChatColor.GRAY + "striking enemies in its path.");
    }
}
