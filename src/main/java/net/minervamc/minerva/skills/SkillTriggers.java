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

    public SkillTriggers(Player player) {
        this.player = player;
    }

    //region Normal Spell
    public String currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R" + ChatColor.RESET + ChatColor.RED + " _ _";
    public boolean spellMode = false;
    public int clicksSoFar = 0;
    public boolean[] spellClicks = new boolean[2]; //left = false, right = true.
    private BukkitTask inactivityTimer;

    public void enterSpellMode(Player player) {
        this.player = player;
        player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 1f);
        spellMode = true;
        inactivityTimer = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(ChatColor.RED + "Spell cancelled due to inactivity.");
                spellMode = false;
                clicksSoFar = 0;
                spellClicks = new boolean[2];
                currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R" + ChatColor.RESET + ChatColor.RED + " _ _";
            }
        }.runTaskLater(plugin, 60L);
        player.sendActionBar(currentMessage);
    }

    public void continueNormalSpell(Action clickType) {
        player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 0.8f);
        boolean click = false;
        if (clickType.equals(Action.RIGHT_CLICK_AIR) || clickType.equals(Action.RIGHT_CLICK_BLOCK)) {
            click = true;
        }
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
            player.sendActionBar(currentMessage);
        }
        clicksSoFar += 1;
        if (clicksSoFar == 2) {
            finishSpell();
            spellMode = false;
            clicksSoFar = 0;
            spellClicks = new boolean[2];
            currentMessage = ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "R" + ChatColor.RESET + ChatColor.RED + " _ _";
        }
    }

    public void finishSpell() {
        inactivityTimer.cancel();
        boolean firstClick = spellClicks[0];
        boolean secondClick = spellClicks[1];

        player.sendActionBar(ChatColor.YELLOW + "Casted!");

        // If the notation is LEFT-RIGHT
        if (!firstClick && secondClick) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "RLR")) {
                player.sendMessage(ChatColor.RED + "That ability is currently on cooldown.");
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.5f);
                return;
            }
            SkillUtils.redirect(player, player.getUniqueId(), SkillType.RLR);
        }

        // If the notation is RIGHT-LEFT
        if (firstClick && !secondClick) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "RRL")) {
                player.sendMessage(ChatColor.RED + "That ability is currently on cooldown.");
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.5f);
                return;
            }
            SkillUtils.redirect(player, player.getUniqueId(), SkillType.RRL);
        }

        // If the notation is LEFT-LEFT
        if (!firstClick && !secondClick) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "RLL")) {
                player.sendMessage(ChatColor.RED + "That ability is currently on cooldown.");
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.5f);
                return;
            }
            SkillUtils.redirect(player, player.getUniqueId(), SkillType.RLL);
        }

        // If the notation is RIGHT-RIGHT
        if (firstClick && secondClick) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "RRR")) {
                player.sendMessage(ChatColor.RED + "That ability is currently on cooldown.");
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.5f);
                return;
            }
            SkillUtils.redirect(player, player.getUniqueId(), SkillType.RRR);
        }
    }
    //endregion
}
