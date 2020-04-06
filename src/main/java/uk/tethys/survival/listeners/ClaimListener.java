package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
        if (item != null && item.getItemMeta() != null
                && item.getItemMeta().getLocalizedName().equals("survival.items.tools.claim")) {
            if (!claimCorners.containsKey(uuid)) {
                Location corner = event.getClickedBlock().getLocation();

                //todo WORLD CHECKS = CANCELLING!

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
                    //TODO UNCOMMENT THIS FOR TESTING ONLY!!!!
                   // return;
                }

                corner1.setX(Math.min(corner1.getX(), corner2.getX()));
                corner1.setZ(Math.min(corner1.getZ(), corner2.getZ()));

                corner2.setX(Math.max(corner1.getX(), corner2.getX()));
                corner2.setZ(Math.max(corner1.getZ(), corner2.getZ()));

                //!(x1 > newx2 or newx1 > x2) and !(z1 > newz2 or nez1 > z2)
                //!(nmin.getX() > max.getX() || nmin.getZ() > max.getZ() ||
                //                        min.getX() > nmax.getX() || min.getZ() > nmax.getZ()

                Set<Claim> overlapping = new HashSet<>();

                try (Connection connection = plugin.getDBConnection()) {
                    ResultSet overlappingClaims = connection.prepareStatement(String.format(
                            "SELECT `owner`, `x1`, `z1`, `x2`, `z2`, `world` FROM claims WHERE `world` = '%s' /*AND " +
                                    "((NOT (`x1` > %d OR %d > `x2`)) AND (NOT (`z1` > %d or %d > `z2`)))*/",
                            corner1.getWorld().getUID(), corner2.getBlockX(), corner1.getBlockX(), corner2.getBlockZ(),
                            corner1.getBlockZ())).executeQuery();

                    while (overlappingClaims.next()) {
                        overlapping.add(new Claim(UUID.fromString(overlappingClaims.getString("owner")),
                                new Location(
                                        Bukkit.getWorld(UUID.fromString(overlappingClaims.getString("world"))),
                                        overlappingClaims.getInt("x1"), 128,
                                        overlappingClaims.getInt("z1")
                                ),
                                new Location(
                                        Bukkit.getWorld(UUID.fromString(overlappingClaims.getString("world"))),
                                        overlappingClaims.getInt("x2"), 128,
                                        overlappingClaims.getInt("z2")
                                )));
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error obtaining overlapping claims from DB");
                    e.printStackTrace();
                }

                for (Claim c : overlapping) {

                    boolean inz, inx;
                    if ((c.getCorner2().getX() < corner1.getBlockX() || c.getCorner1().getX() > corner2.getBlockX())) {
                        Bukkit.broadcastMessage("not in x");
                        inx = false;
                    } else {
                        Bukkit.broadcastMessage("in x span");
                        inx = true;
                    }

                    //swap smallest to be oldrect

                    if ((c.getCorner2().getZ() < corner1.getBlockZ() || c.getCorner1().getZ() > corner2.getBlockZ())) {
                        Bukkit.broadcastMessage("not in z");
                        inz = false;
                    } else {
                        Bukkit.broadcastMessage("in z span");
                        inz = true;
                    }

                    if (inz && inx)
                        player.sendMessage("WOW ITS ALREADY A THING!!!!");

                }

                plugin.getClaimManager().addClaim(new Claim(player, corner1, corner2));

                player.sendMessage("Claim created successfully!");
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
