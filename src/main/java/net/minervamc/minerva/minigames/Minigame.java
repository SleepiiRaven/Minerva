package net.minervamc.minerva.minigames;

import java.util.List;
import org.bukkit.entity.Player;

public abstract class Minigame {
    public abstract void start(List<Player> players);
    public abstract void end();
}
