package net.minervamc.minerva.utils;

import de.tr7zw.nbtapi.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import java.util.UUID;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.comp.mmocore.MMOCoreHook;
import net.Indyuce.mmoitems.comp.rpg.McMMOHook;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.types.SkillType;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class SkillUtils {
    public static void redirect(Player player, UUID pUUID, SkillType skillType) {
        PlayerStats stats = PlayerStats.getStats(pUUID);
        Skill spell = getSkill(skillType, stats);
        int level = 1;
        switch (skillType) {
            case RRR -> {
                level = stats.getRRRLevel();
                if (!stats.getRRRActive()) return;
            }
            case RLR -> {
                level = stats.getRLRLevel();
                if (!stats.getRLRActive()) return;
            }
            case RLL -> {
                level = stats.getRLLLevel();
                if (!stats.getRLLActive()) return;
            }
            case RRL -> {
                level = stats.getRRLLevel();
                if (!stats.getRRLActive()) return;
            }
            case PASSIVE -> {
                // THIS CODE WILL NEVER BE RAN
                if (!stats.getPassiveActive()) return;
            }
        }
        if (spell == null) return;
        spell.cast(player, Minerva.getInstance().getCdInstance(), level);
    }

    public static void setDefaultSkills(HeritageType heritageType, Player player) {
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        stats.setPoints(stats.getMaxPoints());
        stats.setRRRLevel(1);
        stats.setRLRLevel(1);
        stats.setRLLLevel(1);
        stats.setRRLLevel(1);
        stats.setPassiveLevel(1);
        stats.setRRRActive(true);
        stats.setRLRActive(true);
        stats.setRLLActive(true);
        stats.setRRLActive(true);
        stats.setPassiveActive(true);
        switch (heritageType) {
            case HADES, PLUTO ->
                    setSkills(player, Skills.SHADOW_TRAVEL, Skills.UMBRAKINESIS_HADES, Skills.CHANNELING_OF_TARTARUS, Skills.SKELETAL_HANDS, Skills.LIFE_STEAL);
            case ZEUS, JUPITER ->
                    setSkills(player, Skills.SOAR, Skills.LIGHTNING_TOSS, Skills.STORMS_EMBRACE, Skills.WIND_WALL, Skills.PROTECTIVE_CLOUD);
            case POSEIDON, NEPTUNE ->
                    setSkills(player, Skills.TIDAL_WAVE, Skills.AQUATIC_LIMB_EXTENSIONS, Skills.SEISMIC_BLAST, Skills.OCEANS_SURGE, Skills.OCEANS_EMBRACE);
            case APOLLO_GREEK, APOLLO_ROMAN ->
                    setSkills(player, Skills.APOLLOS_HYMN, Skills.PLAGUE_VOLLEY, Skills.BURNING_LIGHT, Skills.ENHANCED_ARCHERY, Skills.ARROWS_OF_THE_SUN);
            case ARTEMIS, DIANA ->
                    setSkills(player, Skills.NIMBLE_DASH, Skills.CALL_OF_THE_WILD, Skills.SUPER_CHARGED, Skills.SHARPSHOOTER, Skills.HUNTRESS_AGILITY);
            case DIONYSUS, BACCHUS ->
                    setSkills(player, Skills.VINE_WHIP, Skills.GRAPE_SHOT, Skills.MAD_GODS_DRINK, Skills.FRENZIED_DANCE, Skills.DRUNKEN_REVELRY);
            case ARES, MARS ->
                    setSkills(player, Skills.SPIRITOFVENGEANCE, Skills.TOMAHAWK_THROW, Skills.CLEAVE, Skills.PRIMAL_SCREAM, Skills.ARES_BLESSING);
        }
    }

    private static void setSkills(Player player, Skill rrr, Skill rlr, Skill rll, Skill rrl, Skill passive) {
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        stats.setSkillRRR(rrr);
        stats.setSkillRLR(rlr);
        stats.setSkillRLL(rll);
        stats.setSkillRRL(rrl);
        stats.setPassive(passive);
        stats.save();
    }

    public static Skill getSkill(SkillType skillType, PlayerStats stats) {
        return switch (skillType) {
            case RRR -> stats.getSkillRRR();
            case RLR -> stats.getSkillRLR();
            case RLL -> stats.getSkillRLL();
            case RRL -> stats.getSkillRRL();
            case PASSIVE -> stats.getPassive();
        };
    }

    public static void damage(LivingEntity livingEntity, double damage, Player damager) {
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
            livingEntity.setHealth(0);
            livingEntity.setKiller(damager);
        } else {
            livingEntity.setHealth(livingEntity.getHealth() - damage);
        }
        livingEntity.damage(0, damager);
    }
}
