package net.minervamc.minerva.skills.greek.dionysus;

import java.util.ArrayList;
import java.util.List;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MadGodsDrink extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double distance = 5;
        int speed = 3; //How many points on the bezier curve we go through
        int duration = 60;
        int poisonDuration = 100;
        int slownessDuration = 60;
        int poisonAmplifier = 0;
        int slownessAmplifier = 0;
        long cooldown = 7000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "madGodsDrink")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "madGodsDrink", cooldown);
        cooldownAlarm(player, cooldown, "Mad God's Drink");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);

        Vector playerLocation = new Vector(0, 0, 0);
        Vector playerEyeLocation = new Vector(0, 2, 0);
        Vector playerDirection = player.getEyeLocation().getDirection();

        Vector A = playerEyeLocation;
        Vector B = playerEyeLocation.clone().add(playerDirection.clone().multiply(distance/5).add(new Vector(0, 1, 0)));
        Vector C = playerLocation.clone().add(playerDirection.clone().multiply(distance).setY(0));

        int p = 0;
        while (!player.getLocation().clone().add(C).getBlock().isSolid()) {
            if (p > 20) return;
            C.subtract(new Vector(0, 1, 0));
            p++;
        }

        List<Vector> bezierPoints = ParticleUtils.getQuadraticBezierPoints(A, B, C, 10 * distance);

        new BukkitRunnable() {
            final Location location = player.getLocation();
            int index = 0;
            Location savedLocation = player.getEyeLocation();
            @Override
            public void run() {
                for (int i = 0; i < speed; i++) {
                    if (index >= bezierPoints.size() || savedLocation.getBlock().isSolid()) {
                        this.cancel();
                        potionExplode(player, savedLocation, 3, duration, slownessDuration, slownessAmplifier, poisonDuration, poisonAmplifier);
                        return;
                    }

                    Location particleLoc = location.clone().add(bezierPoints.get(i + index));
                    particleLoc.getWorld().spawnParticle(Particle.SPELL_WITCH, particleLoc, 0, 0, 0, 0, 0);
                    savedLocation = particleLoc;

                    index++;
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    public void potionExplode(Player player, Location explodeLocation, double radius, int effectTicks, int slowDur, int slowAmp, int poisonDur, int poisonAmp) {
        explodeLocation.getWorld().playSound(explodeLocation, Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= (effectTicks/10)) {
                    this.cancel();
                    explodeLocation.getWorld().playSound(explodeLocation, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                }
                if (ticks != 0) explodeLocation.getWorld().playSound(explodeLocation, Sound.BLOCK_LAVA_POP, 1f, 1f);
                for (Vector point : ParticleUtils.getFilledCirclePoints(radius, 100)) {
                    Location particleLocation = explodeLocation.clone().add(point);
                    particleLocation.getWorld().spawnParticle(Particle.SPELL_WITCH, particleLocation, 0, 0, 0, 0, 0);
                }
                for (Entity entity : explodeLocation.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDur, slowAmp));
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDur, poisonAmp));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 10L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "madGodsDrink";
    }

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(Color.PURPLE);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "[Mad God's Drink]"));

        List<String> lores = new ArrayList<>();
        lores.add(ChatColor.translateAlternateColorCodes('&', ChatColor.GRAY + "Throw a bottle of cursed wine, creating a cloud of noxious gas"));
        lores.add(ChatColor.translateAlternateColorCodes('&', ChatColor.GRAY + "that slows, damages, and poisons any entities that enter, including yourself."));

        meta.setLore(lores);
        item.setItemMeta(meta);
        return item;
    }
}
