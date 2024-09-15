package net.minervamc.minerva.skills.greek.poseidon;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.zeus.LightningToss;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class OceansSurge extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        double damage;
        double push;

        switch (level) {
            default -> {
                cooldown = 8000;
                damage = 20;
                push = 0.5;
            }
            case 2 -> {
                cooldown = 7000;
                damage = 0.5;
                push = 1;
            }
            case 3 -> {
                cooldown = 6500;
                damage = 1;
                push = 1.5;
            }
            case 4 -> {
                cooldown = 6000;
                damage = 3;
                push = 2;
            }
            case 5 -> {
                cooldown = 5000;
                damage = 5;
                push = 3;
            }
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "oceansSurge")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "oceansSurge", cooldown);
        cooldownAlarm(player, cooldown, "Ocean's Surge");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.3f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1f);

        new BukkitRunnable() {
            double t = Math.PI/4;
            Location loc = player.getLocation();
            List<LivingEntity> hitLivingEntities = new ArrayList<>();
            public void run(){
                t += 0.1*Math.PI;
                for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI/32){
                    double x = t * Math.cos(theta);
                    double y = 2 * Math.exp(-0.1*t) * Math.sin(t) + 1.5;
                    double z = t * Math.sin(theta);
                    loc.add(x,y,z);
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 1, 0, 0, 0, 0);
                    for (Entity entity : loc.getNearbyEntities(0.5, 2, 0.5)) {
                        if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Horse) && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                            SkillUtils.damage(livingEntity, damage, player);
                            livingEntity.setVelocity(ParticleUtils.getDirection(player.getLocation(), livingEntity.getLocation()).multiply(push));
                        }
                    }
                    loc.subtract(x,y,z);

                    theta = theta + Math.PI/64;

                    x = t * Math.cos(theta);
                    y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
                    z = t * Math.sin(theta);
                    loc.add(x,y,z);
                    player.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0,157,196), 1));
                    loc.subtract(x,y,z);
                }
                if (t > 20){
                    this.cancel();
                }

                // REPEATED TO BE QUICKER :)

                t += 0.1*Math.PI;
                for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI/32){
                    double x = t * Math.cos(theta);
                    double y = 2 * Math.exp(-0.1*t) * Math.sin(t) + 1.5;
                    double z = t * Math.sin(theta);
                    loc.add(x,y,z);
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 1, 0, 0, 0, 0);
                    for (Entity entity : loc.getNearbyEntities(0.5, 2, 0.5)) {
                        if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Horse) && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                            SkillUtils.damage(livingEntity, damage, player);
                            livingEntity.setVelocity(ParticleUtils.getDirection(player.getLocation(), livingEntity.getLocation()).multiply(push));
                        }
                    }
                    loc.subtract(x,y,z);

                    theta = theta + Math.PI/64;

                    x = t * Math.cos(theta);
                    y = 2 * Math.exp(-0.1 * t) * Math.sin(t) + 1.5;
                    z = t * Math.sin(theta);
                    loc.add(x,y,z);
                    player.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0,157,196), 1));
                    loc.subtract(x,y,z);
                }
                if (t > 20){
                    this.cancel();
                }
            }

        }.runTaskTimer(Minerva.getInstance(), 0, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> "?";
            case 2 -> "??";
            case 3 -> "???";
            case 4 -> "????";
            case 5 -> "?????";
            default -> "-";
        };
    }

    @Override
    public String toString() {
        return "oceansSurge";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BLUE_DYE), ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "[Ocean's Surge]", ChatColor.GRAY + "Push all enemies away from you in a gigantic radius dealing almost no damage but major knockback.");
    }
}
