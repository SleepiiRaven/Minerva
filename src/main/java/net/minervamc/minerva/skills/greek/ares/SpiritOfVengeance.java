package net.minervamc.minerva.skills.greek.ares;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.FastUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpiritOfVengeance extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long pillagerDespawnTicks = 600 / 5; // The runnable is every 5 seconds so the first number is the ticks you want :)
        long cooldown = pillagerDespawnTicks * 5 * 50 + 10000;
        int angerRadius = 10;

        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "spiritOfVengeance")) {
            onCooldown(player);
            return;
        }

        cooldownManager.setCooldownFromNow(player.getUniqueId(), "spiritOfVengeance", cooldown);
        cooldownAlarm(player, cooldown, "Spirit of Vengeance");

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1f, 1f);

        double tiny = 0.5;
        double inner = 1;
        double main = 3;
        double outer = 3.6;
        List<Vector> unitCircle = ParticleUtils.getCirclePoints(1);
        Vector a = getPoint(main, 5.0/6);
        Vector b = getPoint(main, 1.0/6);
        Vector c = getPoint(main, 3.0/2);
        Vector d = getPoint(main, 7.0/6);
        Vector e = getPoint(main, 1.0/2);
        Vector f = getPoint(main, -1.0/6);
        for (Vector vector : unitCircle) {
            Vector tinyVec = vector.clone().multiply(tiny);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(tinyVec), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(vector.clone().multiply(inner)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(vector.clone().multiply(main)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(vector.clone().multiply(outer)), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(d).add(tinyVec), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(e).add(tinyVec), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(f).add(tinyVec), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
        }
        List<Vector> lines = ParticleUtils.getLinePoints(a, b, 0.2);
        lines.addAll(ParticleUtils.getLinePoints(b, c, 0.2));
        lines.addAll(ParticleUtils.getLinePoints(c, a, 0.2));
        lines.addAll(ParticleUtils.getLinePoints(d, e, 0.2));
        lines.addAll(ParticleUtils.getLinePoints(e, f, 0.2));
        lines.addAll(ParticleUtils.getLinePoints(f, d, 0.2));

        for (Vector vector : lines) {
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(vector), 0, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 2f));
        }

        Vector pillagerDirection = new Vector(FastUtils.randomDoubleInRange(-1, 1), 0, FastUtils.randomDoubleInRange(-1, 1));
        Location loc = player.getLocation();
        ItemDisplay[] displays = new ItemDisplay[3];
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                switch (ticks) {
                    case 0:
                        displays[0] = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone().add(d).add(new Vector(0, 1, 0)), EntityType.ITEM_DISPLAY);
                        displays[0].setItemStack(new ItemStack(Material.BONE));
                        break;
                    case 1:
                        displays[1] = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone().add(e).add(new Vector(0, 1, 0)), EntityType.ITEM_DISPLAY);
                        displays[1].setItemStack(new ItemStack(Material.COOKED_BEEF));
                        break;
                    case 2:
                        displays[2] = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone().add(f).add(new Vector(0, 1, 0)), EntityType.ITEM_DISPLAY);
                        displays[2].setItemStack(new ItemStack(Material.ROTTEN_FLESH));
                        break;
                    case 3:
                        this.cancel();
                        return;
                }
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 5L, 5L);

        List<Pillager> pillagers = new ArrayList<>();
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks == 0) {
                    for (ItemDisplay display : displays) {
                        display.remove();
                    }
                    pillagers.add(summonPillager(loc, player, d, pillagerDirection, pillagerDespawnTicks));
                    pillagers.add(summonPillager(loc, player, e, pillagerDirection, pillagerDespawnTicks));
                    pillagers.add(summonPillager(loc, player, f, pillagerDirection, pillagerDespawnTicks));
                } else {

                    if (player.isDead() || !player.isOnline() || ticks >= pillagerDespawnTicks) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1f, 1f);
                        for (Pillager pillager : pillagers) {
                            pillager.remove();
                            Location particleLoc = pillager.getLocation();
                            pillager.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 10);
                        }
                        this.cancel();
                    }

                    for (Pillager pillager : pillagers) {
                        for (Entity entity : pillager.getWorld().getNearbyEntities(pillager.getLocation(), angerRadius, angerRadius, angerRadius)) {
                            if (!(entity instanceof LivingEntity potentialTarget) || (potentialTarget instanceof Tameable && ((Tameable) potentialTarget).getOwner() != null) || potentialTarget.getScoreboardTags().contains(player.getUniqueId().toString()) || potentialTarget == player || (potentialTarget instanceof Player livingPlayer && Party.isPlayerInPlayerParty(player, livingPlayer))) {
                                if (pillager.getTarget() == player) {
                                    pillager.setTarget(null);
                                }
                                continue;
                            }
                            pillager.setTarget(potentialTarget);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 30L, 5L);
    }

    private Vector getPoint(double r, double piMod) {
        return new Vector(r * Math.cos(piMod * Math.PI), 0, r * Math.sin(piMod * Math.PI));
    }

    private Pillager summonPillager(Location loc, Player player, Vector offset, Vector pillagerDirection, long pillagerDespawnTicks) {

        Location pillagerLocation = loc.clone().add(offset);
        loc.getWorld().playSound(loc.clone(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1f, 0.4f);

        Pillager pillager = (Pillager) loc.getWorld().spawnEntity(pillagerLocation.setDirection(pillagerDirection), EntityType.PILLAGER);
        pillager.addScoreboardTag("aresSummoned");
        pillager.addScoreboardTag(player.getUniqueId().toString());
        pillager.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) (pillagerDespawnTicks * 5), 3));
        return pillager;
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "spiritOfVengeance";
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Material.VINE);
    }
}
