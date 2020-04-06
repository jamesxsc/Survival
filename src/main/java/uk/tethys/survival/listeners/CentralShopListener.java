package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import uk.tethys.survival.Survival;

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

        try (Connection connection = plugin.getDBConnection()) {
            ResultSet unlocked = connection.prepareStatement(
                    String.format("SELECT `item` FROM `unlocked_items` WHERE `player` = '%s'",
                            event.getEntity().getUniqueId().toString())).executeQuery();
            while (unlocked.next()) {
                Bukkit.broadcastMessage(unlocked.getString("item"));
                if (event.getItem().getName().equals(unlocked.getString("item"))) {
                    Bukkit.broadcastMessage("already in DB");
                    return;
                }
            }
            Bukkit.broadcastMessage("adding to DB");
            connection.prepareStatement(String.format(
                    "INSERT INTO `unlocked_items` (`player`, `item`) VALUES ('%s', '%s')",
                    event.getEntity().getUniqueId().toString(), event.getItem().getName())).execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error accessing unlocked items from DB");
            e.printStackTrace();
        }
    }

}
