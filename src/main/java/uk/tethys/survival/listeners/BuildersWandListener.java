package uk.tethys.survival.listeners;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import uk.tethys.survival.item.BuildersWandItem;
import uk.tethys.survival.item.CustomItems;

import java.util.List;

import static uk.tethys.survival.item.BuildersWandItem.getLookingAt;

public class BuildersWandListener implements Listener {

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        if (event.getItem() != null
                && event.getItem().getItemMeta() != null
                && event.getItem().getItemMeta().getPersistentDataContainer().has(CustomItems.KEY, PersistentDataType.STRING)
                && event.getItem().getItemMeta().getPersistentDataContainer().get(CustomItems.KEY, PersistentDataType.STRING).equals(CustomItems.BUILDERS_WAND.getId())) {

            event.setCancelled(true);

            Block clicked = event.getClickedBlock();

            if (clicked == null)
                return;

            BlockData clickedData = clicked.getBlockData();
            Player player = event.getPlayer();
            BlockFace facing = getLookingAt(player);

            if (clicked == null) return;

            List<Block> selection = BuildersWandItem.scan(clicked.getRelative(facing), facing, player, clicked.getType());

            for (Block block : selection) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    ItemStack toDecr = player.getInventory().getItem(player.getInventory().first(clicked.getType()));
                    if (toDecr == null)
                        break;
                    toDecr.setAmount(toDecr.getAmount() - 1);
                }
                block.setType(clicked.getType());
                if (clickedData instanceof Directional) {
                    BlockData blockData = block.getBlockData();
                    ((Directional) blockData).setFacing(((Directional) clickedData).getFacing());
                    block.setBlockData(blockData);
                }
            }
        }
    }

}
