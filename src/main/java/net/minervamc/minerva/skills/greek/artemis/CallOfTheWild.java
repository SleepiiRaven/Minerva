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
        double angerRange = 10;
        int radius = 5;
        long wolfDespawnTicks = 200/5; // The runnable is every 5 seconds so the first number is the ticks you want :)
        long cooldown = wolfDespawnTicks * 50 + 6000;

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
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (wolfDespawnTicks*5), 4));
            wolves.add(wolf);
        }

        LivingEntity target = null;

        for (Entity entity : player.getNearbyEntities(angerRange, angerRange, angerRange)) {
            if (entity instanceof LivingEntity livingEntity && livingEntity != player && !entity.isInvulnerable() && !entity.isDead() && !livingEntity.getScoreboardTags().contains("artemisWolf") && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                if (target == null) {
                    target = livingEntity;
                } else if (player.getLocation().distanceSquared(livingEntity.getLocation()) < player.getLocation().distanceSquared(target.getLocation())) {
                    target = livingEntity;
                }
            }
        }

        for (Wolf wolf : wolves) {
            wolf.setAngry(true);
            wolf.setTarget(target);
            wolf.setOwner(player);
        }

        new BukkitRunnable() {
            int ticks = 0;
            LivingEntity target = null;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= wolfDespawnTicks) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1f, 1f);
                    for (Wolf wolf : wolves) {
                        wolf.remove();
                        Location particleLoc = wolf.getLocation();
                        wolf.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, particleLoc, 10);
                    }
                    this.cancel();
                }

                for (Entity entity : player.getNearbyEntities(angerRange, angerRange, angerRange)) {
                    if (entity instanceof LivingEntity livingEntity && livingEntity != player && !entity.isInvulnerable() && !entity.isDead() && !livingEntity.getScoreboardTags().contains("artemisWolf")) {
                        if (target == null) {
                            target = livingEntity;
                        } else if (player.getLocation().distanceSquared(livingEntity.getLocation()) < player.getLocation().distanceSquared(target.getLocation())) {
                            target = livingEntity;
                        }
                    }
                }

                for (Wolf wolf : wolves) {
                    if (wolf.getTarget() == null && target != null) {
                        wolf.setTarget(target);
                    }
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
