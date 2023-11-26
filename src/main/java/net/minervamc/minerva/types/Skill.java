package net.minervamc.minerva.types;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class Skill {
    public abstract void cast(Player player, CooldownManager cooldownManager, int level);

    public abstract String getLevelDescription(int level);

    public abstract String toString();

    public abstract ItemStack getItem();

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
            case "quickDraw" -> Skills.QUICK_DRAW;
            case "superCharged" -> Skills.SUPER_CHARGED;
            case "sharpshooter" -> Skills.SHARPSHOOTER;
            default -> Skills.DEFAULT;
        };
    }

    public static void cooldownAlarm(Player player, long cooldownTime, String abilityName) {
        long cooldownTimeInTicks = (cooldownTime/50);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    //ItemUtils.sendItemMessage(player, ChatColor.GREEN + abilityName + " is no longer on cooldown!");
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

    public static void stun(Entity entity, long stunTicks) {
        Location tpLoc = entity.getLocation();
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= stunTicks) {
                    this.cancel();
                    return;
                }
                tpLoc.setDirection(entity.getLocation().getDirection());
                entity.teleport(tpLoc);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }
}
