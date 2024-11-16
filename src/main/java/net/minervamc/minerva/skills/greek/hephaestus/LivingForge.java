package net.minervamc.minerva.skills.greek.hephaestus;

import java.util.List;
import java.util.Random;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LivingForge extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        int particles = 20;
        int cycleLength = 5; // in ticks
        int particlesPerCycle = particles/cycleLength;
        int duration = 60; // in ticks
        long despawnTicks = 600L;
        long cooldown = despawnTicks * 50 + 20000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "livingForge")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "livingForge", cooldown);
        cooldownAlarm(player, cooldown, "Living Forge");

        Random random = new Random();

        Color[] gradient = {
                Color.fromRGB(79, 28, 13),
                Color.fromRGB(128, 97, 80),
                Color.fromRGB(190, 184, 175),
                Color.fromRGB(87, 80, 71),
                Color.fromRGB(66, 55, 48),
                Color.fromRGB(38, 30, 29)
        };

        Location spawnLoc = player.getEyeLocation();
        World world = player.getWorld();
        ArmorStand init = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        init.setInvisible(true);
        init.setInvulnerable(true);
        init.setSmall(true);
        init.getEquipment().setHelmet(new ItemStack(Material.IRON_BLOCK));
        init.setVelocity(player.getLocation().getDirection());
        List<Vector> curve = ParticleUtils.getCubicBezierPoints(new Vector(5, 2.5, 0), new Vector(0, 5, 0), new Vector(5, 0, 0), new Vector(0, 0, 0), 20);

        new BukkitRunnable() {
            int ticks = 0;
            int ticksCharging = 0;
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline() || player.isDead()) {
                    if (ticks >= duration) {
                        summonGolem(player, despawnTicks, init.getEyeLocation(), (init.getFireTicks() > 0 && getStacks(player, "smolder") > 0));
                    }
                    this.cancel();
                    init.remove();
                    return;
                }

                if (init.isOnGround()) {
                    for (int rot = 0; rot < 360; rot += 30) {
                        for (int i = 0; i < particlesPerCycle; i++) {
                            if (i + ticksCharging >= (curve.size() - 1)) ticksCharging = 0;
                            Vector vector = curve.get(i + ticksCharging);
                            Vector rotated = vector.clone().rotateAroundY(rot);
                            Color randColor = gradient[random.nextInt(0, 5)];
                            world.spawnParticle(Particle.DUST, init.getEyeLocation().add(rotated), 0, 0, 0, 0, 0, new Particle.DustOptions(randColor, 2f));
                            world.spawnParticle(Particle.ENCHANTED_HIT, init.getEyeLocation().add(rotated), 0, 0, 0, 0, 0);
                            world.spawnParticle(Particle.ELECTRIC_SPARK, init.getEyeLocation().add(rotated), 0, 0, 0, 0, 0);
                        }
                    }
                    ticksCharging++;
                    ticks++;
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 5L, 1L);
    }

    public static void summonGolem(Player player, long despawnTicks, Location loc, boolean overheat) {
        IronGolem golem = (IronGolem) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
        golem.addScoreboardTag(player.getUniqueId().toString());

        if (overheat) {
            stack(player, "smolder", -1, "Smolder", 5000);
            overheat(golem);
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > despawnTicks || !player.isOnline() || player.isDead()) {
                    golem.remove();
                    this.cancel();
                    return;
                }

                if (golem.isDead()) {
                    this.cancel();
                    return;
                }

                if (golem.getScoreboardTags().contains("overheated")) {
                    for (Vector vec : ParticleUtils.getCirclePoints(2, 20)) {
                        Location particleLoc = golem.getLocation().add(new Vector(0, 1, 0)).add(vec);
                        particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0, new Particle.DustOptions(
                                Color.fromRGB(255,155,53), 1f));
                    }
                }

                if (!(golem.getTarget() instanceof LivingEntity) || golem.getTarget() == player || golem.getTarget().isDead()) {
                    for (Entity e : golem.getLocation().getNearbyEntities(20, 20, 20)) {
                        if (e instanceof LivingEntity livingEntity && e != golem && e != player && !(e instanceof Player pTarget && Party.isPlayerInPlayerParty(player, pTarget)) && (livingEntity.getTargetEntity(30) != null || livingEntity instanceof Player && livingEntity.getWorld().getPVP())) {
                            golem.setTarget(livingEntity);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    public static void overheat(LivingEntity livingMonster) {
        livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 10000, 0));
        livingMonster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10000, 1));
        livingMonster.addScoreboardTag("overheated");
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "livingForge";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.create(Material.IRON_BLOCK);
    }
}
