package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import uk.tethys.survival.Survival;
import uk.tethys.survival.managers.DeathManager;
import uk.tethys.survival.util.InventorySerializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player.getGameMode() == GameMode.SURVIVAL) {
            String[] invSerial = InventorySerializer.playerInventoryToBase64(player.getInventory());

            Location loc = event.getEntity().getLocation();

            boolean safeToClear = true;

            Optional<DeathManager.Death> d = Survival.INSTANCE.getDeathManager().getLatestDeath(player);
            if (d.isPresent()) {
                // default to summoning grave, clearing old death.
                Survival.INSTANCE.getDeathManager().summonGrave(player);
                Survival.INSTANCE.getDeathManager().getDeaths().remove(d.get());
            }

            try (Connection connection = Survival.INSTANCE.getDBConnection()) {
                // remove any trailing records
                connection.prepareStatement(String.format("DELETE FROM `inventory_cache` WHERE `holder` = '%s'", player.getUniqueId().toString())).execute();
                connection.prepareStatement(String.format("INSERT INTO `inventory_cache` (`holder`, `last_stored`, `contents`, `armor`, `exp`, `x`, `y`, `z`, `world`) " +
                                "VALUES ('%s', %d, '%s', '%s', %d, %d, %d, %d, '%s')", player.getUniqueId().toString(), System.currentTimeMillis(), invSerial[0], invSerial[1],
                        event.getDroppedExp(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getUID())).execute();
            } catch (SQLException ex) {
                ex.printStackTrace();
                player.sendMessage("Failed to save inventory cache... marking to restore.");
                safeToClear = !Survival.INSTANCE.getDeathManager().storeDeath(player.getInventory().getContents(), player.getInventory().getArmorContents(),
                        event.getDroppedExp(), player.getUniqueId(), System.currentTimeMillis());
            }

            if (safeToClear) {
                event.setDroppedExp(0);
                event.getDrops().clear();
                //todo debug this - may need to be scheduled to run in next game tick
                // todo pvp flag + explosives
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(Survival.INSTANCE, new ClearDeathTask(event), 0L);
        }
    }

    private static class ClearDeathTask implements Runnable {

        private final PlayerDeathEvent event;

        public ClearDeathTask(PlayerDeathEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            event.getEntity().spigot().respawn();
        }

    }

}
