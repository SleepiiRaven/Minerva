package net.minervamc.minerva.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused"})
public class Text {

    private static final Pattern pattern = Pattern.compile("&#[a-zA-Z0-9]{6}");

    public static final String BLACK = "black";
    public static final String DARK_BLUE = "dark_blue";
    public static final String DARK_GREEN = "dark_green";
    public static final String DARK_AQUA = "dark_aqua";
    public static final String DARK_RED = "dark_red";
    public static final String DARK_PURPLE = "dark_purple";
    public static final String GOLD = "gold";
    public static final String GRAY = "gray";
    public static final String DARK_GRAY = "dark_gray";
    public static final String BLUE = "blue";
    public static final String GREEN = "green";
    public static final String AQUA = "aqua";
    public static final String RED = "red";
    public static final String LIGHT_PURPLE = "light_purple";
    public static final String YELLOW = "yellow";
    public static final String WHITE = "white";
    public static final String RESET = "reset";

    public static final String CLICK_OPEN_URL = "open_url";
    public static final String CLICK_RUN_COMMAND = "run_command";
    public static final String CLICK_SUGGEST_COMMAND = "suggest_command";
    public static final String CLICK_CHANGE_PAGE = "change_page";

    public static final String HOVER_SHOW_TEXT = "show_text";
    public static final String HOVER_SHOW_ITEM = "show_item";
    public static final String HOVER_SHOW_ENTITY = "show_entity";

    public static String componentToPlainText(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    public static Component format(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    public static Component format(String text, boolean italics) {
        return MiniMessage.miniMessage().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static Component formatLegacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static Component formatLegacy(String text, boolean italics) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static String formatLegacy2(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String stripColorCodes(String input) {
        input = input.replaceAll("(?i)&[0-9A-FK-OR]", "");
        return input.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    /**
     * By Mantice
     * @param input String text
     * @return Component of translated input
     */
    public static @NotNull Component hexAndLegacy(@NotNull String input) {
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            final String hex = input.substring(matcher.start(), matcher.end());
            final String replace = hex.replace("&#", "x");

            final char[] chars = replace.toCharArray();
            final StringBuilder builder = new StringBuilder();
            for (final char c : chars) builder.append("&").append(c);

            input = input.replace(hex, builder.toString());
            matcher = pattern.matcher(input);
        }
        return formatLegacy(input);
    }
}
