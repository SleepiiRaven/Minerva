package net.minervamc.minerva.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class Board {
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, String> lines = new HashMap<>();

    public Board(Component title) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, title, RenderType.INTEGER);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public Board updateTitle(Component title) {
        this.objective.displayName(title);
        return this;
    }

    public Board updateLines(String... newLines) {
        clearLines();
        for (int i = 0; i < newLines.length; i++) {
            String line = newLines[i];
            this.objective.getScore(line).setScore(newLines.length - i);
            lines.put(i, line);
        }
        return this;
    }

    public void clearLines() {
        for (String line : lines.values()) {
            scoreboard.resetScores(line);
        }
        lines.clear();
    }

    public void send(Player player) {
        player.setScoreboard(scoreboard);
    }

    public void remove(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
