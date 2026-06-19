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
        if (player.hasMetadata("NPC")) return;
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
        if (player.hasMetadata("NPC")) return;
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
                    setSkills(player, Skills.SPIRIT_OF_VENGEANCE, Skills.TOMAHAWK_THROW, Skills.CLEAVE, Skills.PRIMAL_SCREAM, Skills.ARES_BLESSING);
            case HEPHAESTUS, VULCAN ->
                    setSkills(player, Skills.SHRAPNEL_GRENADE, Skills.MAGMATISM, Skills.LIVING_FORGE, Skills.GROUND_BREAKER, Skills.SMOLDER);
            case APHRODITE, VENUS ->
                    setSkills(player, Skills.MIRROR_IMAGE, Skills.CHARM, Skills.HEART_SEEKER, Skills.SERENITY, Skills.DOVES);
            case ATHENA, MINERVA ->
                    setSkills(player, Skills.PARRY, Skills.SPEAR_OF_ATHENA, Skills.AEGIS_RUSH, Skills.SPEAR_RAIN, Skills.TACTICAL_AGILITY);
            case DEMETER, CERES ->
                    setSkills(player, Skills.VINE_GRAPPLE, Skills.SEED_BARRAGE, Skills.THORN_BLIGHT, Skills.HARVEST_WRATH, Skills.HARVEST_BLESSING);
            case HERMES, MERCURY ->
                    setSkills(player, Skills.TALARIA_STEP, Skills.CADUCEUS_ARC, Skills.MESSENGER_WAKE, Skills.KINETIC_DISPATCH, Skills.FLEET_FOOTWORK);
            case KHIONE, CHIONE ->
                    setSkills(player, Skills.GLACIAL_GLIDE, Skills.RIME_NOVA, Skills.SHATTERPOINT, Skills.COLD_SNAP, Skills.FROSTBITE);
            case BELLONA ->
                    setSkills(player, Skills.ADVANCE_THE_LINE, Skills.PLANT_WAR_BANNER, Skills.CANNONADE, Skills.STANDARDS_CALL, Skills.WAR_FOOTING);
            case HESTIA, VESTA ->
                    setSkills(player, Skills.EMBER_VEIL, Skills.TEND_THE_HEARTH, Skills.PYRE_RELEASE, Skills.VESTAS_VEIL, Skills.BANKED_EMBERS);
            case HECATE ->
                    setSkills(player, Skills.SHIFT_FACE, Skills.CROSSROADS_TORCHES, Skills.SPECTRAL_HEX, Skills.WITCHING_HOUR, Skills.TRIPLE_GODDESS);
            case PSYCHE_GREEK, PSYCHE_ROMAN ->
                    setSkills(player, Skills.RELEASE_ANIMA, Skills.SOUL_THREAD, Skills.SOUL_LANCE, Skills.CHRYSALIS, Skills.IRIDESCENT_SOUL);
            case JANUS ->
                    setSkills(player, Skills.DOORWAY, Skills.REVERSAL, Skills.TWO_FACED_STRIKE, Skills.TRESPASS, Skills.GOD_OF_TRANSITIONS);
            case IRIS, ARCUS ->
                    setSkills(player, Skills.IRIS_FLIGHT, Skills.SPECTRUM_SHIFT, Skills.REFRACTION_LANCE, Skills.CHROMATIC_BURST, Skills.PRISM);
            case ARKE, ARCE ->
                    setSkills(player, Skills.REEL, Skills.CAST_CHAINS, Skills.TARNISHED_LASH, Skills.CINCH_THE_CHAINS, Skills.SHARED_FATE);
            case HYPNOS, SOMNUS ->
                    setSkills(player, Skills.DREAMDRIFT, Skills.LULLABY, Skills.NIGHTMARE, Skills.VEIL_OF_SOMNUS, Skills.SANDMAN);
            case NEMESIS ->
                    setSkills(player, Skills.BRACE, Skills.SCALES_OF_BALANCE, Skills.COLLECT_THE_DEBT, Skills.MARK_OF_HUBRIS, Skills.LEDGER_OF_WRONGS);
            case PERSEPHONE ->
                    setSkills(player, Skills.DESCENT, Skills.BLOOM, Skills.WITHER, Skills.QUEENS_DECREE, Skills.SEEDS_OF_THE_UNDERWORLD);
            case THANATOS ->
                    setSkills(player, Skills.SHROUD_OF_LETUS, Skills.KNELL, Skills.SCYTHE_OF_LETUS, Skills.TOLL_THE_BELL, Skills.THE_INEVITABLE);
        }
    }

    private static void setSkills(Player player, Skill rrr, Skill rlr, Skill rll, Skill rrl, Skill passive) {
        if (player.hasMetadata("NPC")) return;
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
