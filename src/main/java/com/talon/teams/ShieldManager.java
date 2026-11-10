package com.talon.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;

public class ShieldManager {

    private static final String ITEM_MARKER = "talonteams:teamshield:";
    private final TalonTeams plugin;

    public ShieldManager(TalonTeams plugin) {
        this.plugin = plugin;
    }

    public boolean isTeamShield(ItemStack item) {
        if (item == null || item.getType() != Material.SHIELD
                || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasLocalizedName()
                && meta.getLocalizedName().startsWith(ITEM_MARKER);
    }

    public void updateTeamShield(Player player, Team team) {
        if (player == null) {
            return;
        }

        if (team == null) {
            removeTeamShields(player);
            return;
        }

        removeTeamShields(player);
        giveShield(player, team);
    }

    public void ensureTeamShield(Player player, Team team) {
        if (player == null) {
            return;
        }

        if (team == null) {
            removeTeamShields(player);
            return;
        }

        PlayerInventory inventory = player.getInventory();
        boolean keptOne = false;

        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];

            if (!isTeamShield(item)) {
                continue;
            }

            if (!keptOne && isShieldForTeam(item, team)) {
                keptOne = true;
            } else {
                inventory.setItem(slot, null);
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (isTeamShield(offHand)) {
            if (!keptOne && isShieldForTeam(offHand, team)) {
                keptOne = true;
            } else {
                inventory.setItemInOffHand(null);
            }
        }

        if (!keptOne) {
            giveShield(player, team);
        }
    }

    public void removeTeamShields(Player player) {
        if (player == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            if (isTeamShield(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }

        if (isTeamShield(inventory.getItemInOffHand())) {
            inventory.setItemInOffHand(null);
        }
    }

    private boolean isShieldForTeam(ItemStack item, Team team) {
        if (!isTeamShield(item) || team == null) {
            return false;
        }

        return item.getItemMeta().getLocalizedName()
                .equals(ITEM_MARKER + team.name());
    }

    private void giveShield(Player player, Team team) {
        ItemStack shield = createShield(team);
        Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(shield);

        if (!leftovers.isEmpty()) {
            if (player.getInventory().getItemInOffHand() == null
                    || player.getInventory().getItemInOffHand()
                    .getType() == Material.AIR) {
                player.getInventory().setItemInOffHand(
                        leftovers.values().iterator().next());
            } else {
                player.sendMessage(ChatColor.RED
                        + "Your team shield is ready, but your inventory is full. "
                        + "Make room and rejoin your team to receive it.");
            }
        }
    }

    private ItemStack createShield(Team team) {
        ItemStack shield = new ItemStack(Material.SHIELD, 1);
        ItemMeta rawMeta = shield.getItemMeta();

        if (rawMeta instanceof BannerMeta) {
            BannerMeta meta = (BannerMeta) rawMeta;
            meta.setBaseColor(getBaseColor(team));

            switch (team) {
                case RED:
                    meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.CROSS));
                    meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.CIRCLE));
                    break;
                case BLUE:
                    meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNRIGHT));
                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
                    meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.CIRCLE));
                    break;
                case GREEN:
                    meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.TRIANGLE_TOP));
                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
                    meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                    break;
                case YELLOW:
                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
                    meta.addPattern(new Pattern(DyeColor.RED, PatternType.CROSS));
                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.CIRCLE));
                    break;
                default:
                    break;
            }

            rawMeta = meta;
        }

        rawMeta.setDisplayName(team.getColor() + team.getDisplayName()
                + " Team Shield");
        rawMeta.setLore(createLore(team));
        rawMeta.setLocalizedName(ITEM_MARKER + team.name());
        rawMeta.setUnbreakable(true);
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
                return DyeColor.YELLOW;
            default:
                return DyeColor.WHITE;
        }
    }

    private List<String> createLore(Team team) {
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Issued by TalonTeams");
        lore.add(ChatColor.GRAY + "Belongs to: " + team.getColoredName());
        lore.add(ChatColor.DARK_GRAY + "Cannot be broken or dropped.");
        return lore;
    }
}
