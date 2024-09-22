package net.minervamc.minerva.lib.util;

import java.util.List;

public class StringUtils {

    public static String findClosestString(String input, List<String> values) {
        int minDistance = Integer.MAX_VALUE;
        String closestString = null;
        for (String value : values) {
            int distance = levenshteinDistance(input, value);
            if (distance < minDistance) {
                minDistance = distance;
                closestString = value;
            }
        }
        return closestString;
    }

    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else dp[i][j] = Math.min(dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1), Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
            }
        }
        return dp[a.length()][b.length()];
    }
}
