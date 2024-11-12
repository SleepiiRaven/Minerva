package net.minervamc.minerva.skills.greek.dionysus;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class GrapeShot extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double damage = 2;
        int weakDur = 200;
        int weakAmp = 0;
        double maxDistance = 10;
        int maxTicks = 40;
        double berrySpeed = 1;
        List<Integer> ticksOnWhichBerriesThrow = new ArrayList<>();
        ticksOnWhichBerriesThrow.add(0);
        ticksOnWhichBerriesThrow.add(20);
        ticksOnWhichBerriesThrow.add(40);
        long cooldown = 5000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "grapeShot")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "grapeShot", cooldown);
        cooldownAlarm(player, cooldown, "Grape Shot");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= maxTicks) {
                    this.cancel();
                }

                for (int tickBerryThrow : ticksOnWhichBerriesThrow) {
                    if (ticks == tickBerryThrow) {
                        throwBerry(player, berrySpeed, maxDistance, damage, weakDur, weakAmp);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void throwBerry(Player player, double berrySpeed, double maxDistance, double damage, int weakDur, int weakAmp) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.2f);

        new BukkitRunnable() {
            final Location throwLoc = player.getEyeLocation();
            int ticks = 0;

            @Override
            public void run() {
                double currentDistance = berrySpeed * ticks;
                if (player.isDead() || !player.isOnline() || currentDistance > maxDistance) {
                    this.cancel();
                    return;
                }

                Location particleLoc = throwLoc.clone().add(throwLoc.getDirection().clone().multiply(currentDistance));

                particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0, new Particle.DustOptions(Color.PURPLE, 1f));
                for (Entity entity : particleLoc.getNearbyEntities(1, 1, 1)) {
                    if (entity instanceof LivingEntity livingEntity && livingEntity != player && !(livingEntity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                        damage(livingEntity, damage, player);
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weakDur, weakAmp));
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "grapeShot";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.PURPLE_DYE), ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "[Grape Shot]", ChatColor.GRAY + "Throw fermented grapes at your enemies, dealing a small amount", ChatColor.GRAY + "of damage and causing them to become weakened.");
    }
}
