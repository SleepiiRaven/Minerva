package net.minervamc.minerva.skills.greek.hades;

import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class SkeletalHands extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown;
        long triggerRate; // Time between skeletal hands' particle and grab triggers, measured in ticks.
        double range; // How far away skeletal hands can be spawned
        double effectRadius; // Radius of each hand's effect
        int handCount; // How many hands
        int maxTicks; //How many times each hand can grab for the spell's lifetime. multiply this by the trigger rate to get amount of ticks until end, 20 ticks in a second.

        switch (level) {
            case 2 -> {
                cooldown = 10000;
                triggerRate = 20;
                range = 5;
                effectRadius = 1.5;
                handCount = 5;
                maxTicks = 5;
            }
            case 3 -> {
                cooldown = 10000;
                triggerRate = 20;
                range = 5;
                effectRadius = 2;
                handCount = 6;
                maxTicks = 5;
            }
            case 4 -> {
                cooldown = 10000;
                triggerRate = 20;
                range = 5;
                effectRadius = 2;
                handCount = 8;
                maxTicks = 6;
            }
            case 5 -> {
                cooldown = 8000;
                triggerRate = 20;
                range = 5;
                effectRadius = 2.5;
                handCount = 8;
                maxTicks = 8;
            }
            default -> {
                cooldown = 10000;
                triggerRate = 20;
                range = 5;
                effectRadius = 1;
                handCount = 5;
                maxTicks = 5;
            }
        }


        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "skeletalHands")) {
            onCooldown(player);
            return;
        } else if (!cooldownManager.isCooldownDone(player.getUniqueId(), "channelingOfTartarusCasting")) {
            skillLocked(player, "you are currently casting Channeling of Tartarus");
            return;
        }

        World world = player.getWorld();
        Location pLoc = player.getLocation();
        List<Location> locations = new ArrayList<>();
        List<ArmorStand> hands = new ArrayList<>();
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "skeletalHands", cooldown);
        cooldownAlarm(player, cooldown, "Skeletal Hands");

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAT_HISS, 0.4f, 0.4f);
        for (int i = 0; i < handCount; i++) {
            double xOffset = FastUtils.randomDoubleInRange(-range, range);
            double zOffset = FastUtils.randomDoubleInRange(-range, range);
            Location handLoc = pLoc.clone().add(new Vector(xOffset, 0.1, zOffset));
            // Check if the handLoc is inside a block
            locations.add(handLoc);

        }
        for (Location handLoc : locations) {
            if (handLoc == null) continue;

            ArmorStand armorStand = (ArmorStand) world.spawnEntity(new Location(handLoc.getWorld(), 0, -50, 0), EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setSmall(true);
            armorStand.setInvulnerable(false);

            // Create a bone item
            ItemStack boneItem = new ItemStack(Material.BONE);
            armorStand.setRightArmPose(new EulerAngle(Math.toRadians(90), 0, 0));
            armorStand.getEquipment().setItemInMainHand(boneItem);

            armorStand.teleport(handLoc);

            hands.add(armorStand);
        }
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline() || ticks >= maxTicks) {
                    for (ArmorStand hand : hands) {
                        hand.getWorld().playSound(hand.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1f);
                        hand.remove();
                    }
                    this.cancel();
                }
                for (ArmorStand hand : hands) {
                    if (hand.isDead()) {
                        hand = null;
                        continue;
                    }
                    hand.getWorld().playSound(hand.getLocation(), Sound.ENTITY_WITHER_SKELETON_STEP, 1f, 1f);
                    for (Vector particlePoint : ParticleUtils.getCirclePoints(effectRadius)) {
                        hand.getWorld().spawnParticle(Particle.DUST, hand.getLocation().add(particlePoint), 0, 0, 0, 0.125, new Particle.DustOptions(Color.fromRGB(32, 32, 32), 1));
                        hand.getWorld().spawnParticle(Particle.DUST, hand.getLocation().add(particlePoint), 0, 0, 0, 0.125, new Particle.DustOptions(Color.fromRGB(40, 40, 40), 1));
                    }
                    for (Vector particlePoint : ParticleUtils.getFilledCirclePoints(effectRadius, 2)) {
                        hand.getWorld().spawnParticle(Particle.DUST, hand.getLocation().add(particlePoint), 0, 0, 0, 0.125, new Particle.DustOptions(Color.fromRGB(32, 32, 32), 1));
                        hand.getWorld().spawnParticle(Particle.DUST, hand.getLocation().add(particlePoint), 0, 0, 0, 0.125, new Particle.DustOptions(Color.fromRGB(40, 40, 40), 1));
                    }
                    for (Entity entity : hand.getLocation().getNearbyEntities(effectRadius, effectRadius, effectRadius)) {
                        if (entity instanceof LivingEntity livingEntity && entity != player && !(entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                            if (entity instanceof Player grabbed) {
                                if (cooldownManager.isCooldownDone(grabbed.getUniqueId(), "skeletalHandsWarning")) {
                                    grabbed.sendMessage(ChatColor.YELLOW + "You have been grabbed by " + player.getName() + "'s Skeletal Hands. Crouch to escape!");
                                }
                                cooldownManager.setCooldownFromNow(grabbed.getUniqueId(), "skeletalHandsWarning", (triggerRate * (maxTicks + 1) * 50));
                            }
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    entity.setVelocity(new Vector(0, 0, 0));
                                }
                            }.runTaskLater(Minerva.getInstance(), 1L);
                            final ArmorStand finalHand = hand;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (entity instanceof Player grabbed && grabbed.isSneaking()) {
                                        finalHand.remove();
                                        if (cooldownManager.isCooldownDone(grabbed.getUniqueId(), "skeletalHandsEscapeMessage" + player.getName())) {
                                            grabbed.sendMessage(ChatColor.YELLOW + "You escaped " + player.getName() + "'s Skeletal Hands' grasp!");
                                            cooldownManager.setCooldownFromNow(grabbed.getUniqueId(), "skeletalHandsEscapeMessage" + player.getName(), (long) 6000);
                                        }
                                        if (cooldownManager.isCooldownDone(player.getUniqueId(), "skeletalHandsVictimEscapeMessage" + grabbed.getName())) {
                                            player.sendMessage(ChatColor.YELLOW + grabbed.getName() + " escaped your Skeletal Hands' grasp.");
                                            cooldownManager.setCooldownFromNow(player.getUniqueId(), "skeletalHandsVictimEscapeMessage" + grabbed.getName(), (long) 6000);
                                        }
                                        this.cancel();
                                        return;
                                    } else if (finalHand.isDead()) {
                                        this.cancel();
                                    }
                                    Location tpLoc = finalHand.getLocation().clone();
                                    tpLoc.setDirection(entity.getLocation().getDirection());
                                    entity.teleport(tpLoc);
                                }
                            }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, triggerRate);
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> "?";
            case 2 -> "??";
            case 3 -> "???";
            case 4 -> "????";
            case 5 -> "?????";
            default -> "-";
        };
    }

    @Override
    public String toString() {
        return "skeletalHands";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.BONE), ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "[Skeletal Hands]", ChatColor.GRAY + "Skeletal hands rise from the ground, grabbing at any enemies that enter their range. If an", ChatColor.GRAY + "enemy is trapped, they are stuck for the duration of the ability or until they crouch to escape.");
    }
}
