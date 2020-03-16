package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import uk.tethys.survival.Survival;
import uk.tethys.survival.objects.Claim;
import uk.tethys.survival.tasks.ClaimTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimListener implements Listener {

    private final Survival plugin;

    public ClaimListener(Survival plugin) {
        this.plugin = plugin;
        claimCorners = new HashMap<>();
        particleTasks = new HashMap<>();
    }

    private final Map<UUID, Location> claimCorners;
    private final Map<UUID, Integer> particleTasks;

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack item = event.getItem();
        if (item != null && item.getItemMeta().getLocalizedName().equals("survival.items.tools.claim")) {
            if (!claimCorners.containsKey(uuid)) {
                Location corner = event.getClickedBlock().getLocation();

                claimCorners.put(uuid, corner);

                particleTasks.put(uuid, new ClaimTask.SustainParticle(player, 6 / 24D, corner)
                        .runTaskTimer(plugin, 0, 8).getTaskId());
            } else {
                Location corner1 = claimCorners.get(uuid);
                Location corner2 = event.getClickedBlock().getLocation();

                if (!particleTasks.containsKey(uuid))
                    throw new RuntimeException("Particle task not found for player " + uuid.toString());
                else
                    Bukkit.getScheduler().cancelTask(particleTasks.get(uuid));

                player.spawnParticle(Particle.NOTE, corner1.getBlockX() + .5, corner1.getBlockY() + 1.5,
                        corner1.getBlockZ() + .5, 0, 22 / 24D, 0, 0, 1);
                player.spawnParticle(Particle.NOTE, corner2.getBlockX() + .5, corner2.getBlockY() + 1.5,
                        corner2.getBlockZ() + .5, 0, 22 / 24D, 0, 0, 1);

                claimCorners.remove(uuid);

                // time to figure out claim size

                int lengthX = Math.abs(corner1.getBlockX() - corner2.getBlockX());
                int lengthZ = Math.abs(corner1.getBlockZ() - corner2.getBlockZ());
                int area = lengthX * lengthZ;

                // todo add config for this values
                if (area < 50) {
                    player.sendMessage("Claimed area must be greater than 50 blocks");
                    return;
                }

                plugin.getClaimManager().getClaims().add(new Claim(player, corner1, corner2));

                player.sendMessage("Claim created successfully!");

                player.sendMessage(plugin.getClaimManager().getClaims().toString());
            }
        }
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        Arrays.asList(event.getView().getTopInventory().getContents()).forEach(stack -> {
            if (stack != null && stack.getItemMeta() != null &&
                    stack.getItemMeta().getLocalizedName()
                            .equals("survival.items.tools.claim")) {
                event.getView().getTopInventory().removeItem(stack);
                event.getView().getBottomInventory().addItem(stack);
                ((Player) event.getView().getPlayer()).updateInventory();
                return;
            }
        });
    }

}
