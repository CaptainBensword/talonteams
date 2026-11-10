
package com.talon.teams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
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

    public void setTeam(Player player, Team team) {
        if (team == null) {
            removeTeam(player);
            return;
        }

        plugin.getConfig().set("players." + player.getUniqueId(), team.name());
        plugin.saveConfig();
        updatePlayer(player);
    }

    public void removeTeam(Player player) {
        plugin.getConfig().set("players." + player.getUniqueId(), null);
        plugin.saveConfig();
        updatePlayer(player);
    }

    public List<UUID> getMembers(Team team) {
        List<UUID> members = new ArrayList<UUID>();

        if (team == null || !plugin.getConfig().isConfigurationSection("players")) {
            return members;
        }

        Set<String> keys = plugin.getConfig()
                .getConfigurationSection("players").getKeys(false);

        for (String key : keys) {
            String teamName = plugin.getConfig().getString("players." + key);

            if (team.name().equalsIgnoreCase(teamName)) {
                try {
                    members.add(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUID entries.
                }
            }
        }

        return members;
    }

    // -------------------------
    // TROPHIES
    // -------------------------

    public int getTrophies(UUID uuid) {
        return Math.max(0, plugin.getConfig()
                .getInt("trophies." + uuid, 0));
    }

    public void setTrophies(UUID uuid, int amount) {
        if (uuid == null) {
            return;
        }

        plugin.getConfig().set("trophies." + uuid, Math.max(0, amount));
        plugin.saveConfig();
    }

    public void addTrophies(UUID uuid, int amount) {
        if (uuid == null || amount <= 0) {
            return;
        }

        long total = (long) getTrophies(uuid) + amount;
        setTrophies(uuid, (int) Math.min(total, Integer.MAX_VALUE));
    }

    public int getTeamTrophies(Team team) {
        int total = 0;

        for (UUID uuid : getMembers(team)) {
            total += getTrophies(uuid);
        }

        return total;
    }

    public List<UUID> getTrophyLeaderboard() {
        List<UUID> players = new ArrayList<UUID>();

        if (!plugin.getConfig().isConfigurationSection("trophies")) {
            return players;
        }

        Set<String> keys = plugin.getConfig()
                .getConfigurationSection("trophies").getKeys(false);

        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);

                if (getTrophies(uuid) > 0) {
                    players.add(uuid);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid UUID entries.
            }
        }

        Collections.sort(players, new Comparator<UUID>() {
            @Override
            public int compare(UUID first, UUID second) {
                return Integer.compare(
                        getTrophies(second), getTrophies(first));
            }
        });

        return players;
    }

    // -------------------------
    // TEAM STRENGTH
    // -------------------------

    public int getStrength(Team team) {
        return getMembers(team).size() * 10
                + getTeamTrophies(team) * 5;
    }

    public int getOnlineCount(Team team) {
        int online = 0;

        for (UUID uuid : getMembers(team)) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                online++;
            }
        }

        return online;
    }

    public String getMemberName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();

        if (name == null) {
            return "Unknown Player";
        }

        return name;
    }

    // -------------------------
    // BASES
    // -------------------------

    public void setBase(Team team, Location location) {
        if (team == null || location == null || location.getWorld() == null) {
            return;
        }

        String path = "bases." + team.name();

        plugin.getConfig().set(path + ".world", location.getWorld().getName());
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

        if (!plugin.getConfig().contains(path + ".world")) {
            return null;
        }

        String worldName = plugin.getConfig().getString(path + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return null;
        }

        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getConfig().getDouble(path + ".yaw");
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch");

        Location location = new Location(world, x, y, z, yaw, pitch);
        boolean breached = plugin.getConfig()
                .getBoolean(path + ".breached", false);

        return new TeamBase(location, breached);
    }

    public void setBreached(Team team, boolean breached) {
        if (team == null) {
            return;
        }

        plugin.getConfig().set(
                "bases." + team.name() + ".breached", breached);
        plugin.saveConfig();
    }

    public boolean isBreached(Team team) {
        TeamBase base = getBase(team);
        return base != null && base.isBreached();
    }

    // -------------------------
    // PLAYER DISPLAY
    // -------------------------

    public void updatePlayer(Player player) {
        Team team = getTeam(player.getUniqueId());
        plugin.getShieldManager().ensureTeamShield(player, team);

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
}
