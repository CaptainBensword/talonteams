# TalonTeams

TalonTeams is a team-management plugin for Paper/Spigot 1.12.2 running on Java 8. It provides four teams, a team menu, team-colored player names and chat, team bases, trophies, and team-issued shields.

## Current features

### Teams and players
- Four teams: Red, Blue, Green, and Yellow.
- /team opens the team-selection menu.
- Team assignments are saved by player UUID, so names can change without losing membership.
- /team info shows your team, member counts, team trophies, strength, and base status.
- /team leave removes your team assignment.
- /team set <player> <team> lets an administrator assign an online player.
- Team colors are used for player display names, tab-list names, and the team chat prefix.
- Team membership is cached safely for asynchronous chat formatting.

### Bases
- Administrators set a team's base with /team base set <team>.
- Team members can view their own base with /team base and travel there with /team warp.
- Administrators can remove a base with /team base clear <team>.
- Administrators set the displayed status with /team breach <team> <on|off>.
- The public team menu shows whether a base exists and its status, but does not reveal its coordinates.

### Trophies and strength
- Players can view their trophies with /team trophies.
- /team trophies top shows the top ten trophy holders.
- Administrators can use /team trophies give <player> <amount>, /team trophies set <player> <amount>, and /team trophies reset <player>.
- When one player kills another, the victim's trophies transfer to the killer.
- Team strength is calculated as 10 points per member plus 5 points per team trophy.

### Team-issued items
- Each assigned player receives one team-marked shield.
- Its name and banner design match the player's team.
- Switching teams updates the existing shield instead of issuing another one.
- The shield is unbreakable, cannot be dropped or stored in containers, and is restored after death.
- Leaving a team removes its issued shield.

## Commands

| Command | Who can use it | Purpose |
|---|---|---|
| /team | Player | Open the team menu |
| /team help | Everyone | List commands |
| /team info | Player | View your team's information |
| /team leave | Player | Leave your current team |
| /team base | Team member | View your own team's base |
| /team warp | Team member | Teleport to your team's base |
| /team trophies | Player | View your trophies |
| /team trophies top | Everyone | View the trophy leaderboard |
| /team set <player> <team> | Admin | Assign an online player |
| /team base set <team> | Admin player | Set a team's base at your location |
| /team base clear <team> | Admin | Remove a team's base |
| /team breach <team> <on or off> | Admin | Set a team's base status |
| /team trophies give <player> <amount> | Admin | Add trophies |
| /team trophies set <player> <amount> | Admin | Set a trophy total |
| /team trophies reset <player> | Admin | Reset trophies |

## Configuration

The plugin creates plugins/TalonTeams/config.yml. It includes:

    allow-team-switching: true
    players: {}

Set allow-team-switching to false to stop current team members from switching teams through the menu. New players can still choose a team, administrators can still use /team set, and players can still use /team leave.

Player assignments, trophies, and base locations are stored in the same config file. Back it up before manually editing it. Existing config files are preserved when updating.

## Permissions

- talonteams.use — normal player commands and team selection. Default: everyone.
- talonteams.admin — administrative team, base, breach, and trophy commands. Default: operators.

## Compatibility and build

- Minecraft server: Paper/Spigot 1.12.2
- Java: 8
- API dependency: Spigot 1.12.2 (provided; not bundled)

Build with:

    mvn --batch-mode clean verify

The JAR is created at target/TalonTeams.jar.

GitHub Actions builds the plugin on pushes to main, pull requests, and manual workflow dispatch. Download the TalonTeams artifact from the completed run.

## Install or update

1. Stop the server.
2. Back up plugins/TalonTeams/ and the existing JAR.
3. Replace plugins/TalonTeams.jar with the new TalonTeams.jar.
4. Start the server and check the console for startup errors.

Do not delete plugins/TalonTeams/config.yml when updating; it contains player, trophy, and base data.

## Not included in this release

Territory claiming, capture-the-flag objectives, automated friendly-fire rules, and team leaders are not implemented in this version. They need their own gameplay rules rather than being silently invented as part of a maintenance update.
