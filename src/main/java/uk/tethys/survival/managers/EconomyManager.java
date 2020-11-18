package uk.tethys.survival.managers;

import org.bukkit.entity.Player;
import uk.tethys.survival.Survival;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EconomyManager {

    private final Survival plugin;

    public EconomyManager(Survival plugin) {
        this.plugin = plugin;
    }

    public int getBalance(Player player) throws SQLException {
        return getBalance(player.getUniqueId());
    }

    public int getBalance(UUID uuid) throws SQLException {
        try (Connection connection = plugin.getDBConnection()) {
            ResultSet rs = connection.prepareStatement(String.format("SELECT `balance` FROM `balances` WHERE `player` = '%s'", uuid.toString())).executeQuery();

            if (rs.next()) {
                return rs.getInt("balance");
            } else {
                connection.prepareStatement(String.format("INSERT INTO `balances` (player, balance) VALUES (%s, %d)", uuid.toString(), 0));
                return 0;
            }
        }
    }

    /**
     * @param uuid The UUID of the player whose balance is to be changed
     * @param delta The desired offset
     * @return The new balance
     */
    public int alterBalance(UUID uuid, int delta) throws SQLException, BankruptcyException {
        int oldBalance = getBalance(uuid);

        if (oldBalance + delta < 0) {
            throw new BankruptcyException();
        }

        try (Connection connection = plugin.getDBConnection()) {
            // we can guarantee that there is a record in the database as getBalance ensures this.
            connection.prepareStatement(String.format("UPDATE `balances` SET `balance` = %d WHERE `player` = '%s'",
                    oldBalance + delta, uuid.toString()));
        }

        return getBalance(uuid);
    }

    public int alterBalance(Player player, int delta) throws SQLException, BankruptcyException {
        return alterBalance(player.getUniqueId(), delta);
    }
    public static class BankruptcyException extends Throwable { }
}
