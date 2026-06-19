package net.minervamc.minerva.skills;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.SkillType;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Q-driven spell casting.
 *
 * <p>Press Q (drop) → {@link #enterSpellMode}. Then TWO left/right clicks form the combo
 * (left = L, right = R). The 4 two-click combos map to the 4 active slots (R-prefix is implied):
 * RR→RRR, RL→RRL, LR→RLR, LL→RLL. Casts then auto-exits. Q again exits. Inactivity cancels after 3 s.</p>
 */
public class SkillTriggers {
    public Minerva plugin = Minerva.getInstance();
    public CooldownManager cooldownManager = plugin.getCdInstance();
    public Player player;

    public boolean spellMode = false;
    public boolean ignoreNextSwing = false;
    private final boolean[] clicks = new boolean[2]; // false = left (L), true = right (R)
    private int clicksSoFar = 0;
    private BukkitTask inactivityTimer;

    public SkillTriggers(Player player) {
        this.player = player;
    }

    public void enterSpellMode(Player player) {
        this.player = player;
        spellMode = true;
        clicksSoFar = 0;
        // Pressing Q (drop) also fires one arm-swing on the same tick, AFTER this drop event.
        // Flag it so that one drop-swing is swallowed instead of being read as the first "L".
        ignoreNextSwing = true;
        player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 1f);
        restartInactivityTimer();
        showHud();
    }

    /** Register one combo click. left-click → L (false), right-click → R (true). */
    public void click(boolean right) {
        if (!spellMode) return;
        player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.5f, 0.8f);
        if (clicksSoFar < 2) {
            clicks[clicksSoFar] = right;
            clicksSoFar++;
        }
        if (clicksSoFar >= 2) {
            finishSpell();
        } else {
            restartInactivityTimer();
            showHud();
        }
    }

    /** Exit spell mode without casting (Q-again / external cancel). */
    public void cancelSpellMode() {
        if (inactivityTimer != null) inactivityTimer.cancel();
        spellMode = false;
        clicksSoFar = 0;
        if (player != null) {
            player.resetTitle();
            player.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.4f, 0.6f);
        }
    }

    /** The Q-drop itself fires one arm-swing on the same tick; swallow exactly that one so it is not read as "L". */
    public boolean consumeEntrySwing() {
        if (ignoreNextSwing) {
            ignoreNextSwing = false;
            return true;
        }
        return false;
    }

    /** True while the player is mid spell-cast (Q pressed, combo not yet completed). */
    public static boolean isCasting(Player player) {
        if (player == null) return false;
        net.minervamc.minerva.PlayerStats stats = net.minervamc.minerva.PlayerStats.getStats(player.getUniqueId());
        return stats != null && stats.skillMode && stats.skillTriggers != null && stats.skillTriggers.spellMode;
    }

    private void finishSpell() {
        if (inactivityTimer != null) inactivityTimer.cancel();
        spellMode = false;
        clicksSoFar = 0;

        player.sendTitle(" ", ChatColor.GREEN.toString() + ChatColor.UNDERLINE
            + letter(clicks[0]) + " " + letter(clicks[1]), 0, 5, 5);

        // R-prefix implied (swapped = false); the two clicks pick the slot.
        SkillType skillType = SkillType.fromClicks(false, clicks[0], clicks[1]);
        SkillUtils.redirect(player, player.getUniqueId(), skillType);
    }

    private String letter(boolean right) {
        return right ? "R" : "L";
    }

    private void showHud() {
        StringBuilder sb = new StringBuilder(ChatColor.GREEN.toString()).append(ChatColor.UNDERLINE);
        for (int i = 0; i < 2; i++) {
            if (i < clicksSoFar) sb.append(letter(clicks[i]));
            else sb.append(ChatColor.RESET).append(ChatColor.RED).append("_");
            if (i < 1) sb.append(' ');
        }
        player.sendTitle(" ", sb.toString(), 0, 50, 10);
    }

    private void restartInactivityTimer() {
        if (inactivityTimer != null) inactivityTimer.cancel();
        inactivityTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!spellMode) return;
                player.sendMessage(ChatColor.RED + "Spell cancelled due to inactivity.");
                player.resetTitle();
                spellMode = false;
                clicksSoFar = 0;
            }
        }.runTaskLater(plugin, 60L);
    }
}
