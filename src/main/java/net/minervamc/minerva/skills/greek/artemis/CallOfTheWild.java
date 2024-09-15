package net.minervamc.minerva.skills.greek.artemis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CallOfTheWild extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int wolvesCount = 5;
        int radius = 5;
        long wolfDespawnTicks = 200/5; // The runnable is every 5 seconds so the first number is the ticks you want :)
        long cooldown = wolfDespawnTicks * 5 * 50 + 6000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "callOfTheWild")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "callOfTheWild", cooldown);
        cooldownAlarm(player, cooldown, "Call of the Wild");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1f, 1f);

        List<Wolf> wolves = new ArrayList<>();

        for (int i = 0; i < wolvesCount; i++) {
            Location wolfLocation = player.getLocation().clone().add(FastUtils.randomDoubleInRange(-radius, radius), 0, FastUtils.randomDoubleInRange(-radius, radius));
            Vector wolfDirection = new Vector(FastUtils.randomDoubleInRange(-1, 1), 0, FastUtils.randomDoubleInRange(-1, 1));
            AtomicBoolean isSolid = new AtomicBoolean(false);
            player.getWorld().getChunkAtAsync(wolfLocation, chunk -> {
                if (chunk.isLoaded()) {
                    if (wolfLocation.getBlock().isSolid()) {
                        isSolid.set(true);
                    }
                }
            });
            if (isSolid.get()) {
                i -= 1;
                continue;
            }

            Wolf wolf = (Wolf) player.getWorld().spawnEntity(wolfLocation.setDirection(wolfDirection), EntityType.WOLF);
            wolf.addScoreboardTag("artemisWolf");
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (wolfDespawnTicks*5), 4));
            wolves.add(wolf);
        }

        for (Wolf wolf : wolves) {
            wolf.setOwner(player);
            wolf.setCollarColor(DyeColor.WHITE);
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= wolfDespawnTicks) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1f, 1f);
                    for (Wolf wolf : wolves) {
                        wolf.remove();
                        Location particleLoc = wolf.getLocation();
                        wolf.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 10);
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
        return "callOfTheWild";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BONE), ChatColor.BOLD + "" + ChatColor.DARK_GREEN + "[Call of the Wild]", ChatColor.GRAY + "Blow on a whistle to call the wolves of the Hunt to fight by your side.");
    }
}
