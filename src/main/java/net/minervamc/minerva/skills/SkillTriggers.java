package net.minervamc.minerva.skills;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.SkillType;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SkillTriggers {
    public Minerva plugin = Minerva.getInstance();
    public CooldownManager cooldownManager = plugin.getCdInstance();
    public Player player;
    //region Normal Spell
    public String currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R" + ChatColor.RESET + ChatColor.RED + " _ _";
    public boolean spellMode = false;
    public int clicksSoFar = 0;
    public boolean[] spellClicks = new boolean[2]; //left = false, right = true.
    private BukkitTask inactivityTimer;
    public SkillTriggers(Player player) {
        this.player = player;
    }

    public void enterSpellMode(Player player, boolean swapped) {
        this.player = player;
        player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 1f);
        spellMode = true;

        if (swapped) {
            currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L" + ChatColor.RESET + ChatColor.RED + " _ _";
        }
        inactivityTimer = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(ChatColor.RED + "Spell cancelled due to inactivity.");
                spellMode = false;
                clicksSoFar = 0;
                spellClicks = new boolean[2];
                currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L" + ChatColor.RESET + ChatColor.RED + " _ _";
            }
        }.runTaskLater(plugin, 60L);
        player.sendTitle(" ", currentMessage, 0, 50, 10);
    }

    public void continueNormalSpell(Action clickType, boolean swapped) {
        player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 0.8f);
        boolean click = clickType.equals(Action.RIGHT_CLICK_AIR) || clickType.equals(Action.RIGHT_CLICK_BLOCK);
        if (!swapped) {
            if (clicksSoFar == 0) {
                currentMessage = (click) ? /*IF WE ARE RIGHT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R R" + ChatColor.RESET + ChatColor.RED + " _" :
                        /*IF WE ARE LEFT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R L" + ChatColor.RESET + ChatColor.RED + " _";
            } else {
                // If our first click was a left click
                if (!spellClicks[0]) {
                    currentMessage = (click) ? /*IF WE ARE RIGHT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R L R" :
                            /*IF WE ARE LEFT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R L L";
                } else {
                    currentMessage = (click) ? /*IF WE ARE RIGHT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R R R" :
                            /*IF WE ARE LEFT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R R L";
                }
            }
            if (clicksSoFar < 2) {
                spellClicks[clicksSoFar] = click;
                player.sendTitle(" ", currentMessage, 0, 50, 10);
            }
            clicksSoFar += 1;
            if (clicksSoFar == 2) {
                finishSpell(false);
                spellMode = false;
                clicksSoFar = 0;
                spellClicks = new boolean[2];
                currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R" + ChatColor.RESET + ChatColor.RED + " _ _";
            }
        } else {
            if (clicksSoFar == 0) {
                currentMessage = (click) ? /*IF WE ARE RIGHT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L R" + ChatColor.RESET + ChatColor.RED + " _" :
                        /*IF WE ARE LEFT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L L" + ChatColor.RESET + ChatColor.RED + " _";
            } else {
                // If our first click was a left click
                if (!spellClicks[0]) {
                    currentMessage = (click) ? /*IF WE ARE RIGHT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L L R" :
                            /*IF WE ARE LEFT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L L L";
                } else {
                    currentMessage = (click) ? /*IF WE ARE RIGHT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L R R" :
                            /*IF WE ARE LEFT CLICKING*/ ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L R L";
                }
            }
            if (clicksSoFar < 2) {
                spellClicks[clicksSoFar] = click;
                player.sendTitle(" ", currentMessage, 0, 50, 10);
            }
            clicksSoFar += 1;
            if (clicksSoFar == 2) {
                finishSpell(true);
                spellMode = false;
                clicksSoFar = 0;
                spellClicks = new boolean[2];
                currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "L" + ChatColor.RESET + ChatColor.RED + " _ _";
            }
        }
    }

    public void finishSpell(boolean swapped) {
        inactivityTimer.cancel();
        boolean firstClick = spellClicks[0];
        boolean secondClick = spellClicks[1];

        String firstClickString = (firstClick) ? "R" : "L";
        String secondClickString = (secondClick) ? "R" : "L";

        String prefix = (swapped) ? "L" : "R";

        player.sendTitle(ChatColor.YELLOW + " ", ChatColor.GREEN.toString() + ChatColor.UNDERLINE + prefix + " " + firstClickString + " " + secondClickString, 0, 5, 5);


        SkillType skillType = switch (prefix + firstClickString + secondClickString) {
            case "RLR", "LRL" -> SkillType.RLR;
            case "RLL", "LRR" -> SkillType.RLL;
            case "RRL", "LLR" -> SkillType.RRL;
            default -> SkillType.RRR;
        };

        SkillUtils.redirect(player, player.getUniqueId(), skillType);
    }
    //endregion
}
