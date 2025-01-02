package net.minervamc.minerva.skills.greek.aphrodite;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BlindingDoves extends Skill {
    private final List<UUID> vanishedPlayers = new ArrayList<>();

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;
        int duration = 100;
        int finalRadius = 5;
        int rotations = 3;
        int feathers = 5;
        double damage = 2;
        int blindnessDur = 20;
        int blindnessAmp = 5;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "blindingDoves")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "blindingDoves", cooldown);
        cooldownAlarm(player, cooldown, "Blinding Doves");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.6f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FOX_SNIFF, 2f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOOL_PLACE, 2f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 0.5f);

        doveEffect(player, finalRadius, rotations, feathers, duration, damage, blindnessDur, blindnessAmp);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));

        for (Entity targeting : player.getNearbyEntities(100, 100, 100)) {
            if (targeting instanceof Monster && ((Monster) targeting).getTarget() == player) {
                ((Monster) targeting).setTarget(null);
            }
        }

        try {
            hidePlayer(player);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger().info("Exception at Blinding Doves in function hidePlayer()");
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= duration) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_THROWN, 1.4f, 0.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.45f, 0.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 2f);

                    player.setInvisible(false);

                    try {
                        showPlayer(player);
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    this.cancel();
                }

                player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, player.getLocation(), 20, 1, 1, 1, 1);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void doveEffect(Player player, int finalRadius, int rotations, int feathers, int duration, double damage, int blindnessDur, int blindnessAmp) {
        new BukkitRunnable() {
            double ticks = 0;

            @Override
            public void run() {
                if (ticks > duration || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location pLoc = player.getLocation();
                double t = ticks/duration;
                double angle = Math.PI * 2 * t;
                double radius = finalRadius * Math.min((t * finalRadius), 1);

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity livingEntity) || entity == player ||
                            (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) continue;

                    damage(livingEntity, damage, player);
                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDur, blindnessAmp));
                }

                if (MirrorImage.mirrors.get(player) != null && MirrorImage.mirrors.get(player).isSpawned()) {
                    for (Entity entity : MirrorImage.mirrors.get(player).getEntity().getNearbyEntities(radius, radius, radius)) {
                        if (!(entity instanceof LivingEntity livingEntity) || entity == player ||
                                (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;

                        damage(livingEntity, damage, player);
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDur, blindnessAmp));
                    }
                }

                for (int i = 0; i < feathers; i++) {
                    double a = (rotations * angle) + (i * 2 * Math.PI / feathers);
                    double x = radius * Math.cos(a);
                    double z = radius * Math.sin(a);
                    player.getWorld().spawnParticle(Particle.CLOUD, pLoc.clone().add(new Vector(x, 1 + Math.sin(4*angle), z)), 1, 0, 0, 0, 0);
                    if (MirrorImage.mirrors.get(player) != null && MirrorImage.mirrors.get(player).isSpawned()) {
                        Entity mirror = MirrorImage.mirrors.get(player).getEntity();
                        mirror.getWorld().spawnParticle(Particle.CLOUD, mirror.getLocation().add(new Vector(x, 1, z)), 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    private void hidePlayer(Player player) throws InvocationTargetException {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        PacketContainer destroyPacket = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

        destroyPacket.getIntLists().write(0, List.of(player.getEntityId()));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            manager.sendServerPacket(online, destroyPacket);
        }

        vanishedPlayers.add(player.getUniqueId());
    }

    private void showPlayer(Player player) throws InvocationTargetException {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            manager.updateEntity(player, List.of(online));
        }

        vanishedPlayers.remove(player.getUniqueId());
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "blindingDoves";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
