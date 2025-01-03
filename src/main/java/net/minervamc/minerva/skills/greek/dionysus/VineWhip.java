package net.minervamc.minerva.skills.greek.dionysus;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VineWhip extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double damage = 4;
        double distance = 12;
        long cooldown = 7000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "vineWhip")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "vineWhip", cooldown);
        cooldownAlarm(player, cooldown, "Vine Whip");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 1f);
        List<Vector> linePoints = ParticleUtils.getLinePoints(player.getLocation().getDirection(), distance, 0.5);
        new BukkitRunnable() {
            final List<LivingEntity> hitEnemies = new ArrayList<>();
            final Location location = player.getEyeLocation();
            boolean hit = false;
            int ticks = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 3; i++) {
                    if (ticks >= linePoints.size()) {
                        this.cancel();
                        break;
                    }
                    Location particleLoc = location.clone().add(linePoints.get(ticks));
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.GREEN, 2f));
                    for (Entity entity : particleLoc.getNearbyEntities(0.3, 0.3, 0.3)) {
                        if (entity instanceof LivingEntity livingEntity && !hitEnemies.contains(livingEntity) && livingEntity != player && !(livingEntity instanceof Player livingPlayer && livingPlayer.getGameMode() != GameMode.CREATIVE && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                            Vector direction = location.clone().toVector().subtract(livingEntity.getLocation().toVector()).normalize();
                            if (!hit)
                                livingEntity.getWorld().playSound(livingEntity.getLocation(), Sound.ENTITY_BLAZE_HURT, 1f, 1f);
                            double vel = Math.sqrt(player.getLocation().distance(livingEntity.getLocation()));
                            knockback(livingEntity, direction.clone().multiply(vel));
                            damage(livingEntity, damage, player);
                            hit = true;
                            hitEnemies.add(livingEntity);
                        }
                    }
                    ticks++;
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
        return "vineWhip";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.VINE), ChatColor.GREEN + "" + ChatColor.BOLD + "[Vine Whip]", ChatColor.GRAY + "Throw a grape vine towards an enemy,", ChatColor.GRAY + "damaging them and pulling them towards you.");
    }
}
