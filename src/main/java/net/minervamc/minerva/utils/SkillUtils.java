package net.minervamc.minerva.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.HeritageType;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.types.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    public static boolean isFocus(ItemStack itemStack) {
        if (itemStack.getItemMeta() == null || itemStack.getItemMeta().lore() == null) return false;

        if (itemStack.getItemMeta().getLore().contains(ChatColor.LIGHT_PURPLE + "Focused - Start Casting Skills by Right Clicking")) {
            return true;
        }

        return false;
    }

    public static void setFocus(ItemStack itemStack) {
        List<String> lores = new ArrayList<>();

        if (itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta.hasLore()) {
                assert itemStack.getItemMeta().getLore() != null;
                lores.addAll(itemStack.getItemMeta().getLore());
            }
        }

        lores.add(ChatColor.LIGHT_PURPLE + "Focused - Start Casting Skills by Right Clicking");

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(lores);
        itemStack.setItemMeta(itemMeta);
    }

    public static void removeFocus(ItemStack itemStack) {
        if (!isFocus(itemStack)) return;
        ItemMeta meta = itemStack.getItemMeta();
        List<String> lore = meta.getLore();
        lore.remove(ChatColor.LIGHT_PURPLE + "Focused - Start Casting Skills by Right Clicking");
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
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
}
