package net.minervamc.minerva.minigames.ctf;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.minigames.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CaptureTheFlag extends Minigame {

    @Override
    public void start(List<Player> players) {

        MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setServerName(database.getHost());
        dataSource.setPortNumber(database.getPort());
        dataSource.setDatabaseName(database.getDatabase());
        dataSource.setUser(database.getUser());
        dataSource.setPassword(database.getPassword());
        dataSource.getConnection();
        for (Player player : players) {
            // Save inventories
        }
    }

    @Override
    public void end() {

    }
}
