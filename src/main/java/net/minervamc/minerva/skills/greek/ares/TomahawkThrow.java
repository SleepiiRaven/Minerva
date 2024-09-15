package net.minervamc.minerva.skills.greek.ares;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.SkillUtils;
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
        double speed = 0.2; // Modify this to your liking :)
        final int ROTATE = 90;
        double damage = 15;
        int rotationSpeed = 40;
        int duration = 60;
        double kb = 0.3;
        long cooldown = 6000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tomahawk")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tomahawk", cooldown);
        cooldownAlarm(player, cooldown, "Tomahawk Throw");

        // Effect before loop
        ArmorStand axe = (ArmorStand) player.getWorld().spawnEntity(player.getEyeLocation().add(new Vector(0, -50, 0)), EntityType.ARMOR_STAND);
        axe.setInvulnerable(true);
        axe.setInvisible(true);
        axe.setSmall(true);
        axe.setGravity(false);
        axe.setMarker(true);

        axe.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));

        // Set its mainhand to true here as well and rotate the arm
        axe.teleport(player.getEyeLocation().subtract(new Vector(0, 0.5, 0)));
        new BukkitRunnable() {
            // These variables get set at the start of the runnable and don't get changed
            final Location location = player.getEyeLocation().subtract(new Vector(0, 0.5, 0));
            final Vector direction = player.getEyeLocation().getDirection();

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
                    if (!(entityAxe instanceof LivingEntity livingMonster) || entityAxe == axe || (entityAxe == player) || (entityAxe instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) continue;
                    SkillUtils.damage(livingMonster, damage, player);
                    Vector viewNormalized = (direction.clone().normalize()).multiply(kb);
                    livingMonster.setVelocity(viewNormalized);
                    this.cancel();
                    axe.remove();
                }
                // speed of the projectile
                Location newAxeLocation = location.clone().add(direction.clone().multiply(ticks * (speed *2)));
                if (newAxeLocation.getBlock().getType().isSolid()) {
                    this.cancel();
                    axe.getWorld().playSound(newAxeLocation, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    axe.remove();
                    return;
                }

                axe.teleport(newAxeLocation);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DROWNED_SHOOT, 1f, 0.9f);
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
