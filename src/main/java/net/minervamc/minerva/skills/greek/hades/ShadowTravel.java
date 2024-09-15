package net.minervamc.minerva.skills.greek.hades;

import java.util.Collection;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ShadowTravel extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        double distance;
        int range = 3;
        double damage;
        double kb;

        kb = FastUtils.randomDoubleInRange(0.3, 1);
        switch (level) {
            default -> {
                cooldown = 10000;
                distance = 7;
                damage = 50;
            }
            case 2 -> {
                cooldown = 9000;
                distance = 8;
                damage = 0.1;
            }
            case 3 -> {
                cooldown = 8000;
                distance = 10;
                damage = 0.1;
            }
            case 4 -> {
                cooldown = 8000;
                distance = 12;
                damage = 0.1;
            }
            case 5 -> {
                cooldown = 8000;
                distance = 15;
                damage = 0.1;
            }
        }



        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "shadowTravel")) {
            onCooldown(player);
            return;
        } else if (!cooldownManager.isCooldownDone(player.getUniqueId(), "channelingOfTartarusCasting")) {
            skillLocked(player, "you are currently casting Channeling of Tartarus");
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "shadowTravel", cooldown);
        cooldownAlarm(player, cooldown, "Shadow Travel");

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 100));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
        Vector dir = player.getEyeLocation().getDirection();
        Location previousLoc = player.getEyeLocation();
        Location originalLoc = player.getEyeLocation();
        for (double i = 0; i <= distance; i += 0.5) {
            Location tempLoc = originalLoc.clone().add(dir.clone().multiply(i));
            if (tempLoc.getBlock().getBlockData().getMaterial().isSolid()) {
                teleport(player, previousLoc, distance, dir, range, kb, damage);
                return;
            }
            if (i == distance) {
                teleport(player, tempLoc, distance, dir, range, kb, damage);
            }
            previousLoc = tempLoc;
        }
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

    private void teleport(Player player, Location location, double distance, Vector dir, double range, double kb, double damage) {
        player.teleport(location);
        player.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
        for (double t = 0; t < distance; t += 0.5) {
            Vector teleportDistance = dir.clone().multiply(t);
            location.clone().add(teleportDistance);
            player.getWorld().spawnParticle(Particle.PORTAL, location, 10);
            player.getWorld().spawnParticle(Particle.DUST, location, 10, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(32, 32, 32), 2));
            player.getWorld().spawnParticle(Particle.DUST, location, 10, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(40, 40, 40), 2));
            Collection<Entity> closebyMonsters = player.getWorld().getNearbyEntities(location, range, range, range);
            for (Entity closebyMonster : closebyMonsters) {
                // make sure it's a living entity, not an armor stand or something, continue skips the current loop
                if (!(closebyMonster instanceof LivingEntity) || (closebyMonster == player)) continue;
                LivingEntity livingMonster = (LivingEntity) closebyMonster;
                if (!(livingMonster instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                    // Get the entity's collision box
                    BoundingBox monsterBoundingBox = livingMonster.getBoundingBox();
                    BoundingBox collisionBox = BoundingBox.of(location, range, range, range);
                    if (!(monsterBoundingBox.overlaps(collisionBox))) continue;
                    SkillUtils.damage(livingMonster, damage, player);
                    Vector viewNormalized = ParticleUtils.getDirection(location, livingMonster.getLocation()).normalize().multiply(kb);
                    livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    livingMonster.setVelocity(viewNormalized);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "shadowTravel";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ENDER_PEARL), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "[Shadow Travel]", ChatColor.GRAY + "Cloak yourself in shadows, teleporting far ahead, granting those you hit with blindness,", ChatColor.GRAY + "and granting yourself with invisibility.");
    }
}
