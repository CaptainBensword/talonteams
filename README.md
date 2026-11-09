# TalonTeams

A lightweight team picker and colored team chat plugin for the Talon Minecraft server.

## Current features

- `/team` opens a team selection menu.
- Four main teams: Red, Blue, Green, and Yellow.
- **Vigilante** is an independent group, not a fifth team.
- Team choices are saved by player UUID in `config.yml`.
- Chat shows a colored team label and a player name in that team's color.
- Display names and tab-list names are colored to match the selected group.
- `/team info` shows your current group.
- `/team help` lists commands.
- Staff can use `/team set <player> <team>` with `talonteams.admin`.
- Team switching can be disabled in the config.

## Compatibility

- Minecraft server: Paper/Spigot 1.12.2
- Java: 8
- Designed to coexist with LuckPerms. It does not create, replace, or modify LuckPerms groups.

The plugin does not yet implement territory, flags, friendly-fire rules, or team leaders. Those belong to a later development stage.

## Build

Run:

```bash
mvn clean package
```

The output JAR is:

```text
target/TalonTeams.jar
```

GitHub Actions also builds the JAR automatically after a push. Download it from the workflow run's **Artifacts** section.

## Install

1. Stop the server.
2. Put `TalonTeams.jar` in the server's `plugins` folder.
3. Start the server.
4. Run `/team` in-game.

The plugin creates `plugins/TalonTeams/config.yml` on first startup.
