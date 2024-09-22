package net.minervamc.minerva.lib.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused"})
public class TextContext {

    private final StringBuilder builder;

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

    public TextContext() {
        builder = new StringBuilder();
    }

    public static TextContext get() {
        return new TextContext();
    }

    public TextContext text(String txt, TextDecoration... decorations) {
        builder.append(parse(txt, decorations));
        return this;
    }

    public TextContext black(String txt, TextDecoration... decorations) {
        return colored(BLACK, txt, decorations);
    }

    public TextContext darkBlue(String txt, TextDecoration... decorations) {
        return colored(DARK_BLUE, txt, decorations);
    }

    public TextContext darkGreen(String txt, TextDecoration... decorations) {
        return colored(DARK_GREEN, txt, decorations);
    }

    public TextContext darkAqua(String txt, TextDecoration... decorations) {
        return colored(DARK_AQUA, txt, decorations);
    }

    public TextContext darkRed(String txt, TextDecoration... decorations) {
        return colored(DARK_RED, txt, decorations);
    }

    public TextContext darkPurple(String txt, TextDecoration... decorations) {
        return colored(DARK_PURPLE, txt, decorations);
    }

    public TextContext gold(String txt, TextDecoration... decorations) {
        return colored(GOLD, txt, decorations);
    }

    public TextContext gray(String txt, TextDecoration... decorations) {
        return colored(GRAY, txt, decorations);
    }

    public TextContext darkGray(String txt, TextDecoration... decorations) {
        return colored(DARK_GRAY, txt, decorations);
    }

    public TextContext blue(String txt, TextDecoration... decorations) {
        return colored(BLUE, txt, decorations);
    }

    public TextContext green(String txt, TextDecoration... decorations) {
        return colored(GREEN, txt, decorations);
    }

    public TextContext aqua(String txt, TextDecoration... decorations) {
        return colored(AQUA, txt, decorations);
    }

    public TextContext red(String txt, TextDecoration... decorations) {
        return colored(RED, txt, decorations);
    }

    public TextContext light_purple(String txt, TextDecoration... decorations) {
        return colored(LIGHT_PURPLE, txt, decorations);
    }

    public TextContext yellow(String txt, TextDecoration... decorations) {
        return colored(YELLOW, txt, decorations);
    }

    public TextContext white(String txt, TextDecoration... decorations) {
        return colored(WHITE, txt, decorations);
    }

    public TextContext reset(String txt, TextDecoration... decorations) {
        return colored(RESET, txt, decorations);
    }

    public TextContext clickOpenUrl(String url, String txt) {
        return click(CLICK_OPEN_URL, url, txt);
    }

    public TextContext clickRunCommand(String command, String txt) {
        return click(CLICK_RUN_COMMAND, command, txt);
    }

    public TextContext clickSuggestCommand(String command, String txt) {
        return click(CLICK_SUGGEST_COMMAND, command, txt);
    }

    public TextContext clickChangePage(String value, String txt) {
        return click(CLICK_CHANGE_PAGE, value, txt);
    }

    public TextContext clickOpenUrl(String url, String color, String txt) {
        return click(CLICK_OPEN_URL, url, color, txt);
    }

    public TextContext clickRunCommand(String command, String color, String txt) {
        return click(CLICK_RUN_COMMAND, command, color, txt);
    }

    public TextContext clickSuggestCommand(String command, String color, String txt) {
        return click(CLICK_SUGGEST_COMMAND, command, color, txt);
    }

    public TextContext clickChangePage(String value, String color, String txt) {
        return click(CLICK_CHANGE_PAGE, value, color, txt);
    }

    public TextContext colored(String color, String txt, TextDecoration... decorations) {
        String coloredTxt = "<" + color + ">" + txt + "</" + color + ">";
        builder.append(parse(coloredTxt, decorations));
        return this;
    }

    public TextContext rainbow(String txt, float phase, TextDecoration... decorations) {
        String rainbowTxt = "<rainbow" + (phase != 0 ? ":" + phase : "") + ">" + txt + "</rainbow>";
        builder.append(parse(rainbowTxt, decorations));
        return this;
    }

