package uk.tethys.survival.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BalanceCommand implements CommandExecutor {

    private final Survival plugin;

    public BalanceCommand(Survival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("You must be a player to use this command!");
        } else {
            Player player = (Player) commandSender;

            try (ResultSet resultSet = plugin.getDBConnection().prepareStatement(String.format("SELECT `balance` FROM balances WHERE `player` = '%s' LIMIT 1",
                    player.getUniqueId().toString())).executeQuery()) {
                int balance = resultSet.getInt(1);

                player.sendMessage(Messages.SELF_BALANCE(balance));
            } catch (SQLException e) {
                player.sendMessage(Messages.DATABASE_ERROR("sql.balances.balance", e.getMessage()));
            }
        }
        return true;
    }

}
