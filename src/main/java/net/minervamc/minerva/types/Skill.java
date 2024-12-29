package net.minervamc.minerva.types;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.hephaestus.Smolder;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
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
            case "spiritOfVengeance" -> Skills.SPIRIT_OF_VENGEANCE;
            case "cleave" -> Skills.CLEAVE;
            case "primalScream" -> Skills.PRIMAL_SCREAM;
            case "tomahawkThrow" -> Skills.TOMAHAWK_THROW;
            case "groundBreaker" -> Skills.GROUND_BREAKER;
            case "livingForge" -> Skills.LIVING_FORGE;
            case "magmatism" -> Skills.MAGMATISM;
            case "shrapnelGrenade" -> Skills.SHRAPNEL_GRENADE;
            case "smolder" -> Skills.SMOLDER;
            case "centerOfAttention" -> Skills.CENTER_OF_ATTENTION;
            case "charm" -> Skills.CHARM;
            case "blindingLove" -> Skills.BLINDING_LOVE;
            case "mirrorImage" -> Skills.MIRROR_IMAGE;
            case "heartSeeker" -> Skills.HEART_SEEKER;
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
        if (entity.hasMetadata("NPC")) return;

        if (PlayerStats.isSummoned(inflictor, entity)) return;

        Location tpLoc = entity.getLocation();

        if (entity instanceof Player player && Party.isPlayerInPlayerParty(inflictor, player)) {
            return;
        }

        if (entity instanceof Player player) {
            player.sendActionBar(Component.text("STUNNED!", TextColor.color(255,223,62)));
        }

        Color[] gradient = {
                Color.fromRGB(255,249,221),
                Color.fromRGB(255,246,198),
                Color.fromRGB(255,239,156),
                Color.fromRGB(255,223,62),
                Color.fromRGB(255,213,0)
        };

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= stunTicks || (entity instanceof LivingEntity lE && lE.getHealth() <= 0.1)) {
                    this.cancel();
                    return;
                }

                if (ticks % 3 == 0 && ticks < stunTicks - 2L) {
                    Location loc = entity.getLocation().clone().add(new Vector(0, 2.5, 0));
                    for (Vector vec : ParticleUtils.getCirclePoints(0.5)) {
                        Location particleLoc = loc.clone().add(vec);
                        loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0, ParticleUtils.getDustOptionsFromGradient(gradient, 1f));
                    }
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
        if (livingEntity.hasMetadata("NPC")) return;
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

        if (livingEntity.getNoDamageTicks() > 0) return;

        DamageMetadata damageFinal = new DamageMetadata(damage, DamageType.MAGIC);
        AttackMetadata attack = new AttackMetadata(damageFinal, MMOPlayerData.get(damager).getStatMap().cache(EquipmentSlot.MAIN_HAND));
        MythicLib.plugin.getDamage().damage(attack, livingEntity);
    }

    public static void damage(LivingEntity livingEntity, double damage, Player damager, boolean ignoreInvulnTicks, boolean addStr) {
        if (livingEntity.hasMetadata("NPC")) return;

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

        if (damager.hasPotionEffect(PotionEffectType.STRENGTH) && addStr) {
            damage += 3 * (damager.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1);
        }

        if (livingEntity.getNoDamageTicks() > 0 && !ignoreInvulnTicks) return;

        DamageMetadata damageFinal = new DamageMetadata(damage, DamageType.MAGIC);
        AttackMetadata attack = new AttackMetadata(damageFinal, MMOPlayerData.get(damager).getStatMap().cache(EquipmentSlot.MAIN_HAND));
        MythicLib.plugin.getDamage().damage(attack, livingEntity);
    }

    public static void knockback(Entity entity, Vector kb) {
        if (entity.hasMetadata("NPC")) return;

        if (entity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (entity instanceof Player && !entity.getWorld().getPVP()) {
            return;
        }

        entity.setVelocity(kb);
    }

    public static void stack(Player player, String ability, int increment, String abilityFormal, long timeUntilExpires) {
        if (player.hasMetadata("NPC")) return;
        Map<String, Integer> stackingAbilities = PlayerStats.getStats(player.getUniqueId()).getStackingAbilities();
        int newStacks = getStacks(player, ability) + increment;
        int maxStack = switch (ability) {
            case "smolder" -> ((Smolder) Skills.SMOLDER).stackSmolder(player, newStacks, timeUntilExpires);
            default -> 99;
        };
        if (maxStack < newStacks) newStacks = maxStack;

        if (newStacks <= 0) {
            stackingAbilities.remove(ability);
            Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), ability, 0L);
            return;
        }

        stackingAbilities.put(ability, newStacks);
        if (player.isOnline()) {
            player.sendActionBar(ChatColor.YELLOW + abilityFormal + " Stacks: " + newStacks);
        }

        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), ability, timeUntilExpires);
    }

    public static int getStacks(Player player, String ability) {
        if (player.hasMetadata("NPC")) return 0;
        Map<String, Integer> stackingAbilities = PlayerStats.getStats(player.getUniqueId()).getStackingAbilities();
        if (Minerva.getInstance().getCdInstance().isCooldownDone(player.getUniqueId(), ability)) {
            stackingAbilities.remove(ability);
            return 0;
        } else {
            return stackingAbilities.getOrDefault(ability, 0);
        }
    }
}
