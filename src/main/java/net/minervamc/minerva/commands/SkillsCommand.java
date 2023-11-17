package net.minervamc.minerva.commands;

import java.util.Arrays;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            if (commandSender instanceof Player player) {
                AncestryGUI.openGUI(player);
            } else {
                commandSender.sendMessage(ChatColor.RED + "Invalid command for non-player. Correct usage to open GUI for player that isn't you: /mskills [gui] [player]");
            }
            return true;
        }
        if (args[0].equals("default")) {
            if (args.length > 2 && Bukkit.getPlayer(args[2]) != null) {
                commandSender.sendMessage("Currently working on this command.");
            } else if (commandSender instanceof Player player) {
                switch (args[1].toLowerCase()) {
                    case "hades" -> setSkills(player, Skills.SHADOW_TRAVEL, Skills.UMBRAKINESIS_HADES, Skills.CHANNELING_OF_TARTARUS, Skills.SKELETAL_HANDS, Skills.LIFE_STEAL, commandSender, "Hades");
                    case "zeus" -> setSkills(player, Skills.SOAR, Skills.LIGHTNING_TOSS, Skills.STORMS_EMBRACE, Skills.WIND_WALL, Skills.PROTECTIVE_CLOUD, commandSender, "Zeus");
                    case "poseidon" -> setSkills(player, Skills.TIDAL_WAVE, Skills.AQUATIC_LIMB_EXTENSIONS, Skills.SEISMIC_BLAST, Skills.OCEANS_SURGE, Skills.OCEANS_EMBRACE, commandSender, "Poseidon");
                }
            }
        }
        return true;
    }

    private void setSkills(Player player, Skill rrr, Skill rlr, Skill rll, Skill rrl, Skill passive, CommandSender commandSender, String name) {
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        playerStats.setRRRLevel(1);
        playerStats.setRLRLevel(1);
        playerStats.setRLLLevel(1);
        playerStats.setRRLLevel(1);
        playerStats.setPassiveLevel(1);
        playerStats.setRRRActive(true);
        playerStats.setRLRActive(true);
        playerStats.setRLLActive(true);
        playerStats.setRRLActive(true);
        playerStats.setPassiveActive(true);
        playerStats.setPoints(playerStats.getMaxPoints());
        playerStats.setSkillRRR(rrr);
        playerStats.setSkillRLR(rlr);
        playerStats.setSkillRLL(rll);
        playerStats.setSkillRRL(rrl);
        playerStats.setPassive(passive);
        playerStats.save();
    }
}
