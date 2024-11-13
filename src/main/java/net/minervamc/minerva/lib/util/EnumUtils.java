package net.minervamc.minerva.lib.util;

@SuppressWarnings("unused")
public class EnumUtils {
    public static String toHumanReadableString(Enum<?> enumValue) {
        String name = enumValue.name();
        String[] words = name.split("_");
        StringBuilder humanReadableName = new StringBuilder();

        for (String word : words) {
            if (!humanReadableName.isEmpty()) {
                humanReadableName.append(" ");
            }
            humanReadableName.append(word.substring(0, 1).toUpperCase())
                             .append(word.substring(1).toLowerCase());
        }

        return humanReadableName.toString();
    }

    public static <T extends Enum<T>> T findClosestEnum(String input, T[] values) {
        int minDistance = Integer.MAX_VALUE;
        T closestEnum = null;
        for (T value : values) {
            int distance = StringUtils.levenshteinDistance(input, value.name());
            if (distance < minDistance) {
                minDistance = distance;
                closestEnum = value;
            }
        }
        return closestEnum;
    }
}
