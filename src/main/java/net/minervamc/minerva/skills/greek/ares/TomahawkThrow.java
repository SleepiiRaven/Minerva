package net.minervamc.minerva.skills.greek.ares;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class TomahawkThrow extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double speed = 1; //blocks per rotation
        double damage = 15;
        int rotationSpeed = 2; // ticks per rotation
        double kb = 0.3;
        long cooldown = 6000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tomahawk")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tomahawk", cooldown);
        cooldownAlarm(player, cooldown, "Tomahawk Throw");

        ItemDisplay display = player.getWorld().spawn(player.getEyeLocation().subtract(0, 0.5, 0), ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(Material.IRON_AXE));
        });

        final Vector direction = player.getEyeLocation().getDirection();
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
        new BukkitRunnable() {
            int ticks = 0;
            float angle = 0; // Start angle for rotation

            @Override
            public void run() {
                if (display.getLocation().getBlock().isSolid() || !display.isValid()) {
                    display.getWorld().playSound(display.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                    display.remove();
                    this.cancel();
                }

                if (ticks % rotationSpeed == 0) {
                    Matrix4f matrix = new Matrix4f().identity();

                    angle += 45;
                    display.setTransformationMatrix(matrix.rotateX(((float) Math.toRadians(angle)) + 0.1F));
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(rotationSpeed);
                }
                display.teleport(display.getLocation().add(direction.clone().multiply(speed)));
                for (Entity entity : display.getWorld().getNearbyEntities(display.getLocation(), 1, 1, 1)) {
                    if (!(entity instanceof LivingEntity livingMonster) || (entity == display) || (entity.getScoreboardTags().contains("aresSummoned")) || (entity == player) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                        continue;
                    SkillUtils.damage(livingMonster, damage, player);
                    livingMonster.setVelocity(livingMonster.getVelocity().add(direction.clone().multiply(kb)));
                    display.getWorld().playSound(display.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                    display.remove();
                    this.cancel();
                }
                display.getWorld().spawnParticle(Particle.CLOUD, display.getLocation(), 0, 0, 0, 0, 0);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 2L, 1L);

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