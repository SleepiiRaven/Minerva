package net.minervamc.minerva.skills.greek.poseidon;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TidalWave extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int tickDuration;
        long cooldown;
        double damage;

        switch (level) {
            case 2 -> {
                tickDuration = 30;
                cooldown = 9000 + (tickDuration * 50);
                damage = 4;
            }
            case 3 -> {
                tickDuration = 40;
                cooldown = 8000 + (tickDuration * 50);
                damage = 8;
            }
            case 4 -> {
                tickDuration = 50;
                cooldown = 7000 + (tickDuration * 50);
                damage = 16;
            }
            case 5 -> {
                tickDuration = 60;
                cooldown = 8000 + (tickDuration * 50);
                damage = 32;
            }
            default -> {
                tickDuration = 20;
                cooldown = 10000 + (tickDuration * 50);
                damage = 2;
            }
        }

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "tidalWave")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "tidalWave", cooldown);
        cooldownAlarm(player, cooldown, "Tidal Wave");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);

        final Vector relativePlayerVector = new Vector(0, 1.5, 0);
        Horse mount = (Horse) player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
        mount.setInvisible(true);
        mount.setInvulnerable(true);
        mount.setSilent(true);
        //mount.setAI(false);
        mount.addPassenger(player);
        //mount.setOwner(player);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || player.isSneaking() || ticks >= (tickDuration / 5)) {
                    player.teleport(mount.getLocation());
                    mount.remove();
                    this.cancel();
                    Location location = player.getLocation();
                    Vector direction = player.getLocation().getDirection().clone().setY(0);
                    Vector lineDirection = direction.clone().rotateAroundY(90);
                    waveParticleEffect(direction, lineDirection, relativePlayerVector, location, player);
                    return;
                }

                Location location = mount.getLocation();
                Vector direction = player.getEyeLocation().getDirection().clone().setY(0);

                Vector lineDirection = direction.clone().rotateAroundY(90);

                waveParticleEffect(direction, lineDirection, relativePlayerVector, location, player);

                if (!mount.getLocation().clone().subtract(0, 1, 0).getBlock().isSolid() && !mount.getLocation().clone().subtract(0, 1, 0).getBlock().isLiquid()) {
                    mount.setVelocity(direction.clone().multiply(2).subtract(new Vector(0, 0.5, 0)));
                } else {
                    mount.setVelocity(direction.clone().multiply(2));
                }

                for (Entity entity : mount.getLocation().getNearbyEntities(2, 2, 2)) {
                    if (entity instanceof LivingEntity livingEntity && livingEntity != mount && livingEntity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                        damage(livingEntity, damage, player);
                        knockback(livingEntity, direction.clone());
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 5L, 5L);
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

    private void waveParticleEffect(Vector direction, Vector lineDirection, Vector relativePlayerVector, Location location, Player player) {
        for (double i = -1; i <= 1; i += 0.2) {
            Vector localDirection = direction.clone();
            Vector progressVector = lineDirection.clone().multiply(i);
            Vector A = relativePlayerVector.clone().subtract(localDirection.clone().add(new Vector(0, 1.5, 0)));
            Vector B = relativePlayerVector.clone().add(new Vector(0, 0.5, 0));
            Vector C = relativePlayerVector.clone().add(localDirection.clone().multiply(0.8).subtract(new Vector(0, 0.5, 0)));
            for (Vector particleVector : ParticleUtils.getQuadraticBezierPoints(A, B, C, 10)) {
                Location particleLocation = location.clone().add(particleVector).clone().add(progressVector);
                player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0, 157, 196), 1));
                player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, new Particle.DustOptions(Color.BLUE, 1));
            }
            A = C.clone();
            B = relativePlayerVector.clone().subtract(new Vector(0, 0.5, 0));
            C = A.clone().subtract(new Vector(0, 1, 0));
            for (Vector particleVector : ParticleUtils.getQuadraticBezierPoints(A, B, C, 10)) {
                Location particleLocation = location.clone().add(particleVector).clone().add(progressVector);
                player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0, 157, 196), 1));
                player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, new Particle.DustOptions(Color.BLUE, 1));
            }
        }
    }

    @Override
    public String toString() {
        return "tidalWave";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PRISMARINE_SHARD), ChatColor.AQUA + "" + ChatColor.BOLD + "[Tidal Wave]", ChatColor.GRAY + "Ride forward on a speedy wave of water that does a medium", ChatColor.GRAY + "amount of damage to enemies in its way.");
    }
}
