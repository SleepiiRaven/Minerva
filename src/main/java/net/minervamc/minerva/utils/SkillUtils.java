package net.minervamc.minerva.utils;

import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.types.SkillType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
            case HADES, PLUTO -> setSkills(player, Skills.SHADOW_TRAVEL, Skills.UMBRAKINESIS_HADES, Skills.CHANNELING_OF_TARTARUS, Skills.SKELETAL_HANDS, Skills.LIFE_STEAL);
            case ZEUS, JUPITER -> setSkills(player, Skills.SOAR, Skills.LIGHTNING_TOSS, Skills.STORMS_EMBRACE, Skills.WIND_WALL, Skills.PROTECTIVE_CLOUD);
            case POSEIDON, NEPTUNE -> setSkills(player, Skills.TIDAL_WAVE, Skills.AQUATIC_LIMB_EXTENSIONS, Skills.SEISMIC_BLAST, Skills.OCEANS_SURGE, Skills.OCEANS_EMBRACE);
            case APOLLO_GREEK, APOLLO_ROMAN -> setSkills(player, Skills.APOLLOS_HYMN, Skills.PLAGUE_VOLLEY, Skills.BURNING_LIGHT, Skills.ENHANCED_ARCHERY, Skills.ARROWS_OF_THE_SUN);
            case ARTEMIS, DIANA -> setSkills(player, Skills.NIMBLE_DASH, Skills.CALL_OF_THE_WILD, Skills.SUPER_CHARGED, Skills.SHARPSHOOTER, Skills.HUNTRESS_AGILITY);
            case DIONYSUS, BACCHUS -> setSkills(player,  Skills.VINE_WHIP, Skills.GRAPE_SHOT, Skills.MAD_GODS_DRINK, Skills.FRENZIED_DANCE, Skills.DRUNKEN_REVELRY);
            case ARES, MARS -> setSkills(player, Skills.BLOODLUST, Skills.TOMAHAWK_THROW, Skills.CLEAVE, Skills.PRIMAL_SCREAM, Skills.ARES_BLESSING);
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
        double pvpNerf = 0.35;

        if (livingEntity instanceof Player) {
            livingEntity.damage(damage * pvpNerf, damager);
        } else {
            livingEntity.damage(damage, damager);
        }
    }
}
