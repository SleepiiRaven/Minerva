package net.minervamc.minerva.utils;

import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.types.SkillType;
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
        switch (heritageType) {
            case HADES -> setSkills(player, Skills.SHADOW_TRAVEL, Skills.UMBRAKINESIS_HADES, Skills.CHANNELING_OF_TARTARUS, Skills.SKELETAL_HANDS, Skills.LIFE_STEAL);
            case ZEUS -> setSkills(player, Skills.SOAR, Skills.LIGHTNING_TOSS, Skills.STORMS_EMBRACE, Skills.WIND_WALL, Skills.PROTECTIVE_CLOUD);
            case POSEIDON -> setSkills(player, Skills.TIDAL_WAVE, Skills.AQUATIC_LIMB_EXTENSIONS, Skills.SEISMIC_BLAST, Skills.OCEANS_SURGE, Skills.OCEANS_EMBRACE);
            case APOLLO_GREEK, APOLLO_ROMAN -> setSkills(player, Skills.APOLLOS_HYMN, Skills.PLAGUE_VOLLEY, Skills.BURNING_LIGHT, Skills.ENHANCED_ARCHERY, Skills.ARROWS_OF_THE_SUN);
            case ARTEMIS -> setSkills(player, Skills.CALL_OF_THE_WILD, Skills.QUICK_DRAW, Skills.SUPER_CHARGED, Skills.SHARPSHOOTER, Skills.HUNTRESS_AGILITY);
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
}
