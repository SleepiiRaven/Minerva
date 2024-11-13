package net.minervamc.minerva.skills.greek.dionysus;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class FrenziedDance extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "frenziedDance")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "frenziedDance", cooldown);
        cooldownAlarm(player, cooldown, "Frenzied Dance");

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 9));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= 6) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                    this.cancel();
                }

                double x = FastUtils.randomDoubleInRange(-1, 1);
                double y = FastUtils.randomDoubleInRange(-1, 1);
                double z = FastUtils.randomDoubleInRange(-1, 1);
                player.getWorld().spawnParticle(Particle.NOTE, player.getLocation(), 10, x, y, z, 0.35);

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 5L);

        playSong(player);
    }

    private void playSong(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.41f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.78f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.19f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.78f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                switch (ticks) {
                    case 0 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.12f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.59f);
                    }
                    case 1 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.19f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.78f);
                        this.cancel();
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 16L, 16L);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                switch (ticks) {
                    case 0 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.5f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 2.0f);
                    }
                    case 1 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.59f);
                    }
                    case 2, 4 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.41f);
                    }
                    case 3 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.78f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.33f);
                    }
                    case 5 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.06f);
                    }
                    case 6 -> {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.19f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.78f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 0.89f);
                        this.cancel();
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 4L, 4L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "frenziedDance";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.TOTEM_OF_UNDYING), ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Frenzied Dance]", ChatColor.GRAY + "Call upon Dionysus, playing a song in his honor. In return", ChatColor.GRAY + "Dionysus blesses you, granting you a", ChatColor.GRAY + "temporary boost to your movement speed and resistance.");
    }
}
