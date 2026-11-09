
package com.talon.teams;

import org.bukkit.Location;

public class TeamBase {

    private final Location location;
    private final boolean breached;

    public TeamBase(Location location, boolean breached) {
        this.location = location;
        this.breached = breached;
    }

    public Location getLocation() {
        return location.clone();
    }

    public boolean isBreached() {
        return breached;
    }
}
