package net.minervamc.minerva.skills.greek.ares;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Cleave extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;
        double damage = 2;
        double damageSlam = 5;
        double kb = 0.2;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "cleave")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "cleave", cooldown);
        cooldownAlarm(player, cooldown, "Cleave");

        Color[] gradient = {
                Color.fromRGB(0, 0, 0),
                Color.fromRGB(16, 2, 1),
                Color.fromRGB(26, 5, 3),
                Color.fromRGB(33, 7, 4),
                Color.fromRGB(40, 9, 6),
                Color.fromRGB(47, 10, 8),
                Color.fromRGB(54, 8, 9),
                Color.fromRGB(61, 6, 11),
                Color.fromRGB(69, 4, 13),
                Color.fromRGB(77, 0, 14),
                Color.fromRGB(77, 0, 14)
        };

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.3f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.7f);

        new BukkitRunnable() {
            int ticks = 0;
            int ticks2 = 0;

            @Override
            public void run() {
                Vector right = ParticleUtils.rotateYAxis(player.getLocation().getDirection().clone(), 90);
                Vector left = ParticleUtils.rotateYAxis(player.getLocation().getDirection().clone(), -90);
                Vector iPrimarySlashA = right.clone().multiply(2);
                Vector iPrimarySlashB = player.getLocation().getDirection().clone().multiply(4).add(new Vector(0, 1, 0));
                Vector iPrimarySlashC = left.clone().multiply(2).add(new Vector(0, 1, 0));
                Vector iSecondarySlashA = iPrimarySlashC.clone();
                Vector iSecondarySlashB = player.getLocation().getDirection().clone().multiply(-4).add(new Vector(0, 1, 0));
                Vector iSecondarySlashC = right.multiply(2).clone().add(new Vector(0, 2, 0));
                Vector iSpinA = iSecondarySlashC.clone();
                Vector iSpinB = player.getLocation().getDirection().clone().multiply(4).add(new Vector(0, 2, 0));
                Vector iSpinC = left.multiply(2).clone().add(new Vector(0, 2.5, 0));
                Vector iBeforeSlamA = iSpinC.clone();
                Vector iBeforeSlamB = player.getLocation().getDirection().clone().multiply(-4).add(new Vector(0, 2, 0));
                Vector iBeforeSlamC = new Vector(0, 3.5, 0);

                List<Vector> iVectorList = ParticleUtils.getQuadraticBezierPoints(iPrimarySlashA, iPrimarySlashB, iPrimarySlashC, 10);
                iVectorList.addAll(ParticleUtils.getQuadraticBezierPoints(iSecondarySlashA, iSecondarySlashB, iSecondarySlashC, 10));
                iVectorList.addAll(ParticleUtils.getQuadraticBezierPoints(iSpinA, iSpinB, iSpinC, 10));
                iVectorList.addAll(ParticleUtils.getQuadraticBezierPoints(iBeforeSlamA, iBeforeSlamB, iBeforeSlamC, 10));
                Object[] vectors = iVectorList.toArray();
                Vector iSlamA = iBeforeSlamC.clone();
                Vector iSlamB = player.getLocation().getDirection().clone().multiply(2).add(new Vector(0, 4, 0));
                Vector iSlamC = player.getLocation().getDirection().clone().multiply(4);
                Object[] vecs = ParticleUtils.getQuadraticBezierPoints(iSlamA, iSlamB, iSlamC, 20).toArray();
                List<Object> vecsOffset = new ArrayList<>();
                vecsOffset.addAll(iVectorList);
                vecsOffset.addAll(Arrays.stream(vecs).toList());
                List<Vector> vecsOffsetNormalized = new ArrayList<>();

                for (Object vec : vecsOffset) {
                    vecsOffsetNormalized.add(((Vector) vec).clone().normalize());
                }

                if (ticks < vectors.length) {
                    if (ticks % 5 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DROWNED_SHOOT, 0.2f, 0.6f);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_SCRAPE, 0.2f, 0.6f);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.2f, 0.6f);
                    }

                    player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add((Vector) vectors[ticks]), 1, 0.1, 0.1, 0.1, 0.05);
                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation().add((Vector) vectors[ticks + 1]), 1, 1, 1)) {
                        if (!(entity instanceof LivingEntity livingMonster) || PlayerStats.getStats(player.getUniqueId()).getSummoned().get(player).contains(livingMonster) || (entity == player) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;
                        damage(livingMonster, damage, player, false, true);
                        Vector viewNormalized = (new Vector(0, 1, 0)).multiply(kb);
                        knockback(livingMonster, viewNormalized);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.3f, 0.6f);
                        player.getWorld().spawnParticle(Particle.SMOKE, livingMonster.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                    }
                } else if (ticks2 < 20) {
                    if (ticks2 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FOX_DEATH, 1.0f, 0.6f);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 0.6f);
                    }
                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation().add((Vector) vecs[ticks2]), 1, 1, 1)) {
                        if (!(entity instanceof LivingEntity livingMonster) || PlayerStats.isSummoned(player, entity) || (entity == player) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;
                        damage(livingMonster, damage, player, false, true);
                        Vector viewNormalized = (new Vector(0, 1, 0)).multiply(kb);
                        knockback(livingMonster, viewNormalized);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.3f, 0.6f);
                        player.getWorld().spawnParticle(Particle.SMOKE, livingMonster.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                    }
                    ticks2++;
                } else {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.6f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 0.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.7f, 0.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRAVEL_FALL, 0.7f, 0.5f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(iSlamC), 1, 0, 0, 0, 0.05);
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(iSlamC), 10, 0.3, 0.3, 0.3, 0.01);
                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation().add(iSlamC), 4, 4, 4)) {
                        if (!(entity instanceof LivingEntity livingMonster) || PlayerStats.isSummoned(player, entity) || (entity == player) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;
                        damage(livingMonster, damageSlam, player, true, true);
                        Vector viewNormalized = (new Vector(0, 1, 0)).multiply(kb);
                        knockback(livingMonster, viewNormalized);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.3f, 0.6f);
                        player.getWorld().spawnParticle(Particle.SMOKE, livingMonster.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                    }
                    cancel();
                }
                if (ticks < vecsOffsetNormalized.size()) {
                    int i = 0;
                    for (Vector vec : ParticleUtils.getLinePoints(vecsOffsetNormalized.get(ticks), 2, 0.2)) {
                        if (i >= gradient.length) {
                            i--;
                        }
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(vec.clone().add((Vector) vecsOffset.get(ticks))), 0, 0, 0, 0, 0, new Particle.DustOptions(gradient[i], 1f));
                        i++;
                    }
                }
                if (ticks + 1 < vecsOffsetNormalized.size()) {
                    int i = 0;
                    for (Vector vec : ParticleUtils.getLinePoints(vecsOffsetNormalized.get(ticks + 1), 2, 0.2)) {
                        if (i >= gradient.length) {
                            i--;
                        }
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(vec.clone().add((Vector) vecsOffset.get(ticks + 1))), 0, 0, 0, 0, 0, new Particle.DustOptions(gradient[i], 1f));
                        i++;
                    }
                }
                ticks += 2;
            }

        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "cleave";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.NETHERITE_SWORD), ChatColor.WHITE + "" + ChatColor.BOLD + "[Cleave]", ChatColor.GRAY + "Swing your weapon", ChatColor.GRAY + "around yourself in", ChatColor.GRAY + "quick succession,", ChatColor.GRAY + "dealing massive damage", ChatColor.GRAY + "and a devastating", ChatColor.GRAY + "stun to unfortunate", ChatColor.GRAY + "souls hit by the", ChatColor.GRAY + "final slam.");
    }
}
