package com.talon.teams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeamManager {
    private final TalonTeams plugin;

    // Async chat events read this cache instead of Bukkit's YAML config.
    private final Map<UUID, Team> teamCache =
            new ConcurrentHashMap<UUID, Team>();

    public TeamManager(TalonTeams plugin) {
        this.plugin = plugin;
        loadTeamCache();
    }

    private void loadTeamCache() {
        if (!plugin.getConfig().isConfigurationSection("players")) {
            return;
        }

        Set<String> keys = plugin.getConfig()
                .getConfigurationSection("players").getKeys(false);

        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);
                Team team = Team.fromString(
                        plugin.getConfig().getString("players." + key));
                if (team != null) {
                    teamCache.put(uuid, team);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed player UUIDs.
            }
        }
    }

    public Team getTeam(UUID uuid) {
        return uuid == null ? null : teamCache.get(uuid);
    }

    public void setTeam(Player player, Team team) {
        if (team == null) {
            removeTeam(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        plugin.getConfig().set("players." + uuid, team.name());
        plugin.saveConfig();
        teamCache.put(uuid, team);
        updatePlayer(player);
    }

    public void removeTeam(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getConfig().set("players." + uuid, null);
        plugin.saveConfig();
        teamCache.remove(uuid);
        updatePlayer(player);
    }

    public List<UUID> getMembers(Team team) {
        List<UUID> members = new ArrayList<UUID>();
        if (team == null) {
            return members;
        }

        for (Map.Entry<UUID, Team> entry : teamCache.entrySet()) {
            if (entry.getValue() == team) {
                members.add(entry.getKey());
            }
        }

        Collections.sort(members, new Comparator<UUID>() {
            @Override
            public int compare(UUID first, UUID second) {
                return first.toString().compareTo(second.toString());
            }
        });
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

    public long getTeamTrophies(Team team) {
        long total = 0L;
        for (UUID uuid : getMembers(team)) {
            long value = getTrophies(uuid);
            if (Long.MAX_VALUE - total < value) {
                return Long.MAX_VALUE;
            }
            total += value;
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

    public long getStrength(Team team) {
        long memberStrength = getMembers(team).size() * 10L;
        long trophies = getTeamTrophies(team);
        if (trophies > (Long.MAX_VALUE - memberStrength) / 5L) {
            return Long.MAX_VALUE;
        }
        return memberStrength + trophies * 5L;
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
        return name == null ? "Unknown Player" : name;
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

    public void clearBase(Team team) {
        if (team == null) {
            return;
        }
        plugin.getConfig().set("bases." + team.name(), null);
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
        if (team == null || getBase(team) == null) {
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
        plugin.getShieldManager().updateTeamShield(player, team);

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
