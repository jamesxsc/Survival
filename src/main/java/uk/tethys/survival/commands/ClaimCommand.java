package uk.tethys.survival.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;

public class ClaimCommand implements CommandExecutor {

    private final Survival plugin;

    public static NamespacedKey IS_CLAIM_TOOL;
    public static NamespacedKey CLAIM_TOOL_MODE;
    public static NamespacedKey CLAIM_FLAG_NAME;
    public static NamespacedKey CLAIM_FLAG_AUTH_LEVEL;

    public ClaimCommand(Survival plugin) {
        this.plugin = plugin;
        IS_CLAIM_TOOL = new NamespacedKey(plugin, "is-claim-tool");
        CLAIM_TOOL_MODE = new NamespacedKey(plugin, "claim-tool-mode");
        CLAIM_FLAG_NAME = new NamespacedKey(plugin, "claim-flag-name");
        CLAIM_FLAG_AUTH_LEVEL = new NamespacedKey(plugin, "claim-flag-auth-level");
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }
        Player player = (Player) commandSender;
        ItemStack tool = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta toolMeta = tool.getItemMeta();

        toolMeta.getPersistentDataContainer().set(Survival.IS_CUSTOM_ITEM, PersistentDataType.BYTE, (byte) 1);
        toolMeta.getPersistentDataContainer().set(IS_CLAIM_TOOL, PersistentDataType.BYTE, (byte) 1);
        toolMeta.getPersistentDataContainer().set(CLAIM_TOOL_MODE, PersistentDataType.STRING, "claim");

        toolMeta.setDisplayName(ChatColor.AQUA + "Claim Tool");
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
        player.sendMessage(Messages.CLAIM_TOOL_INFO);
        return true;
    }

}
