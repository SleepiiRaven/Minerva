package net.minervamc.minerva.types;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public abstract class Skill {
    public static Skill fromString(String string) {
        return switch (string) {
            case "channelingOfTartarus" -> Skills.CHANNELING_OF_TARTARUS;
            case "umbrakinesisHades" -> Skills.UMBRAKINESIS_HADES;
            case "skeletalHands" -> Skills.SKELETAL_HANDS;
            case "shadowTravel" -> Skills.SHADOW_TRAVEL;
            case "windWall" -> Skills.WIND_WALL;
            case "soar" -> Skills.SOAR;
            case "lightningToss" -> Skills.LIGHTNING_TOSS;
            case "stormsEmbrace" -> Skills.STORMS_EMBRACE;
            case "seismicBlast" -> Skills.SEISMIC_BLAST;
            case "oceansSurge" -> Skills.OCEANS_SURGE;
            case "tidalWave" -> Skills.TIDAL_WAVE;
            case "aquaticLimbExtensions" -> Skills.AQUATIC_LIMB_EXTENSIONS;
            case "lifeSteal" -> Skills.LIFE_STEAL;
            case "oceansEmbrace" -> Skills.OCEANS_EMBRACE;
            case "protectiveCloud" -> Skills.PROTECTIVE_CLOUD;
            case "apollosHymn" -> Skills.APOLLOS_HYMN;
            case "arrowsOfTheSun" -> Skills.ARROWS_OF_THE_SUN;
            case "burningLight" -> Skills.BURNING_LIGHT;
            case "enhancedArchery" -> Skills.ENHANCED_ARCHERY;
            case "plagueVolley" -> Skills.PLAGUE_VOLLEY;
            case "callOfTheWild" -> Skills.CALL_OF_THE_WILD;
            case "huntressAgility" -> Skills.HUNTRESS_AGILITY;
            case "nimbleDash" -> Skills.NIMBLE_DASH;
            case "superCharged" -> Skills.SUPER_CHARGED;
            case "sharpshooter" -> Skills.SHARPSHOOTER;
            case "drunkenRevelry" -> Skills.DRUNKEN_REVELRY;
            case "frenziedDance" -> Skills.FRENZIED_DANCE;
            case "grapeShot" -> Skills.GRAPE_SHOT;
            case "vineWhip" -> Skills.VINE_WHIP;
            case "madGodsDrink" -> Skills.MAD_GODS_DRINK;
            case "aresBlessing" -> Skills.ARES_BLESSING;
            case "spiritOfVengeance" -> Skills.SPIRITOFVENGEANCE;
            case "cleave" -> Skills.CLEAVE;
            case "primalScream" -> Skills.PRIMAL_SCREAM;
            case "tomahawkThrow" -> Skills.TOMAHAWK_THROW;
            default -> Skills.DEFAULT;
        };
    }

    public static void cooldownAlarm(Player player, long cooldownTime, String abilityName) {
        long cooldownTimeInTicks = (cooldownTime / 50);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " is no longer on cooldown!");
                }
            }
        }.runTaskLater(Minerva.getInstance(), cooldownTimeInTicks);
    }

    public static void onCooldown(Player player) {
        player.sendMessage(ChatColor.RED + "That ability is currently on cooldown.");
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }

    public static void skillLocked(Player player, String reason) {
        player.sendMessage(ChatColor.RED + "That skill is currently locked because " + reason + ".");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }

    public static void stun(Player inflictor, Entity entity, long stunTicks) {
        Location tpLoc = entity.getLocation();

        if (entity instanceof Player player && Party.isPlayerInPlayerParty(inflictor, player)) {
            return;
        }
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= stunTicks || (entity instanceof LivingEntity lE && lE.getHealth() <= 0.1)) {
                    this.cancel();
                    return;
                }
                tpLoc.setDirection(entity.getLocation().getDirection());
                entity.teleport(tpLoc);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    public abstract void cast(Player player, CooldownManager cooldownManager, int level);

    public abstract String getLevelDescription(int level);

    public abstract String toString();

    public abstract ItemStack getItem();

    public static void damage(LivingEntity livingEntity, double damage, Player damager) {
        if (livingEntity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP()) {
            return;
        }

        if (livingEntity instanceof Tameable) {
            if (((Tameable) livingEntity).getOwner() != null) {
                return;
            }
        }

        double magicDamage = 0;
        double magicResist = 0;
        if (MMOPlayerData.isLoaded(damager.getUniqueId())) {
            magicDamage = MMOPlayerData.get(damager.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE");
        }

        if (MMOPlayerData.isLoaded(livingEntity.getUniqueId())) {
            magicResist = MMOPlayerData.get(livingEntity.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE_REDUCTION");
        }

        damage *= (magicDamage + 100)/100;
        damage *= (100 - magicResist)/100;

        if (livingEntity.getNoDamageTicks() > 0) return;

        if (livingEntity.getHealth()-damage <= 0) {
            if (livingEntity.isDead()) {
                return;
            }
            livingEntity.setHealth(0);
            livingEntity.setKiller(damager);
            if (livingEntity instanceof Player p) {
                Bukkit.getPluginManager().callEvent(new PlayerDeathEvent(p, DamageSource.builder(DamageType.MAGIC).build(), Arrays.asList(p.getInventory().getStorageContents()), 0, Component.text(p.getName() + " was killed by " + damager.getName() + "'s heritage skill.")));
            }
        } else {
            livingEntity.setHealth(livingEntity.getHealth() - damage);
        }
        livingEntity.damage(0, damager);
    }

    public static void damage(LivingEntity livingEntity, double damage, Player damager, boolean ignoreInvulnTicks, boolean addStr) {
        if (livingEntity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP()) {
            return;
        }

        if (livingEntity instanceof Tameable) {
            if (((Tameable) livingEntity).getOwner() != null) {
                return;
            }
        }

        double magicDamage = 0;
        double magicResist = 0;
        if (MMOPlayerData.isLoaded(damager.getUniqueId())) {
            magicDamage = MMOPlayerData.get(damager.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE");
        }

        if (MMOPlayerData.isLoaded(livingEntity.getUniqueId())) {
            magicResist = MMOPlayerData.get(livingEntity.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE_REDUCTION");
        }

        if (damager.hasPotionEffect(PotionEffectType.STRENGTH)) {
            damage += 3 * (damager.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1);
        }

        damage *= (magicDamage + 100)/100;
        damage *= (100 - magicResist)/100;

        if (livingEntity.getNoDamageTicks() > 0 && !ignoreInvulnTicks) return;

        if (livingEntity.getHealth()-damage <= 0) {
            if (livingEntity.isDead()) {
                return;
            }
            livingEntity.setHealth(0);
            livingEntity.setKiller(damager);
            if (livingEntity instanceof Player p) {
                Bukkit.getPluginManager().callEvent(new PlayerDeathEvent(p, DamageSource.builder(DamageType.MAGIC).build(), Arrays.asList(p.getInventory().getStorageContents()), 0, Component.text(p.getName() + " was killed by " + damager.getName() + "'s heritage skill.")));
            }
        } else {
            livingEntity.setHealth(livingEntity.getHealth() - damage);
        }
        livingEntity.damage(0, damager);
    }

    public static void knockback(Entity entity, Vector kb) {
        if (entity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (entity instanceof Player && !entity.getWorld().getPVP()) {
            return;
        }

        entity.setVelocity(kb);
    }
}
