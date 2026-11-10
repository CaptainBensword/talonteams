
package com.talon.teams;

import org.bukkit.ChatColor;

public enum Team {
    RED(ChatColor.RED, "Red"),
    BLUE(ChatColor.BLUE, "Blue"),
    GREEN(ChatColor.GREEN, "Green"),
    YELLOW(ChatColor.YELLOW, "Yellow");

    private final ChatColor color;
    private final String displayName;

    Team(ChatColor color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
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
