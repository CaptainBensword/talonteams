
package com.talon.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TalonTeams extends JavaPlugin implements Listener {

    private TeamManager teamManager;
    private ShieldManager shieldManager;

    private static final String MENU_TITLE =
            ChatColor.DARK_RED + "Talon Teams";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        shieldManager = new ShieldManager(this);
        teamManager = new TeamManager(this);

        Bukkit.getPluginManager().registerEvents(shieldManager, this);
        Bukkit.getPluginManager().registerEvents(this, this);
        teamManager.updateOnlinePlayers();

        getLogger().info("TalonTeams Update 3 enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TalonTeams disabled.");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ShieldManager getShieldManager() {
        return shieldManager;
    }

    private boolean hasUsePermission(Player player) {
        if (player.hasPermission("talonteams.use")) {
            return true;
        }

        player.sendMessage(ChatColor.RED
                + "You do not have permission to use TalonTeams.");
        return false;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission("talonteams.admin")) {
            return true;
        }

        sender.sendMessage(ChatColor.RED
                + "You do not have permission to do that.");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("team")) {
            return false;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can open the team menu.");
                return true;
            }

            Player player = (Player) sender;

            if (hasUsePermission(player)) {
                openTeamMenu(player);
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("trophies")) {
            handleTrophies(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "TalonTeams Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/team - Open team menu");
            sender.sendMessage(ChatColor.YELLOW + "/team info - View your team");
            sender.sendMessage(ChatColor.YELLOW + "/team trophies - View your trophies");
            sender.sendMessage(ChatColor.YELLOW + "/team trophies top - Trophy leaderboard");
            sender.sendMessage(ChatColor.YELLOW + "/team base - View your base");
            sender.sendMessage(ChatColor.YELLOW + "/team warp - Warp to your base");
            sender.sendMessage(ChatColor.YELLOW + "/team set <player> <team> - Admin");
            sender.sendMessage(ChatColor.YELLOW + "/team base set <team> - Admin");
            sender.sendMessage(ChatColor.YELLOW + "/team breach <team> <on|off> - Admin");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;

            if (!hasUsePermission(player)) {
                return true;
            }

            Team team = teamManager.getTeam(player.getUniqueId());

            if (team == null) {
                player.sendMessage(ChatColor.RED + "You are not on a team.");
                return true;
            }

            player.sendMessage(ChatColor.GOLD + "Your Team: "
                    + team.getColoredName());
            player.sendMessage(ChatColor.GRAY + "Members: "
                    + teamManager.getMembers(team).size());
            player.sendMessage(ChatColor.GRAY + "Online: "
                    + teamManager.getOnlineCount(team));
            player.sendMessage(ChatColor.GRAY + "Trophies: "
                    + teamManager.getTeamTrophies(team));
            player.sendMessage(ChatColor.GRAY + "Strength: "
                    + teamManager.getStrength(team));

            TeamBase base = teamManager.getBase(team);

            if (base == null) {
                player.sendMessage(ChatColor.GRAY + "Base: Not set");
            } else {
                player.sendMessage(ChatColor.GRAY + "Base: Set");
                player.sendMessage(ChatColor.GRAY + "Status: "
                        + (base.isBreached()
                        ? ChatColor.RED + "BREACHED"
                        : ChatColor.GREEN + "Secure"));
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!hasAdminPermission(sender)) {
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED
                        + "Usage: /team set <player> <team>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            Team team = Team.fromString(args[2]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED
                        + "That player is not online.");
                return true;
            }

            if (team == null) {
                sender.sendMessage(ChatColor.RED + "Unknown team.");
                return true;
            }

            teamManager.setTeam(target, team);

            sender.sendMessage(ChatColor.GREEN + "Set "
                    + target.getName() + "'s team to "
                    + team.getColoredName() + ChatColor.GREEN + ".");

            target.sendMessage(ChatColor.GREEN + "Your team is now "
                    + team.getColoredName() + ChatColor.GREEN + ".");

            return true;
        }

        if (args[0].equalsIgnoreCase("base")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                if (!hasAdminPermission(sender)) {
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can set a base.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED
                            + "Usage: /team base set <team>");
                    return true;
                }

                Player player = (Player) sender;
                Team team = Team.fromString(args[2]);

                if (team == null) {
                    player.sendMessage(ChatColor.RED + "Unknown team.");
                    return true;
                }

                teamManager.setBase(team, player.getLocation());

                player.sendMessage(ChatColor.GREEN
                        + team.getColoredName()
                        + ChatColor.GREEN + " base has been set.");

                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can view a team base.");
                return true;
            }

            Player player = (Player) sender;

            if (!hasUsePermission(player)) {
                return true;
            }

            Team team = teamManager.getTeam(player.getUniqueId());

            if (team == null) {
                player.sendMessage(ChatColor.RED
                        + "You must join a team first.");
                return true;
            }

            TeamBase base = teamManager.getBase(team);

            if (base == null) {
                player.sendMessage(ChatColor.RED
                        + "Your team does not have a base yet.");
                return true;
            }

            Location location = base.getLocation();

            player.sendMessage(ChatColor.GOLD
                    + team.getDisplayName() + " Team Base");
            player.sendMessage(ChatColor.GRAY + "World: "
                    + location.getWorld().getName());
            player.sendMessage(ChatColor.GRAY + "Coordinates: "
                    + location.getBlockX() + ", "
                    + location.getBlockY() + ", "
                    + location.getBlockZ());

            player.sendMessage(ChatColor.GRAY + "Status: "
                    + (base.isBreached()
                    ? ChatColor.RED + "BREACHED"
                    : ChatColor.GREEN + "Secure"));

            return true;
        }

        if (args[0].equalsIgnoreCase("warp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;

            if (!hasUsePermission(player)) {
                return true;
            }

            Team team = teamManager.getTeam(player.getUniqueId());

            if (team == null) {
                player.sendMessage(ChatColor.RED
                        + "You must join a team first.");
                return true;
            }

            TeamBase base = teamManager.getBase(team);

            if (base == null) {
                player.sendMessage(ChatColor.RED
                        + "Your team does not have a base yet.");
                return true;
            }

            player.teleport(base.getLocation());
            player.sendMessage(ChatColor.GREEN
                    + "Teleported to your team base.");

            return true;
        }

        if (args[0].equalsIgnoreCase("breach")) {
            if (!hasAdminPermission(sender)) {
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED
                        + "Usage: /team breach <team> <on|off>");
                return true;
            }

            Team team = Team.fromString(args[1]);

            if (team == null) {
                sender.sendMessage(ChatColor.RED + "Unknown team.");
                return true;
            }

            boolean breached;

            if (args[2].equalsIgnoreCase("on")) {
                breached = true;
            } else if (args[2].equalsIgnoreCase("off")) {
                breached = false;
            } else {
                sender.sendMessage(ChatColor.RED + "Use on or off.");
                return true;
            }

            teamManager.setBreached(team, breached);

            sender.sendMessage(ChatColor.GREEN
                    + team.getDisplayName() + " base status set to "
                    + (breached ? ChatColor.RED + "BREACHED"
                    : ChatColor.GREEN + "Secure"));

            return true;
        }

        sender.sendMessage(ChatColor.RED
                + "Unknown command. Use /team help.");

        return true;
    }

    private void handleTrophies(CommandSender sender, String[] args) {

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console must specify a player.");
                return;
            }

            Player player = (Player) sender;

            if (!hasUsePermission(player)) {
                return;
            }

            player.sendMessage(ChatColor.GOLD + "Your trophies: "
                    + ChatColor.YELLOW
                    + teamManager.getTrophies(player.getUniqueId()));
            return;
        }

        if (args[1].equalsIgnoreCase("top")) {
            if (sender instanceof Player
                    && !hasUsePermission((Player) sender)) {
                return;
            }

            List<UUID> leaderboard =
                    teamManager.getTrophyLeaderboard();

            sender.sendMessage(ChatColor.GOLD
                    + "----- Trophy Leaderboard -----");

            if (leaderboard.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY
                        + "Nobody has earned trophies yet.");
                return;
            }

            int limit = Math.min(10, leaderboard.size());

            for (int i = 0; i < limit; i++) {
                UUID uuid = leaderboard.get(i);
                String name = teamManager.getMemberName(uuid);
                int trophies = teamManager.getTrophies(uuid);

                sender.sendMessage(ChatColor.YELLOW + "#" + (i + 1)
                        + ChatColor.GRAY + " " + name
                        + ChatColor.GOLD + " - " + trophies);
            }

            return;
        }

        if (!hasAdminPermission(sender)) {
            return;
        }

        if (args[1].equalsIgnoreCase("give")
                || args[1].equalsIgnoreCase("set")) {

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED
                        + "Usage: /team trophies "
                        + args[1].toLowerCase()
                        + " <player> <amount>");
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

            if (target.getName() == null) {
                sender.sendMessage(ChatColor.RED
                        + "That player could not be found.");
                return;
            }

            int amount;

            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(ChatColor.RED
                        + "Enter a valid whole number.");
                return;
            }

            if (amount < 0
                    || (args[1].equalsIgnoreCase("give") && amount == 0)) {
                sender.sendMessage(ChatColor.RED
                        + "Enter a positive amount.");
                return;
            }

            if (args[1].equalsIgnoreCase("give")) {
                teamManager.addTrophies(target.getUniqueId(), amount);
            } else {
                teamManager.setTrophies(target.getUniqueId(), amount);
            }

            int newTotal = teamManager.getTrophies(target.getUniqueId());

            sender.sendMessage(ChatColor.GREEN + "Updated "
                    + target.getName() + "'s trophies to "
                    + newTotal + ".");

            if (target.isOnline()) {
                Player onlineTarget = target.getPlayer();
                onlineTarget.sendMessage(ChatColor.GOLD
                        + "Your trophy count is now "
                        + ChatColor.YELLOW + newTotal + ChatColor.GOLD + ".");
            }

            getLogger().info(sender.getName() + " changed "
                    + target.getName() + "'s trophies to " + newTotal + ".");

            return;
        }

        if (args[1].equalsIgnoreCase("reset")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED
                        + "Usage: /team trophies reset <player>");
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

            if (target.getName() == null) {
                sender.sendMessage(ChatColor.RED
                        + "That player could not be found.");
                return;
            }

            teamManager.setTrophies(target.getUniqueId(), 0);

            sender.sendMessage(ChatColor.GREEN
                    + "Reset " + target.getName() + "'s trophies.");

            if (target.isOnline()) {
                target.getPlayer().sendMessage(ChatColor.RED
                        + "Your trophies have been reset by an admin.");
            }

            getLogger().info(sender.getName()
                    + " reset " + target.getName() + "'s trophies.");

            return;
        }

        sender.sendMessage(ChatColor.RED
                + "Usage: /team trophies [top|give|set|reset]");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        int transferred = teamManager.getTrophies(victim.getUniqueId());

        if (transferred <= 0) {
            return;
        }

        teamManager.setTrophies(victim.getUniqueId(), 0);
        teamManager.addTrophies(killer.getUniqueId(), transferred);

        killer.sendMessage(ChatColor.GOLD + "You claimed "
                + ChatColor.YELLOW + transferred
                + ChatColor.GOLD + " trophies from "
                + victim.getName() + ".");

        victim.sendMessage(ChatColor.RED + "You lost "
                + transferred + " trophies to "
                + killer.getName() + ".");

        getLogger().info(killer.getName() + " claimed "
                + transferred + " trophies from " + victim.getName() + ".");
    }

    private void openTeamMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(
                null, 27, MENU_TITLE);

        addTeamItem(inventory, 10, Team.RED, (short) 14);
        addTeamItem(inventory, 12, Team.BLUE, (short) 11);
        addTeamItem(inventory, 14, Team.GREEN, (short) 13);
        addTeamItem(inventory, 16, Team.YELLOW, (short) 4);

        player.openInventory(inventory);
    }

    private void addTeamItem(Inventory inventory, int slot,
                             Team team, short woolColor) {

        ItemStack item = new ItemStack(
                Material.WOOL, 1, woolColor);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(team.getColoredName());

        List<String> lore = new ArrayList<String>();

        lore.add(ChatColor.GRAY + "Team Members:");

        for (UUID uuid : teamManager.getMembers(team)) {
            Player online = Bukkit.getPlayer(uuid);
            String name = teamManager.getMemberName(uuid);

            if (online != null && online.isOnline()) {
                lore.add(ChatColor.GREEN + "[Online] " + name);
            } else {
                lore.add(ChatColor.GRAY + "[Offline] " + name);
            }
        }

        if (teamManager.getMembers(team).isEmpty()) {
            lore.add(ChatColor.GRAY + "No members yet.");
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Strength: "
                + teamManager.getStrength(team));
        lore.add(ChatColor.GRAY + "Online: "
                + teamManager.getOnlineCount(team));

        TeamBase base = teamManager.getBase(team);

        if (base == null) {
            lore.add(ChatColor.GRAY + "Base: Not set");
            lore.add(ChatColor.GRAY + "Status: Unknown");
        } else {
            Location location = base.getLocation();

            lore.add(ChatColor.GRAY + "Base: "
                    + location.getWorld().getName()
                    + " (" + location.getBlockX()
                    + ", " + location.getBlockY()
                    + ", " + location.getBlockZ() + ")");

            lore.add(ChatColor.GRAY + "Status: "
                    + (base.isBreached()
                    ? ChatColor.RED + "BREACHED"
                    : ChatColor.GREEN + "Secure"));
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to join this team.");

        meta.setLore(lore);
        item.setItemMeta(meta);

        inventory.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView()
                .getTopInventory().getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!hasUsePermission(player)) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()
                || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String teamName = ChatColor.stripColor(
                clicked.getItemMeta().getDisplayName());

        Team team = Team.fromString(teamName);

        if (team == null) {
            return;
        }

        teamManager.setTeam(player, team);

        player.sendMessage(ChatColor.GREEN + "You joined "
                + team.getColoredName() + ChatColor.GREEN + ".");

        player.closeInventory();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teamManager.updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team team = teamManager.getTeam(player.getUniqueId());

        String prefix = team == null
                ? ChatColor.GRAY + "[No Team]"
                : team.getColoredName();

        event.setFormat(prefix + ChatColor.GRAY + " %1$s"
                + ChatColor.WHITE + ": %2$s");
    }
}
