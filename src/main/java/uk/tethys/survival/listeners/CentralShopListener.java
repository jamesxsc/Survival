package uk.tethys.survival.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CentralShopListener implements Listener {

    private final Survival plugin;

    public CentralShopListener(Survival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUnlock(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        String itemName;

        if (event.getItem().getItemStack().getItemMeta() != null) {
            String localizedName = event.getItem().getItemStack().getItemMeta().getLocalizedName();
            if (localizedName.startsWith("survival.")) {
                // tools do not count for unlockable items
                return;
            } else if (localizedName.startsWith("svcustom.")) {
                // custom items cant use vanilla names
                itemName = localizedName;
            } else {
                itemName = event.getItem().getItemStack().getType().name();
            }
        } else {
            itemName = event.getItem().getItemStack().getType().name();
        }

        try (Connection connection = plugin.getDBConnection()) {
            ResultSet unlocked = connection.prepareStatement(
                    String.format("SELECT `item` FROM `unlocked_items` WHERE `player` = '%s'",
                            event.getEntity().getUniqueId().toString())).executeQuery();
            while (unlocked.next())
                if (itemName.equals(unlocked.getString("item"))) return;
            connection.prepareStatement(String.format(
                    "INSERT INTO `unlocked_items` (`player`, `item`) VALUES ('%s', '%s')",
                    event.getEntity().getUniqueId().toString(), itemName)).execute();

            event.getEntity().sendMessage(Messages.UNLOCK_ITEM(event.getItem().getItemStack()));
        } catch (SQLException e) {
            event.getEntity().sendMessage(Messages.DATABASE_ERROR("sql.unlocked_items", e.getMessage()));
        }
    }

}
