package net.minervamc.minerva.types;

public enum SkillType {
    RRR("R-R"),
    RLL("L-L"),
    RLR("L-R"),
    RRL("R-L"),
    PASSIVE("Passive");

    private final String displayCombo;

    SkillType(String displayCombo) {
        this.displayCombo = displayCombo;
    }

    public String getDisplayCombo() {
        return displayCombo;
    }

    public static SkillType fromClicks(boolean swapped, boolean firstClickRight, boolean secondClickRight) {
        String prefix = swapped ? "L" : "R";
        String firstClick = firstClickRight ? "R" : "L";
        String secondClick = secondClickRight ? "R" : "L";

        return switch (prefix + firstClick + secondClick) {
            case "RLR", "LRL" -> RLR;
            case "RLL", "LRR" -> RLL;
            case "RRL", "LLR" -> RRL;
            default -> RRR;
        };
    }
}
