package uk.tethys.survival.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import uk.tethys.survival.objects.Shop;

public class CreateShopDisplayTask extends BukkitRunnable {

    private final Shop shop;

    public CreateShopDisplayTask(Shop shop) {
        this.shop = shop;
    }

    @Override
    public void run() {
        Location location = shop.getLocation().getLocation();
        if (((Chest) location.getBlock().getState()) instanceof DoubleChestInventory) {
            if (!shop.getDoubleDirection().isPresent())
                throw new IllegalStateException(
                        "Method Shop#getDoubleDirection() (ShopListener#findAdjacentOfSameType(Block)) returned a very unexpected result. " +
                                "Check plugin jar hash.");
            if (shop.getDoubleDirection().get().equals("WEST")) {
                location.setZ(location.getBlockZ() + .5);
            } else if (shop.getDoubleDirection().get().equals("EAST")) {
                location.setX(location.getBlockX() + 1);
                location.setZ(location.getBlockZ() + .5);
            } else if (shop.getDoubleDirection().get().equals("SOUTH")) {
                location.setZ(location.getBlockZ() + 1);
                location.setX(location.getBlockX() + .5);
            } else if (shop.getDoubleDirection().get().equals("NORTH")) {
                location.setX(location.getBlockX() + .5);
            } else
                throw new IllegalStateException(
                        "Method Shop#getDoubleDirection() (ShopListener#findAdjacentOfSameType(Block)) returned a very unexpected result. " +
                                "Check plugin jar hash.");
        } else {
            location.setX(location.getBlockX() + .5d);
            location.setZ(location.getBlockZ() + .5d);
        }

        //item
        Location itemLoc = location.clone();
        itemLoc.setY(itemLoc.getBlockY() + 1.2d);
        location.getWorld().spawn(itemLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.getEquipment().setHelmet(new ItemStack(shop.getMaterial()));
            as.setSmall(true);
            as.setGravity(false);
            as.setInvulnerable(true);
        });
        //line1
        if (shop.getBuy() != -1) {
            Location line1Loc = location.clone();
            line1Loc.setY(line1Loc.getBlockY() + .2d);
            location.getWorld().spawn(line1Loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setSmall(true);
                as.setCustomNameVisible(true);
                as.setCustomName(String.format("%sBuy for %d", ChatColor.DARK_GREEN, shop.getBuy()));
                as.setGravity(false);
                as.setInvulnerable(true);
            });
        }
        //line2
        if (shop.getSell() != -1) {
            Location line2Loc = location.clone();
            line2Loc.setY(line2Loc.getBlockY() - .2d);
            location.getWorld().spawn(line2Loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setSmall(true);
                as.setCustomNameVisible(true);
                as.setCustomName(String.format("%sSell for %d", ChatColor.DARK_RED, shop.getSell()));
                as.setGravity(false);
                as.setInvulnerable(true);
            });
        }
    }

}
