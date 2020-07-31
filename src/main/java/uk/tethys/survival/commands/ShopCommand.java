package uk.tethys.survival.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;

public class ShopCommand implements CommandExecutor {

    private final Survival plugin;

    public ShopCommand(Survival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) commandSender;

        player.openInventory(Bukkit.createInventory(null, 18, ChatColor.GOLD + "Survival Shop"));

        return true;
    }

}
