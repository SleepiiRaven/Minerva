package net.minervamc.minerva.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minervamc.minerva.PlayerStats;

public class JsonUtils {
    private static final GsonBuilder BUILDER = new GsonBuilder().registerTypeAdapter(PlayerStats.class, new PlayerStatsAdapter())
            .disableHtmlEscaping();
    public static Gson GSON;

    static {
        GSON = BUILDER.setPrettyPrinting().create();
    }
}
