package uk.tethys.survival.item;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import uk.tethys.survival.Survival;
import uk.tethys.survival.listeners.ClaimListener;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.*;

public class BuildersWandItem implements CustomItem {

    @Override
    public NamespacedKey getRecipeKey() {
        return new NamespacedKey(Survival.INSTANCE, "builders_wand");
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getRecipeKey(), getItem());

        recipe.shape("O", "S", "S");

        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('O', Material.NETHER_STAR);

        return recipe;
    }

    @Override
    public String getId() {
        return "builders_wand";
    }

    @Override
    public ItemStack getItem() throws RuntimeException {
        ItemStack item = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(CustomItems.KEY, PersistentDataType.STRING, getId());
        } else {
            throw new RuntimeException("Failed to get meta of newly created item (BUILDERS_WAND)");
        }

        meta.setDisplayName(ChatColor.YELLOW + "Builder's Wand");

        item.setItemMeta(meta);

        return item;
    }

    public static class PreviewTask implements Runnable {

        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack stack = player.getInventory().getItemInMainHand();

                ItemMeta meta = stack.getItemMeta();

                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();

                    if (pdc.has(CustomItems.KEY, PersistentDataType.STRING)) {

                        String id = pdc.get(CustomItems.KEY, PersistentDataType.STRING);

                        if (id.equals(CustomItems.BUILDERS_WAND.getId())) {
                            // is holding the builder's wand

                            Block targeted = player.getTargetBlock(null, 4); //todo what is default reach?

                            BlockFace facing = getLookingAt(player);

                            if (facing != null) {

                                List<Block> selection = scan(targeted.getRelative(facing), facing, player, targeted.getType());

                                selection.forEach(b -> {
                                    Location l = b.getLocation();
                                    l.setX(b.getLocation().getX() + .5);
                                    l.setZ(b.getLocation().getZ() + .5);
                                    l.setY(b.getLocation().getY() + .5);
                                    player.getWorld().spawnParticle(Particle.COMPOSTER, l, 1);
                                });


                            }
                        }
                    }
                }
            }
        }

        static int countInInventory(Inventory inventory, Material material) {
            int i = 0;

            for (Map.Entry<Integer, ? extends ItemStack> entry : inventory.all(material).entrySet())
                i += entry.getValue().getAmount();

            return i;
        }

    }

    public static BlockFace getLookingAt(Player player) {
        List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 4); //todo same reach issue
        if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding())
            return null;
        Block targetBlock = lastTwoTargetBlocks.get(1);
        Block adjacentBlock = lastTwoTargetBlocks.get(0);

        return targetBlock.getFace(adjacentBlock);
    }

    public static List<Block> scan(Block block, BlockFace face, Player player, Material material) {
        List<Block> blocks = new ArrayList<>();

        Queue<Block> blocksToCheck = new LinkedList<>();

        blocksToCheck.add(block);

        int maxSize = Math.min(
                50,
                player.getGameMode() == GameMode.CREATIVE ? Integer.MAX_VALUE : PreviewTask.countInInventory(player.getInventory(), material)
        );

        while (blocksToCheck.size() > 0 && blocks.size() < maxSize) {
            Block toCheck = blocksToCheck.remove();

            if (
                    toCheck.getType().equals(Material.AIR) &&
                            toCheck.getRelative(face.getOppositeFace()).getType().equals(material) &&
                            !blocks.contains(toCheck) &&
                            !ClaimListener.isDenied(player, toCheck.getLocation(), "BREAK")
            ) {
                if (!(toCheck.getLocation().distance(player.getLocation()) < 1))
                    blocks.add(toCheck);

                switch (face) {
                    case UP:
                    case DOWN:
                        blocksToCheck.add(toCheck.getRelative(BlockFace.EAST));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.WEST));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.NORTH));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.SOUTH));
                    case EAST:
                    case WEST:
                        blocksToCheck.add(toCheck.getRelative(BlockFace.UP));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.DOWN));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.NORTH));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.SOUTH));
                    case NORTH:
                    case SOUTH:
                        blocksToCheck.add(toCheck.getRelative(BlockFace.EAST));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.WEST));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.UP));
                        blocksToCheck.add(toCheck.getRelative(BlockFace.DOWN));
                }

            }
        }

        return blocks;
    }

}
