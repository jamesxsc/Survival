package uk.tethys.survival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ShopTest {

    public static void createShopDisplay(ItemStack itemStack, Location location, int buy, int sell) {
        try {
        } catch (Throwable t) {
            Bukkit.broadcastMessage(Arrays.toString(t.getStackTrace()));
        }
    }

}
