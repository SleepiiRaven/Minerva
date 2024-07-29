package net.minervamc.minerva.skills.greek.apollo;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.AttributeUtils;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ApollosHymn extends Skill {

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long ticksBetweenHeal = 20L;
        int radius = 3;
        double enemyHeal = 0.75;
        double enemyHealLast = 3;
        double selfHeal = 1;
        double selfHealLast = 4;
        double heal = 1.5;
        double healLast = 6;
        int maxHeals = 3; // PLUS ONE FOR THE INITIAL HEAL
        long cooldown = 10000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "apollosHymn")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "apollosHymn", cooldown);
        cooldownAlarm(player, cooldown, "Apollo's Hymn");

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_STEP, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_DEATH, 1f, 1f);
        Location playerLocation = player.getLocation();
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks > maxHeals) {
                    this.cancel();
                    return;
                }
                for (Vector point : ParticleUtils.getFilledCirclePoints(radius, radius * 20)) {
                    Location loc = playerLocation.clone().add(point);
                    World world = loc.getWorld();
                    if (ticks == maxHeals) {
                        world.spawnParticle(Particle.HEART, loc, 1, 0, 0, 0, 0);
                    }
                    world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(250, 250, 210), 2));
                    world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(184, 134, 11), 2));
                    world.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                }

                playerLocation.getWorld().playSound(playerLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);

                for (Entity entity : playerLocation.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity livingEntity) {
                        double localSelfHeal = selfHeal;
                        double localHeal = heal;
                        double localEnemyHeal = enemyHeal;
                        if (ticks == maxHeals) {
                            localSelfHeal = selfHealLast;
                            localHeal = healLast;
                            localEnemyHeal = enemyHealLast;
                        }

                        if (livingEntity == player) {
                            double maxHealth = AttributeUtils.getMaxHealth(player);
                            player.setHealth(Math.min(player.getHealth() + localSelfHeal, maxHealth));
                        } else if (livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)) {
                            double maxHealth = AttributeUtils.getMaxHealth(livingEntity);
                            livingEntity.setHealth(Math.min(livingEntity.getHealth() + localHeal, maxHealth));
                        } else {
                            double maxHealth = AttributeUtils.getMaxHealth(livingEntity);
                            livingEntity.setHealth(Math.min(livingEntity.getHealth() + localEnemyHeal, maxHealth));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, ticksBetweenHeal);
    }

    @Override
    public String getLevelDescription(int level) {
        return "?";
    }

    @Override
    public String toString() {
        return "apollosHymn";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ALLIUM), ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Apollo's Hymn", ChatColor.GRAY + "You sing one of Apollo's hymns, blessing the ground and", ChatColor.GRAY + "healing ANY entities that stand on the sacred earth.", ChatColor.GRAY + "During the last moments of the blessing on the ground, those who stand", ChatColor.GRAY + "there heal significantly more using the blessing's last breath.");
    }
}
