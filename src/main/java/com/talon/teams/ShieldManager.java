package com.talon.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class ShieldManager implements Listener {

    private static final String SHIELD_MARKER = "talonteams:shield:";
    private final TalonTeams plugin;

    public ShieldManager(TalonTeams plugin) {
        this.plugin = plugin;
    }

    /**
     * Keeps exactly one shield belonging to this player and makes its design
     * match the player's current team. This method is called from the central
     * TeamManager update path, so menu changes, admin changes, joins, and
     * leaving all use the same reconciliation logic.
     */
    public void updateTeamShield(Player player, Team team) {
        if (player == null) {
            return;
        }

        UUID owner = player.getUniqueId();
        boolean[] keptOne = new boolean[] { false };

        ItemStack cursor = player.getItemOnCursor();
        ItemStack updatedCursor = reconcileItem(cursor, owner, team, keptOne);
        if (updatedCursor != cursor) {
            player.setItemOnCursor(updatedCursor);
        }

        PlayerInventory inventory = player.getInventory();

        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            ItemStack updated = reconcileItem(item, owner, team, keptOne);
            if (updated != item) {
                inventory.setItem(slot, updated);
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        ItemStack updatedOffHand =
                reconcileItem(offHand, owner, team, keptOne);
        if (updatedOffHand != offHand) {
            inventory.setItemInOffHand(updatedOffHand);
        }

        ItemStack[] armor = inventory.getArmorContents();
        boolean armorChanged = false;
        for (int slot = 0; slot < armor.length; slot++) {
            ItemStack item = armor[slot];
            ItemStack updated = reconcileItem(item, owner, team, keptOne);
            if (updated != item) {
                armor[slot] = updated;
                armorChanged = true;
            }
        }
        if (armorChanged) {
            inventory.setArmorContents(armor);
        }

        if (team != null && !keptOne[0]) {
            ItemStack shield = createTeamShield(owner, team);
            Map<Integer, ItemStack> leftovers = inventory.addItem(shield);

            if (!leftovers.isEmpty()) {
                if (isEmpty(inventory.getItemInOffHand())) {
                    inventory.setItemInOffHand(shield);
                } else {
                    player.sendMessage(ChatColor.RED
                            + "Your team shield is ready, but your inventory "
                            + "is full. Make an empty slot to receive it.");
                }
            }
        }
    }

    private ItemStack reconcileItem(ItemStack item, UUID owner,
                                    Team team, boolean[] keptOne) {
        if (!isTeamShield(item)) {
            return item;
        }

        if (team == null || !belongsTo(item, owner) || keptOne[0]) {
            return null;
        }

        keptOne[0] = true;
        return createTeamShield(owner, team);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR
                || item.getAmount() <= 0;
    }

    private boolean isTeamShield(ItemStack item) {
        if (item == null || item.getType() != Material.SHIELD
                || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasLocalizedName()
                && meta.getLocalizedName().startsWith(SHIELD_MARKER);
    }

    private boolean belongsTo(ItemStack item, UUID owner) {
        if (!isTeamShield(item)) {
            return false;
        }

        String expected = SHIELD_MARKER + owner.toString();
        return expected.equals(item.getItemMeta().getLocalizedName());
    }

    private ItemStack createTeamShield(UUID owner, Team team) {
        ItemStack shield = new ItemStack(Material.SHIELD, 1);
        ItemMeta rawMeta = shield.getItemMeta();

        if (rawMeta instanceof BlockStateMeta) {
            BlockStateMeta meta = (BlockStateMeta) rawMeta;
            BlockState state = meta.getBlockState();

            if (state instanceof Banner) {
                Banner banner = (Banner) state;
                banner.setBaseColor(getBaseColor(team));
                banner.setPatterns(getPatterns(team));
                meta.setBlockState(banner);
            } else {
                plugin.getLogger().warning(
                        "Could not apply the " + team.name()
                        + " shield banner: SHIELD did not return a Banner "
                        + "block state on this server.");
            }
        } else {
            plugin.getLogger().warning(
                    "Could not apply team shield patterns: SHIELD did not "
                    + "return BlockStateMeta on this server.");
        }

        rawMeta.setDisplayName(team.getColor()
                + team.getDisplayName() + " Team Shield");
        rawMeta.setLocalizedName(SHIELD_MARKER + owner.toString());
        rawMeta.setUnbreakable(true);

        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "A shield issued by TalonTeams.");
        lore.add(ChatColor.DARK_GRAY + "Bound to its team member.");
        rawMeta.setLore(lore);

        shield.setItemMeta(rawMeta);
        return shield;
    }

    private DyeColor getBaseColor(Team team) {
        switch (team) {
            case RED:
                return DyeColor.RED;
            case BLUE:
                return DyeColor.BLUE;
            case GREEN:
                return DyeColor.GREEN;
            case YELLOW:
            default:
                return DyeColor.YELLOW;
        }
    }

    private List<Pattern> getPatterns(Team team) {
        List<Pattern> patterns = new ArrayList<Pattern>();

        switch (team) {
            case RED:
                patterns.add(new Pattern(DyeColor.WHITE,
                        PatternType.CROSS));
                patterns.add(new Pattern(DyeColor.BLACK,
                        PatternType.TRIANGLE_TOP));
                break;
            case BLUE:
                patterns.add(new Pattern(DyeColor.LIGHT_BLUE,
                        PatternType.DIAGONAL_LEFT));
                patterns.add(new Pattern(DyeColor.WHITE,
                        PatternType.CIRCLE_MIDDLE));
                break;
            case GREEN:
                patterns.add(new Pattern(DyeColor.LIME,
                        PatternType.CURLY_BORDER));
                patterns.add(new Pattern(DyeColor.BLACK,
                        PatternType.STRAIGHT_CROSS));
                break;
            case YELLOW:
                patterns.add(new Pattern(DyeColor.ORANGE,
                        PatternType.DIAGONAL_RIGHT));
                patterns.add(new Pattern(DyeColor.BLACK,
                        PatternType.TRIANGLE_BOTTOM));
                break;
            default:
                break;
        }

        return patterns;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamShieldClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = rawSlot >= 0 && rawSlot < topSize;

        boolean cursorShield = isTeamShield(event.getCursor());
        boolean clickedShield = isTeamShield(event.getCurrentItem());

        int hotbarButton = event.getHotbarButton();
        boolean hotbarShield = hotbarButton >= 0
                && isTeamShield(player.getInventory().getItem(hotbarButton));

        if (clickedTop && (cursorShield || clickedShield || hotbarShield)) {
            event.setCancelled(true);
            return;
        }

        if (cursorShield && rawSlot == -999) {
            event.setCancelled(true);
            return;
        }

        if (clickedShield && event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if (clickedShield && (event.getAction() == InventoryAction.DROP_ONE_SLOT
                || event.getAction() == InventoryAction.DROP_ALL_SLOT)) {
            event.setCancelled(true);
            return;
        }

        if (clickedShield && event.getClick() == ClickType.MIDDLE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamShieldDrag(InventoryDragEvent event) {
        int topSize = event.getView().getTopInventory().getSize();

        if (isTeamShield(event.getOldCursor())) {
            for (Integer rawSlot : event.getRawSlots()) {
                if (rawSlot >= 0 && rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        for (Map.Entry<Integer, ItemStack> entry
                : event.getNewItems().entrySet()) {
            Integer rawSlot = entry.getKey();
            if (rawSlot >= 0 && rawSlot < topSize
                    && isTeamShield(entry.getValue())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamShieldDrop(PlayerDropItemEvent event) {
        if (isTeamShield(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED
                    + "Your team shield cannot be dropped or traded.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamShieldPickup(PlayerPickupItemEvent event) {
        if (isTeamShield(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamShieldInventoryMove(InventoryMoveItemEvent event) {
        if (isTeamShield(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeamShieldDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            if (isTeamShield(drops.next())) {
                drops.remove();
            }
        }
    }

    @EventHandler
    public void onTeamShieldRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getTeamManager().updatePlayer(player);
            }
        }.runTaskLater(plugin, 1L);
    }
}
