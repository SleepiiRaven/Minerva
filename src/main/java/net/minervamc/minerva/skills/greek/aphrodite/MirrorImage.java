package net.minervamc.minerva.skills.greek.aphrodite;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.HashMap;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.SkinTrait;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
    public static HashMap<Player, NPC> mirrors = new HashMap<>();

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long dur = 150;
        double rad = 5;
        double damage = 7.5;
        long cooldown = 20000;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "mirrorImage")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "mirrorImage", cooldown);
        cooldownAlarm(player, cooldown, "Mirror Image");

        Location location = player.getLocation();
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        npc.getOrAddTrait(SkinTrait.class).setSkinName(player.getName());
        npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
        npc.getOrAddTrait(Gravity.class).setHasGravity(true);
        PlayerProfile profile = player.getPlayerProfile();
        profile.getTextures();

        mirrors.put(player, npc);

        npc.spawn(location);
        npc.getEntity().addScoreboardTag("mirrorImage");
        npc.getEntity().addScoreboardTag(player.getUniqueId().toString());
        PlayerStats.summon(player, npc.getEntity());
        ((Player) npc.getEntity()).setNoDamageTicks(0);
        npc.setProtected(false);

        player.getWorld().playSound(player, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.5f, 1.9f);
        player.getWorld().playSound(player, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.75f, 1.6f);
        player.getWorld().playSound(player, Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.5f, 1.4f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= dur) {
                    npc.getEntity().getWorld().spawnParticle(Particle.REVERSE_PORTAL, npc.getEntity().getLocation(), 40, 1, 1, 1, 1);
                    npc.getEntity().getWorld().playSound(npc.getEntity().getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.9f);
                    npc.getEntity().getWorld().playSound(npc.getEntity().getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2f, 0.5f);
                    PlayerStats.removeSummon(player, npc.getEntity());
                    npc.despawn();
                    mirrors.remove(player);
                    CitizensAPI.getNPCRegistry().deregister(npc);
                    cancel();
                    return;
                }

                if (npc.getEntity().isDead()) {
                    npc.getEntity().getWorld().spawnParticle(Particle.REVERSE_PORTAL, npc.getEntity().getLocation(), 80, 1, 1, 1, 1);
                    npc.getEntity().getWorld().spawnParticle(Particle.INSTANT_EFFECT, npc.getEntity().getLocation(), 80, 2, 2, 2, 1);
                    npc.getEntity().getWorld().spawnParticle(Particle.DUST, npc.getEntity().getLocation(), 80, 1, 1, 1, 1, new Particle.DustOptions(Color.fromRGB(212,16,81), 2f));
                    npc.getEntity().getWorld().playSound(npc.getEntity().getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.9f);
                    npc.getEntity().getWorld().playSound(npc.getEntity().getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2f, 0.5f);
                    npc.getEntity().getWorld().playSound(npc.getEntity().getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 2f, 0.95f);

                    for (Entity entity : npc.getEntity().getLocation().getNearbyEntities(rad, rad, rad)) {
                        if (!(entity instanceof LivingEntity livingMonster) || entity == player || (entity instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer)))
                            continue;

                        damage(livingMonster, damage, player);
                    }

                    PlayerStats.removeSummon(player, npc.getEntity());
                    npc.despawn();
                    CitizensAPI.getNPCRegistry().deregister(npc);
                    this.cancel();;
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