    public TextContext gradient(String txt, List<String> colors, float phase, TextDecoration... decorations) {
        StringBuilder gradient = new StringBuilder("<gradient:");
        for (String color : colors) {
            gradient.append(color).append(":");
        }
        gradient.deleteCharAt(gradient.length() - 1).append(phase != 0 ? ":" + phase : "").append(">").append(txt).append("</gradient>");
        builder.append(parse(gradient.toString(), decorations));
        return this;
    }

    public TextContext transition(String txt, List<String> colors, float phase, TextDecoration... decorations) {
        StringBuilder transition = new StringBuilder("<transition:");
        for (String color : colors) {
            transition.append(color).append(":");
        }
        transition.deleteCharAt(transition.length() - 1).append(phase != 0 ? ":" + phase : "").append(">").append(txt).append("</transition>");
        builder.append(parse(transition.toString(), decorations));
        return this;
    }

    public TextContext rainbow(String txt, TextDecoration... decorations) {
        String rainbowTxt = "<rainbow>" + txt + "</rainbow>";
        builder.append(parse(rainbowTxt, decorations));
        return this;
    }

    public TextContext gradient(String txt, List<String> colors, TextDecoration... decorations) {
        StringBuilder gradient = new StringBuilder("<gradient:");
        for (String color : colors) {
            gradient.append(color).append(":");
        }
        gradient.deleteCharAt(gradient.length() - 1).append(">").append(txt).append("</gradient>");
        builder.append(parse(gradient.toString(), decorations));
        return this;
    }

    public TextContext transition(String txt, List<String> colors, TextDecoration... decorations) {
        StringBuilder transition = new StringBuilder("<transition:");
        for (String color : colors) {
            transition.append(color).append(":");
        }
        transition.deleteCharAt(transition.length() - 1).append(">").append(txt).append("</transition>");
        builder.append(parse(transition.toString(), decorations));
        return this;
    }

    public TextContext click(String action, String actionValue, String txt, TextDecoration... decorations) {
        String clickTxt = "<click:" + action + ":" + actionValue + ">" + txt + "</click>";
        builder.append(parse(clickTxt, decorations));
        return this;
    }

    public TextContext click(String action, String actionValue, String color, String txt, TextDecoration... decorations) {
        String clickTxt = "<click:" + action + ":" + actionValue + ">" + txt + "</click>";
        clickTxt = parse(clickTxt, decorations);
        return colored(color, clickTxt);
    }

    public TextContext hover(String action, String actionValue, String txt, TextDecoration... decorations) {
        String hoverTxt = "<hover:" + action + ":" + actionValue + ">" + txt + "</hover>";
        builder.append(parse(hoverTxt, decorations));
        return this;
    }

    public TextContext font(String font, String txt, TextDecoration... decorations) {
        String fontTxt = "<font:" + font + ">" + txt + "</font>";
        builder.append(parse(fontTxt, decorations));
        return this;
    }

    public TextContext translatable(String key, TextDecoration... decorations) {
        String translatableTxt = "<lang:" + key + ">";
        builder.append(parse(translatableTxt, decorations));
        return this;
    }

    public TextContext keybind(String key, TextDecoration... decorations) {
        String keybindTxt = "<key:" + key + ">";
        builder.append(parse(keybindTxt, decorations));
        return this;
    }

    public Component build() {
        return MiniMessage.miniMessage().deserialize(builder.toString());
    }

    public Component build(boolean italics) {
        return MiniMessage.miniMessage().deserialize(builder.toString()).decoration(TextDecoration.ITALIC, italics);
    }

    public String buildAsString() {
        return builder.toString();
    }

    private String parse(String txt, TextDecoration... decorations) {
        for (TextDecoration decoration : decorations) {
            txt = applyDecoration(decoration, txt);
        }
        return txt;
    }

    private String applyDecoration(TextDecoration decoration, String txt) {
        return switch (decoration) {
            case BOLD -> "<bold>" + txt + "</bold>";
            case ITALIC -> "<italic>" + txt + "</italic>";
            case UNDERLINED -> "<underlined>" + txt + "</underlined>";
            case STRIKETHROUGH -> "<strikethrough>" + txt + "</strikethrough>";
            case OBFUSCATED -> "<obfuscated>" + txt + "</obfuscated>";
        };
    }

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
