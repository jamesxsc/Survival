package uk.tethys.survival.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;

public class CreateShopCommand implements CommandExecutor {

    private final Survival plugin;

    public CreateShopCommand(Survival plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }
        Player player = (Player) commandSender;
        ItemStack tool = new ItemStack(Material.PAPER);
        ItemMeta toolMeta = tool.getItemMeta();
        toolMeta.setLocalizedName("survival.items.tools.shop");
        toolMeta.setDisplayName(ChatColor.GRAY + "Create Shop Tool");
        tool.setItemMeta(toolMeta);

        PlayerInventory inventory = player.getInventory();

        if (inventory.contains(tool)) {
            int heldItemSlot = inventory.getHeldItemSlot();
            int toolSlot = inventory.first(tool);

            inventory.setItem(toolSlot, inventory.getItem(heldItemSlot));
            inventory.setItem(heldItemSlot, tool);

        } else {
            inventory.addItem(tool);

        }
        player.sendMessage(Messages.SHOP_TOOL_INFO);
        return true;
    }

}
