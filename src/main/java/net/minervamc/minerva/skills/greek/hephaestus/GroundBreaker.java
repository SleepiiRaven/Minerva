package net.minervamc.minerva.skills.greek.hephaestus;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class GroundBreaker extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        double damage = 10;
        int fireTicks = 40;
        boolean hasSmolder = (getStacks(player, "smolder") > 0);
        int ticksToFall = 20;
        int initialHeight = 20;
        Color[] gradient = (hasSmolder) ? new Color[] {
                Color.fromRGB(49,46,40),
                Color.fromRGB(121,112,98),
                Color.fromRGB(193,174,144),
                Color.fromRGB(222,190,144),
                Color.fromRGB(235,180,99),
                Color.fromRGB(249,243,124),
                Color.fromRGB(255,155,53),
                Color.fromRGB(189,55,10)
        } : new Color[] {
            Color.fromRGB(79, 28, 13),
            Color.fromRGB(128, 97, 80),
            Color.fromRGB(190, 184, 175),
            Color.fromRGB(87, 80, 71),
            Color.fromRGB(66, 55, 48)
        };
        int stunTicks = 10;
        double radius = 12;
        int ticksForShockwave = 40;
        long cooldown = 30000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "groundBreaker")) {
            onCooldown(player);
            return;
        }

        RayTraceResult result = player.rayTraceBlocks(75);
        Location loc;
        if (result == null) {
            loc = player.getLocation();
        } else if (result.getHitBlock() == null) {
            if (result.getHitEntity() == null) {
                loc = player.getLocation();
            }
            loc = result.getHitEntity().getLocation();
        } else {
            loc = result.getHitBlock().getLocation();
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "groundBreaker", cooldown);
        cooldownAlarm(player, cooldown, "Ground Breaker");


        loc.setDirection(new Vector(1, 0, 0)).add(-2.5, 1, 2.5);

        BlockDisplay anvil = (BlockDisplay) player.getWorld().spawnEntity(loc.clone().add(0, initialHeight, 0), EntityType.BLOCK_DISPLAY);
        anvil.setBlock(Bukkit.createBlockData(Material.ANVIL));
        anvil.setTransformationMatrix(new Matrix4f().identity().scale(5, 5, 5));

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.8f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 2f, 1f);

        if (hasSmolder) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 2f, 1f);
            stack(player, "smolder", -1, "Smolder", 5000);
        }

        Location finalLoc = loc;
        new BukkitRunnable() {
            boolean hitInitLoc = false;
            boolean anvilHit = false;
            double currRadius = 2;
            @Override
            public void run() {
                if (anvilHit) {
                    if (currRadius >= radius) {
                        anvil.remove();
                        this.cancel();
                    }

                    currRadius += radius/ticksForShockwave;

                    Location initLoc = anvil.getLocation().subtract(-2.5, 0, 2.5);

                    for (Vector vec : ParticleUtils.getCirclePoints(currRadius, 2 * Math.PI * radius)) {
                        Location currLoc = initLoc.clone().add(vec);

                        for (int i = 0; i < 4; i++){
                            if (currLoc.clone().add(0, 0.1, 0).getBlock().isSolid()) {
                                currLoc.add(0, 1, 0);
                            } else if (!currLoc.clone().subtract(0, 0.1, 0).getBlock().isSolid()) {
                                currLoc.add(0, -1, 0);
                            } else {
                                break;
                            }
                        }

                        anvil.getWorld().spawnParticle(Particle.DUST, currLoc, 0, 0, 0, 0, 0,
                                ParticleUtils.getDustOptionsFromGradient(gradient, 2f));

                        if (hasSmolder) {
                            anvil.getWorld().spawnParticle(Particle.LAVA, currLoc, 0, 0, 0, 0);
                        }

                        for (Entity entity : currLoc.getNearbyEntities(1, 1, 1)) {
                            if (!(entity instanceof LivingEntity livingMonster) || (entity == player) || (entity == anvil) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                                continue;

                            if (entity instanceof IronGolem golem && PlayerStats.isSummoned(player, entity) && entity.getScoreboardTags().contains("livingForge") && hasSmolder) {
                                LivingForge.overheat(golem);
                            } else {
                                stun(player, livingMonster, stunTicks);
                                if (hasSmolder) livingMonster.setFireTicks(fireTicks);
                            }
                        }
                    }

                    return;
                }

                Location tpLoc = anvil.getLocation().subtract(0, ((double) initialHeight)/ticksToFall, 0);
                anvil.teleport(tpLoc);
                if (Math.abs(tpLoc.getY() - finalLoc.getY()) < 0.01) {
                    hitInitLoc = true;
                }

                if (tpLoc.clone().add(-2.5, -1, 2.5).getBlock().getType().isSolid() && hitInitLoc) {
                    anvilHit = true;

                    Location location = anvil.getLocation().clone().subtract(-2.5, 0, 2.5);

                    anvil.getWorld().playSound(location, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1f, 0.2f);
                    anvil.getWorld().playSound(location, Sound.BLOCK_BEEHIVE_ENTER, 0.8f, 0.6f);
                    anvil.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.15f, 0.1f);
                    anvil.getWorld().playSound(location, Sound.ENTITY_IRON_GOLEM_REPAIR, 0.5f, 0.7f);

                    if (hasSmolder) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2f, 1f);
                    }

                    anvil.setBlock(Bukkit.createBlockData(Material.DAMAGED_ANVIL));

                    for (Entity entity : location.getNearbyEntities(2.5, 2.5, 2.5)) {
                        if (!(entity instanceof LivingEntity livingMonster) || (entity == player) || (entity == anvil) || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)) || PlayerStats.isSummoned(player, entity))
                            continue;
                        livingMonster.damage(damage);
                        stun(player, livingMonster, stunTicks);
                        if (hasSmolder) livingMonster.setFireTicks(fireTicks);
                    }
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "groundBreaker";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.create(Material.ANVIL);
    }
}
