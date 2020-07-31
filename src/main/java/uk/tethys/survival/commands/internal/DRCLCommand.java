package uk.tethys.survival.commands.internal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;

import java.util.Arrays;
import java.util.Objects;

public class DRCLCommand implements CommandExecutor {

    private final Survival plugin;

    public DRCLCommand(Survival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }
        Player player = (Player) commandSender;
        if (plugin.getEconomyListener().getAllowed().contains(player.getUniqueId())) {
            Inventory reward = Bukkit.createInventory(null, InventoryType.DROPPER, "Claim your reward!");
            reward.setContents(getContents());
            player.openInventory(reward);
        } else {
            player.sendMessage(Messages.DRCL_WAIT());
        }
        return true;
    }

    private ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[9];
        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.setDisplayName(ChatColor.BLACK + "" + ChatColor.MAGIC + "xxx");
        fillMeta.setLocalizedName("survival.internal.fill");
        fill.setItemMeta(fillMeta);
        contents[0] = fill;
        contents[1] = fill;
        contents[2] = fill;
        contents[6] = fill;
        contents[7] = fill;
        contents[8] = fill;
        ItemStack reward1 = new ItemStack(Material.GOLD_NUGGET);
        reward1.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta1 = reward1.getItemMeta();
        Objects.requireNonNull(meta1).setDisplayName("Level 1 Reward");
        // todo real currency please
        meta1.setLore(Arrays.asList("$100", "100% Chance"));
        meta1.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        reward1.setItemMeta(meta1);
        contents[3] = reward1;
        ItemStack reward2 = new ItemStack(Material.GOLD_INGOT);
        reward2.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta2 = reward2.getItemMeta();
        Objects.requireNonNull(meta2).setDisplayName("Level 2 Reward");
        meta2.setLore(Arrays.asList("$1000", "30% Chance"));
        meta2.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        reward2.setItemMeta(meta2);
        contents[4] = reward2;
        ItemStack reward3 = new ItemStack(Material.GOLD_BLOCK);
        reward3.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta3 = reward3.getItemMeta();
        Objects.requireNonNull(meta3).setDisplayName("Level 3 Reward");
        meta3.setLore(Arrays.asList("$10000", "5% Chance"));
        meta3.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        reward3.setItemMeta(meta3);
        contents[5] = reward3;
        return contents;
    }

}
