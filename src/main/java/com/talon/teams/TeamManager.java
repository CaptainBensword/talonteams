package com.talon.teams;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TeamManager {

    private final TalonTeams plugin;

    public TeamManager(TalonTeams plugin) {
        this.plugin = plugin;
    }

    public Team getTeam(UUID uuid) {
        String teamName = plugin.getConfig().getString("players." + uuid);

        if (teamName == null) {
            return null;
        }

        return Team.fromString(teamName);
    }

    public Team getTeam(Player player) {
        return getTeam(player.getUniqueId());
    }

    public boolean setTeam(Player player, Team team) {
        if (team == null) {
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
}
