package net.minervamc.minerva.skills.greek.aphrodite;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.SkinTrait;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MirrorImage extends Skill {

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long dur = 200;
        Location location = player.getLocation();
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        npc.getOrAddTrait(SkinTrait.class).setSkinName(player.getName());
        npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
        npc.getOrAddTrait(Gravity.class).setHasGravity(true);
        PlayerProfile profile = player.getPlayerProfile();
        profile.getTextures();

        npc.spawn(location);
        npc.getEntity().addScoreboardTag("mirrorImage");
        npc.getEntity().addScoreboardTag(player.getUniqueId().toString());
        ((Player) npc.getEntity()).setNoDamageTicks(0);
        npc.setProtected(false);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= dur) {
                    npc.despawn();
                    CitizensAPI.getNPCRegistry().deregister(npc);
                    cancel();
                    return;
                }

                for (Entity e : npc.getEntity().getLocation().getNearbyEntities(10, 10, 10)) {
                    if (e instanceof Monster monster) monster.setTarget((LivingEntity) npc.getEntity());
                }

                ticks++;

                if (player.isSneaking()) {
                    npc.setSneaking(true);
                } else {
                    npc.setSneaking(false);
                }

                Location playerLoc = player.getLocation();
                Location npcLoc = location.clone();
                npcLoc.setY(npc.getEntity().getLocation().getY());

                double offsetX = playerLoc.getX() - location.getX();
                double offsetZ = playerLoc.getZ() - location.getZ();

                npcLoc.setX(location.getX() - offsetX);
                npcLoc.setZ(location.getZ() - offsetZ);
                npcLoc.setYaw(-playerLoc.getYaw());
                if (!npcLoc.getBlock().isSolid()) {
                    npc.teleport(npcLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }

                if (npc.getEntity().isOnGround()) {
                    npc.getEntity().setVelocity(player.getVelocity());
                } else {
                    npc.getEntity().setVelocity(player.getVelocity().clone().add(new Vector(0, -0.05, 0)));
                }

                if ((Entity) npc.getEntity() instanceof LivingEntity lE) {
                    EntityEquipment equipment = lE.getEquipment();
                    assert equipment != null;
                    equipment.setItemInMainHand(player.getInventory().getItemInMainHand());
                    equipment.setItemInOffHand(player.getInventory().getItemInOffHand());
                    equipment.setHelmet(player.getInventory().getHelmet());
                    equipment.setChestplate(player.getInventory().getChestplate());
                    equipment.setLeggings(player.getInventory().getLeggings());
                    equipment.setBoots(player.getInventory().getBoots());
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 1L, 1L);
    }

    public static void explode(Location location, Player creator) {

    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "mirrorImage";
    }

    @Override
    public ItemStack getItem() {
        return null;
    }
}
