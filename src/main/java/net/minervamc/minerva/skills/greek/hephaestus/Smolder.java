package net.minervamc.minerva.skills.greek.hephaestus;

import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class Smolder extends Skill {
    BukkitTask task;

    Color[] pallete = {
            Color.fromRGB(49,46,40),
            Color.fromRGB(121,112,98),
            Color.fromRGB(193,174,144),
            Color.fromRGB(222,190,144),
            Color.fromRGB(235,180,99),
            Color.fromRGB(249,243,124),
            Color.fromRGB(255,155,53),
            Color.fromRGB(189,55,10)
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {

    }

    public int stackSmolder(Player player, int level, long milisUntilOver) {
        if (task != null) task.cancel();

        int maxLevel = 5;

        if (level > maxLevel) {
            level = maxLevel;
        }

        if (level <= 0) {
            player.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
            return level;
        }

        final int levelFinal = level;

        long ticksUntilOver = milisUntilOver / 50;

        player.playSound(player, Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 1f);

        task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= ticksUntilOver/5 || player.isDead() || !player.isOnline()) {
                    player.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
                    this.cancel();
                    return;
                }

                ticks++;

                World world = player.getWorld();
                Location loc = player.getLocation();
                Random random = new Random();

                switch (levelFinal) {
                    case 1 -> {
                        world.spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0, new Particle.DustOptions(pallete[0], 2f));
                        break;
                    }
                    case 2 -> {
                        int rInt = random.nextInt(0, 2);
                        world.spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0, new Particle.DustOptions(pallete[rInt], 2f));
                        break;
                    }
                    case 3 -> {
                        List<Vector> circle = ParticleUtils.getCirclePoints(1);
                        for (Vector vec : circle) {
                            int rInt = random.nextInt(0, 3);
                            world.spawnParticle(Particle.DUST, loc.clone().add(vec), 0, 0, 0, 0, 0, new Particle.DustOptions(pallete[rInt], 2f));
                        }
                        break;
                    }
                    case 4 -> {
                        List<Vector> circle = ParticleUtils.getCirclePoints(2);
                        for (Vector vec : circle) {
                            int rInt = random.nextInt(0, 5);
                            world.spawnParticle(Particle.DUST, loc.clone().add(vec), 0, 0, 0, 0, 0, new Particle.DustOptions(pallete[rInt], 2f));
                        }
                        break;
                    }
                    case 5 -> {
                        List<Vector> stars = ParticleUtils.getStarPoints(5, 1, 2, 10);
                        for (Vector vec : stars) {
                            int rInt = random.nextInt(0, 7);
                            world.spawnParticle(Particle.DUST, loc.clone().add(vec), 0, 0, 0, 0, 0, new Particle.DustOptions(pallete[rInt], 2f));
                        }
                        break;
                    }
                    default -> {
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 10L);

        return level;
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "smolder";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.BLAZE_POWDER)
                .setName(TextContext.formatLegacy("&lSmolder", false).color(NamedTextColor.GOLD))
                .setLore(List.of(
                        TextContext.formatLegacy("&7When you get hit, gain a", false),
                        TextContext.formatLegacy("&7stack of Smolder. With a", false),
                        TextContext.formatLegacy("&7maximum of 5 stacks, each", false),
                        TextContext.formatLegacy("&7stack increases your next", false),
                        TextContext.formatLegacy("&7hit's damage by an exponential", false),
                        TextContext.formatLegacy("&7amount. Smolder stacks are", false),
                        TextContext.formatLegacy("&7used in other skills as well.", false)
                )).build();
    }
}
