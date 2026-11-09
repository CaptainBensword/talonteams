package com.talon.teams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class TalonTeams extends JavaPlugin implements Listener {

    private TeamManager teamManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teamManager = new TeamManager(this);

        Bukkit.getPluginManager().registerEvents(this, this);
        teamManager.updateOnlinePlayers();

        getLogger().info("TalonTeams has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TalonTeams has been disabled.");
    }

    public TeamManager getTeamManager() {
        return teamManager;
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

            openTeamMenu((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "TalonTeams Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/team - Open the team menu");
            sender.sendMessage(ChatColor.YELLOW + "/team info - View your team");
            sender.sendMessage(ChatColor.YELLOW + "/team set <player> <team> - Set a player's team");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;
            Team team = teamManager.getTeam(player);

            if (team == null) {
                player.sendMessage(ChatColor.RED + "You are not on a team.");
            } else {
                player.sendMessage(ChatColor.GRAY + "Your team: "
                        + team.getColoredName());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("talonteams.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /team set <player> <team>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            Team team = Team.fromString(args[2]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "That player is not online.");
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

        sender.sendMessage(ChatColor.RED + "Unknown command. Use /team help.");
        return true;
    }

    private void openTeamMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(
                null, 27, ChatColor.DARK_RED + "Choose Your Team");

        addTeamItem(inventory, 10, Team.RED, Material.WOOL, (short) 14);
        addTeamItem(inventory, 12, Team.BLUE, Material.WOOL, (short) 11);
        addTeamItem(inventory, 14, Team.GREEN, Material.WOOL, (short) 13);
        addTeamItem(inventory, 16, Team.YELLOW, Material.WOOL, (short) 4);
        addTeamItem(inventory, 22, Team.VIGILANTE, Material.NETHER_STAR, (short) 0);

        player.openInventory(inventory);
    }

    private void addTeamItem(Inventory inventory, int slot, Team team,
                             Material material, short data) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(team.getColoredName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Join the " + team.getDisplayName() + " team",
                ChatColor.YELLOW + "Click to select"
        ));

        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle())
                .equalsIgnoreCase("Choose Your Team")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
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
        Team team = teamManager.getTeam(player);

        String prefix = team == null
                ? ChatColor.GRAY + "[No Team]"
                : team.getColoredName();

        event.setFormat(prefix + ChatColor.GRAY + " %1$s"
                + ChatColor.WHITE + ": %2$s");
    }
}
