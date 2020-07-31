package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;
import uk.tethys.survival.objects.Shop;
import uk.tethys.survival.tasks.CreateShopDisplayTask;
import uk.tethys.survival.util.Pair;
import uk.tethys.survival.util.SerializableLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CreateShopListener implements Listener {

    private final Survival plugin;

    public CreateShopListener(Survival plugin) {
        this.plugin = plugin;

        this.awaitingBuyPrice = new HashMap<>();
        this.awaitingSellPrice = new HashMap<>();
    }

    private final Map<UUID, Shop> awaitingBuyPrice;
    private final Map<UUID, Shop> awaitingSellPrice;

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;
        if (event.getClickedBlock().getType() != Material.CHEST
                && event.getClickedBlock().getType() != Material.TRAPPED_CHEST)
            return;
        if (event.getItem() == null || !event.getItem().getItemMeta().getLocalizedName()
                .equals("survival.items.tools.shop"))
            return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation().clone();
        location.setX(location.getBlockX() + .5d);
        location.setZ(location.getBlockZ() + .5d);

        Inventory selector = Bukkit.createInventory(player, InventoryType.HOPPER,
                ChatColor.DARK_AQUA + "Insert Item for Shop");
        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.setDisplayName(ChatColor.BLACK + "" + ChatColor.MAGIC + "xxx");
        fillMeta.setLocalizedName("survival.internal.fill");
        fill.setItemMeta(fillMeta);
        selector.setItem(0, fill);
        selector.setItem(1, fill);
        selector.setItem(3, fill);
        selector.setItem(4, fill);
        player.openInventory(selector);
    }
//
//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onInsert(InventoryMoveItemEvent event) {
//        Player player = (Player) event.getInitiator().getViewers().get(0);
//        if (!player.getOpenInventory().getTitle().equals(ChatColor.DARK_AQUA + "Insert Item for Shop"))
//            return;
//        // todo generify this
//        if (event.getItem().getItemMeta() != null && event.getItem().getItemMeta().getLocalizedName().equals("survival.internal.fill")) {
//            event.setCancelled(true);
//            return;
//        }
//        if (event.getDestination().getType().equals(InventoryType.HOPPER)) {
//
//            Material shopItem = event.getItem().getType();
//            Block chest = player.getTargetBlock(null, 5);
//
//            String direction = null;
//            if (((Chest) chest.getState()).getInventory() instanceof DoubleChestInventory) {
//                Optional<Pair<Block, String>> adj = findAdjacentOfSameType(chest);
//                if (!adj.isPresent()) {
//                    throw new IllegalStateException("Chest is double but there is no adjacent chest!");
//                }
//                direction = adj.get().getB();
//            }
//
//            Shop shop = new Shop();
//            shop.setOwner(player.getUniqueId());
//            shop.setLocation(new SerializableLocation(chest.getLocation()));
//            shop.setMaterial(shopItem);
//            shop.setDoubleDirection(direction == null ? Optional.empty() : Optional.of(direction));
//
//            event.setCancelled(true);
//            player.updateInventory();
//            player.closeInventory();
//            player.sendMessage(Messages.BUY_PRICE_PROMPT);
//            awaitingBuyPrice.put(shop.getOwner(), shop);
//        }
//    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInsert(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().equals(ChatColor.DARK_AQUA + "Insert Item for Shop"))
            return;
        if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null
                && event.getCurrentItem().getItemMeta().getLocalizedName().equals("survival.internal.fill")) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction().equals(InventoryAction.PLACE_ONE)
                || event.getAction().equals(InventoryAction.PLACE_ALL)
                || event.getAction().equals(InventoryAction.HOTBAR_SWAP) || (
                event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                        && event.getClickedInventory() != null
                        && event.getClickedInventory().getType().equals(InventoryType.PLAYER)
        )) {

            Material shopItem = (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
                    ? event.getCursor().getType() : event.getCurrentItem().getType();
            Block chest = player.getTargetBlock(null, 5);

            String direction = null;
            if (((Chest) chest.getState()).getInventory() instanceof DoubleChestInventory) {
                Optional<Pair<Block, String>> adj = findAdjacentOfSameType(chest);
                if (!adj.isPresent()) {
                    throw new IllegalStateException("Chest is double but there is no adjacent chest!");
                }
                direction = adj.get().getB();
            }

            Shop shop = new Shop();
            shop.setOwner(player.getUniqueId());
            shop.setLocation(new SerializableLocation(chest.getLocation()));
            shop.setMaterial(shopItem);
            shop.setDoubleDirection(direction == null ? Optional.empty() : Optional.of(direction));

            event.setCancelled(true);
            player.updateInventory();
            player.closeInventory();
            player.sendMessage(Messages.BUY_PRICE_PROMPT);
            awaitingBuyPrice.put(shop.getOwner(), shop);
        }
    }

    //-1 = cant buy / cant sell
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (awaitingBuyPrice.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            if (message.toLowerCase().contains("exit")) {
                player.sendMessage(Messages.OPERATION_CANCELLED);
                awaitingBuyPrice.remove(player.getUniqueId());
                return;
            }
            int price = 0;
            try {
                price = Integer.parseInt(message);
            } catch (NumberFormatException e) {
                player.sendMessage(Messages.NFE_LOOP_OR_EXIT);
                return;
            }
            Shop shop = awaitingBuyPrice.get(player.getUniqueId());
            shop.setBuy(price == -1 ? price : Math.abs(price));
            player.sendMessage(Messages.SELL_PRICE_PROMPT);
            awaitingSellPrice.put(player.getUniqueId(), shop);
            awaitingBuyPrice.remove(player.getUniqueId());
        } else if (awaitingSellPrice.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            if (message.toLowerCase().contains("exit")) {
                player.sendMessage(Messages.OPERATION_CANCELLED);
                awaitingSellPrice.remove(player.getUniqueId());
                return;
            }
            int price = 0;
            try {
                price = Integer.parseInt(message);
            } catch (NumberFormatException e) {
                player.sendMessage(Messages.NFE_LOOP_OR_EXIT);
                return;
            }

            Shop shop = awaitingSellPrice.get(player.getUniqueId());
            if (shop.getBuy() == -1 && price == -1) {
                player.sendMessage(Messages.NO_BUY_OR_SELL);
                return;
            }
            shop.setSell(price == -1 ? price : Math.abs(price));
            awaitingSellPrice.remove(player.getUniqueId());

            player.sendMessage(Messages.CREATING_SHOP);
            new CreateShopDisplayTask(shop).runTask(plugin);
        }
    }

    private Optional<Pair<Block, String>> findAdjacentOfSameType(Block block) {
        Material type = block.getType();
        Location first = block.getLocation();
        Location adj = first.clone();
        adj.setX(adj.getX() - 1);
        if (adj.getBlock().getType() == type)
            return Optional.of(new Pair<Block, String>(adj.getBlock(), "WEST"));
        adj.setX(adj.getX() + 2);
        if (adj.getBlock().getType() == type)
            return Optional.of(new Pair<Block, String>(adj.getBlock(), "EAST"));
        adj.setX(adj.getX() - 1);
        adj.setZ(adj.getZ() + 1);
        if (adj.getBlock().getType() == type)
            return Optional.of(new Pair<Block, String>(adj.getBlock(), "SOUTH"));
        adj.setZ(adj.getZ() - 2);
        if (adj.getBlock().getType() == type)
            return Optional.of(new Pair<Block, String>(adj.getBlock(), "NORTH"));
        return Optional.empty();
    }

}
