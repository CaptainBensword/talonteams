package com.talon.teams;

import org.bukkit.ChatColor;

public enum Team {

    RED("Red", ChatColor.RED),
    BLUE("Blue", ChatColor.BLUE),
    GREEN("Green", ChatColor.GREEN),
    YELLOW("Yellow", ChatColor.YELLOW);

    private final String displayName;
    private final ChatColor color;

    Team(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getColoredName() {
        return color + displayName;
    }

    public static Team fromString(String name) {
        if (name == null) {
            return null;
        }

        for (Team team : values()) {
            if (team.name().equalsIgnoreCase(name)
                    || team.displayName.equalsIgnoreCase(name)) {
                return team;
            }
        }

        return null;
    }
}
