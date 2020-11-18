package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import uk.tethys.survival.Survival;
import uk.tethys.survival.managers.EconomyManager;
import uk.tethys.survival.util.InventorySerializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

public class PlayerRespawnListener implements Listener {

    private static final int BASE_RESTORE_COST = 2500;
    private final Survival plugin;

    public PlayerRespawnListener(Survival survival) {
        this.plugin = survival;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SURVIVAL) {
            try (Connection connection = Survival.INSTANCE.getDBConnection()) {
                ResultSet rs = connection.prepareStatement(String.format("SELECT * FROM `inventory_cache` WHERE `holder` = '%s'",
                        player.getUniqueId().toString())).executeQuery();
                if (rs.next()) {
//
//                    ItemStack[] contents = InventorySerializer.itemStackArrayFromBase64(rs.getString("contents"));
//                    ItemStack[] armor = InventorySerializer.itemStackArrayFromBase64(rs.getString("armor"));
//                    player.getInventory().setContents(contents);
//                    player.getInventory().setArmorContents(armor);
                    //todo display time better
                    Inventory postMortem = Bukkit.createInventory(null, 27, "Death");
//                    Inventory postMortem = Bukkit.createInventory(null, 27, "Death at " + rs.getTimestamp("last_stored").toGMTString());
                    postMortem.setContents(getPostMortenContents());

                    // todo remind how to open if accidentally closed
                    player.openInventory(postMortem);
                } else {
                    handleDBException(player);
                }
            } catch (SQLException ex) {
                handleDBException(player);
//            } catch (IOException e) {
//                player.sendMessage("Error deserializing inventory. Report this immediately.");
            }
        }
    }

    @EventHandler
    public void onSelectPostMortemOption(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        if (plugin.getDeathManager().getLatestDeath(player).isPresent()) {
            if (event.getView().getTitle().equals("Death")) {
//            if (event.getView().getTitle().equals("Death at " + new Date(plugin.getDeathManager().getDeaths().get(player.getUniqueId())).toGMTString())) {
                ItemStack selected = (event.getCursor() == null || event.getCursor().getType() == Material.AIR) ? event.getCurrentItem() : event.getCursor();
                if (selected == null || selected.getType() == Material.AIR)
                    return;

                boolean success;
                switch (selected.getType()) {
                    case CHEST:
                        success = plugin.getDeathManager().summonGrave(player);
                        break;
                    case ENDER_CHEST:
                        int balance;
                        try {
                            balance = plugin.getEconomyManager().getBalance(player);
                        } catch (SQLException ex) {
                            // todo inform player and cancel
                            return;
                        }

                        // todo calc for complex inv contents
                        if (balance > BASE_RESTORE_COST) {
                            success = restoreInventory(player);
                        } else {
                            // todo inform player that they are poor lmao
                            return;
                        }

                        if (success) {
                            try {
                                plugin.getEconomyManager().alterBalance(player, -BASE_RESTORE_COST);
                            } catch (SQLException ex) {
                                // todo inform player and give them it on the house oops economy = broken
                            } catch (EconomyManager.BankruptcyException ex) {
                                // todo still somehow bankrupt... mainly to be thread safe
                            }
                        }

                        // todo change pooling of connections (get statement not connection each time but make sure to close)
                        break;
                    default:
                        return;
                }

                event.setCancelled(true);
                player.closeInventory();
                plugin.getDeathManager().getDeaths().remove(player.getUniqueId());

                if (!success) {
                    handleDBException(player);
                }
            }
        }
    }


    // todo task to clear old graves and inform player that grave will last e.g. 3 hours
    @EventHandler
    public void onOpenGrave(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        Block block = event.getClickedBlock();
        if (block == null)
            return;
        if (block.getType() != Material.CHEST)
            return;
        if (!block.hasMetadata("is_grave") || !block.getMetadata("is_grave").get(0).asBoolean())
            return;

        // at this point we are fairly certain the player has right clicked a grave with their main hand
        // todo generify to avoid using get(0)
        UUID graveOwner = (UUID) block.getMetadata("grave_owner").get(0).value();
        ItemStack[] contents = (ItemStack[]) block.getMetadata("grave_contents").get(0).value();
        ItemStack[] armor = (ItemStack[]) block.getMetadata("grave_armor").get(0).value();
        int exp = block.getMetadata("grave_exp").get(0).asInt();

        Player player = event.getPlayer();

        if (player.getUniqueId().equals(graveOwner)) {
            block.setType(Material.AIR);
            player.setTotalExperience(exp);

            Inventory reclaim = Bukkit.createInventory(null, 36, "Reclaim Items");
            reclaim.setContents((ItemStack[]) Stream.concat(Arrays.stream(contents), Arrays.stream(armor)).toArray());
            player.openInventory(reclaim);
        } else {
            // todo NOT YOUR GRAVE!
            player.sendMessage("Not your grave!");
        }
    }

    private boolean restoreInventory(Player player) {
        // we begin by retrieving data about the death
        ItemStack[] inv;
        ItemStack[] armor;
        int exp;
        long millis;
        try (Connection connection = Survival.INSTANCE.getDBConnection()) {
            ResultSet rs = connection.prepareStatement(String.format("SELECT * FROM `inventory_cache` WHERE `holder` = '%s'", player.getUniqueId().toString())).executeQuery();
            if (rs.next()) {
                inv = InventorySerializer.itemStackArrayFromBase64(rs.getString("contents"));
                armor = InventorySerializer.itemStackArrayFromBase64(rs.getString("armor"));
                exp = rs.getInt("exp");
                millis = rs.getTimestamp("last_stored").getTime();
            } else return false;
        } catch (SQLException | IOException ex) {
            // catch db exception or any other weird error whilst decoding base64
            return false;
        }

        player.getInventory().setArmorContents(armor);
        player.getInventory().setContents(inv);
        player.setTotalExperience(exp);
        return true;
    }

    private void handleDBException(Player respawned) {
        respawned.sendMessage("Your inventory failed to save... it will be restored automatically");
        // todo restore from runtime
    }


    private ItemStack[] getPostMortenContents() {
        ItemStack[] stacks = new ItemStack[27];

        ItemStack reclaim = new ItemStack(Material.ENDER_CHEST);
        ItemMeta reclaimMeta = reclaim.getItemMeta();
        reclaimMeta.setDisplayName(ChatColor.GOLD + "Reclaim items immediately");
        reclaimMeta.setLore(Arrays.asList(
                ChatColor.RESET + "",
                //todo currency symbol and calculate true value of inv and take log percent of that value
                ChatColor.RED + "Warning: costs $2500"
        ));
        reclaim.setItemMeta(reclaimMeta);
        stacks[11] = reclaim;

        ItemStack deposit = new ItemStack(Material.CHEST);
        ItemMeta depositMeta = deposit.getItemMeta();
        depositMeta.setDisplayName(ChatColor.GOLD + "Leave items at location of death");
        depositMeta.setLore(Arrays.asList(
                ChatColor.RESET + "",
                ChatColor.DARK_GRAY + "Items will be stored safely",
                ChatColor.DARK_GRAY + "and will be inaccessible to",
                ChatColor.DARK_GRAY + "other players."
        ));
        deposit.setItemMeta(depositMeta);
        stacks[14] = deposit;

        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.setDisplayName(ChatColor.BLACK + "" + ChatColor.MAGIC + "xxx");
        fillMeta.setLocalizedName("survival.internal.fill");
        fill.setItemMeta(fillMeta);

        for (int j = 0; j < stacks.length; j++) {
            if (stacks[j] == null) {
                stacks[j] = fill;
            }
        }

        return stacks;
    }

}
