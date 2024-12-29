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
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BlindingLove extends Skill {
    private final List<UUID> vanishedPlayers = new ArrayList<>();

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 10000;
        int duration = 40;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "blindingLove")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "blindingLove", cooldown);
        cooldownAlarm(player, cooldown, "Blinding Love");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.6f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FOX_SNIFF, 2f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.5f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));

        for (Entity targeting : player.getNearbyEntities(100, 100, 100)) {
            if (targeting instanceof Monster && ((Monster) targeting).getTarget() == player) {
                ((Monster) targeting).setTarget(null);
            }
        }

        try {
            hidePlayer(player);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger().info("Exception at Blinding Love in function hidePlayer()");
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
        return "blindingLove";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
