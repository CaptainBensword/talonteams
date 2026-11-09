
package com.talon.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeamManager {

    private final TalonTeams plugin;

    public TeamManager(TalonTeams plugin) {
        this.plugin = plugin;
    }

    public Team getTeam(UUID uuid) {
        String name = plugin.getConfig().getString("players." + uuid);
        return Team.fromString(name);
    }

    public Team getTeam(Player player) {
        return getTeam(player.getUniqueId());
    }

    public boolean setTeam(Player player, Team team) {
        if (player == null || team == null) {
            return false;
        }

        plugin.getConfig().set(
            "players." + player.getUniqueId(),
            team.name()
        );

        plugin.saveConfig();
        updatePlayer(player, team);
        return true;
    }

    public void removeTeam(Player player) {
        if (player == null) {
            return;
        }

        plugin.getConfig().set(
            "players." + player.getUniqueId(), null
        );

        plugin.saveConfig();
        updatePlayer(player, null);
    }

    public List<UUID> getMembers(Team team) {
        List<UUID> members = new ArrayList<UUID>();

        if (team == null) {
            return members;
        }

        Set<String> keys = plugin.getConfig()
            .getConfigurationSection("players") == null
            ? null
            : plugin.getConfig()
                .getConfigurationSection("players").getKeys(false);

        if (keys == null) {
            return members;
        }

        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);

                if (getTeam(uuid) == team) {
                    members.add(uuid);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return members;
    }

    public int getStrength(Team team) {
        return getMembers(team).size() * 10;
    }

    public int getOnlineCount(Team team) {
        int count = 0;

        for (UUID uuid : getMembers(team)) {
            if (Bukkit.getPlayer(uuid) != null) {
                count++;
            }
        }

        return count;
    }

    public String getMemberName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();

        return name == null ? uuid.toString() : name;
    }

    public void updatePlayer(Player player) {
        updatePlayer(player, getTeam(player));
    }

    public void updatePlayer(Player player, Team team) {
        if (team == null) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            return;
        }

        String coloredName = team.getColor() + player.getName();

        player.setDisplayName(coloredName);
        player.setPlayerListName(coloredName);
    }

    public void updateOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void setBase(Team team, Location location) {
        if (team == null || location == null
                || location.getWorld() == null) {
            return;
        }

        String path = "bases." + team.name();

        plugin.getConfig().set(path + ".world",
            location.getWorld().getName());
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());

        plugin.saveConfig();
    }

    public TeamBase getBase(Team team) {
        if (team == null) {
            return null;
        }

        String path = "bases." + team.name();
        String worldName = plugin.getConfig()
            .getString(path + ".world");

        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return null;
        }

        Location location = new Location(
            world,
            plugin.getConfig().getDouble(path + ".x"),
            plugin.getConfig().getDouble(path + ".y"),
            plugin.getConfig().getDouble(path + ".z"),
            (float) plugin.getConfig().getDouble(path + ".yaw"),
            (float) plugin.getConfig().getDouble(path + ".pitch")
        );

        boolean breached = plugin.getConfig()
            .getBoolean(path + ".breached", false);

        return new TeamBase(location, breached);
    }

    public void setBreached(Team team, boolean breached) {
        if (team == null) {
            return;
        }

        plugin.getConfig().set(
            "bases." + team.name() + ".breached",
            breached
        );

        plugin.saveConfig();
    }

    public boolean isBreached(Team team) {
        TeamBase base = getBase(team);
        return base != null && base.isBreached();
    }
}
