package net.minervamc.minerva.skills.greek.ares;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pillager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Bloodlust extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int pillagersCount = 3;
        int radius = 3;
        long pillagerDespawnTicks = 200/5; // The runnable is every 5 seconds so the first number is the ticks you want :)
        long cooldown = pillagerDespawnTicks * 5 * 50 + 6000;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);

        List<Pillager> pillagers = new ArrayList<>();

        for (int i = 0; i < pillagersCount; i++) {
            Location pillagerLocation = player.getLocation().clone().add(FastUtils.randomDoubleInRange(-radius, radius), 0, FastUtils.randomDoubleInRange(-radius, radius));
            Vector pillagerDirection = new Vector(FastUtils.randomDoubleInRange(-1, 1), 0, FastUtils.randomDoubleInRange(-1, 1));
            AtomicBoolean isSolid = new AtomicBoolean(false);
            player.getWorld().getChunkAtAsync(pillagerLocation, chunk -> {
                if (chunk.isLoaded()) {
                    if (pillagerLocation.getBlock().isSolid()) {
                        isSolid.set(true);
                    }
                }
            });
            if (isSolid.get()) {
                i -= 1;
                continue;
            }

            Pillager pillager = (Pillager) player.getWorld().spawnEntity(pillagerLocation.setDirection(pillagerDirection), EntityType.PILLAGER);
            pillager.addScoreboardTag("artemisPillager");
            pillager.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (pillagerDespawnTicks*5), 4));
            pillagers.add(pillager);
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= pillagerDespawnTicks) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1f, 1f);
                    for (Pillager pillager : pillagers) {
                        pillager.remove();
                        Location particleLoc = pillager.getLocation();
                        pillager.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 10);
                    }
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 5L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "bloodlust";
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Material.VINE);
    }
}
