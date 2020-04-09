package uk.tethys.survival.listeners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
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

                //todo WORLD CHECKS = CANCELLING! yes please

                claimCorners.put(uuid, corner);

                particleTasks.put(uuid, new ClaimTask.SustainParticle(player, 6 / 24D, corner)
                        .runTaskTimer(plugin, 0, 8).getTaskId());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.5f);
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

                // todo add const for this values
                if (area < 50) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, .7f);
                    player.sendMessage("Mission failed.");
                    player.sendMessage("Claimed area must be greater than 50 blocks");
                    return;
                }


                double new1X = Math.min(corner1.getX(), corner2.getX());
                double new1Z = Math.min(corner1.getZ(), corner2.getZ());
                double new2X = Math.max(corner1.getX(), corner2.getX());
                double new2Z = Math.max(corner1.getZ(), corner2.getZ());

                corner1.setX(new1X);
                corner1.setZ(new1Z);
//OHHHH
                corner2.setX(new2X);
                corner2.setZ(new2Z);

                Set<Claim> sameWorld = new HashSet<>();

                try (Connection connection = plugin.getDBConnection()) {
                    ResultSet overlappingClaims = connection.prepareStatement(String.format(
                            "SELECT `owner`, `x1`, `z1`, `x2`, `z2`, `world` FROM claims WHERE `world` = '%s'",
                            corner1.getWorld().getUID(), corner2.getBlockX(), corner1.getBlockX(), corner2.getBlockZ(),
                            corner1.getBlockZ())).executeQuery();

                    while (overlappingClaims.next()) {
                        sameWorld.add(new Claim(UUID.fromString(overlappingClaims.getString("owner")),
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
                    //todo player feeback
                    plugin.getLogger().severe("Error obtaining overlapping claims from DB");
                    e.printStackTrace();
                }

                Claim newClaim = new Claim(player, corner1, corner2);

                Set<Claim> overlapping = new HashSet<>();

                for (Claim c : sameWorld)
                    if (c.overlaps(newClaim) && newClaim.overlaps(c))
                        overlapping.add(c);

                double playerY = player.getLocation().getY();
                World world = Bukkit.getWorld(newClaim.getCorner1().getWorld());

                for (Claim c : overlapping) {
                    ChatColor color = ChatColor.values()[(int) (System.currentTimeMillis() % ChatColor.values().length)];
                    player.sendMessage(color + "Area already claimed by another player!");

                    Location c1 = c.getCorner1().getLocation();
                    Location c2 = c.getCorner2().getLocation();
                    c1.setY(playerY);
                    c2.setY(playerY);
                    Location c3 = new Location(world, c1.getX(), playerY, c2.getZ());
                    Location c4 = new Location(world, c2.getX(), playerY, c1.getZ());
                    // todo display claims
                    Set<Slime> corners = new HashSet<>();
                    corners.add(world.spawn(c1, Slime.class));
                    corners.add(world.spawn(c2, Slime.class));
                    corners.add(world.spawn(c3, Slime.class));
                    corners.add(world.spawn(c4, Slime.class));

                    Team team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(color + "svl.clm");
                    team.setColor(color);
                    for (Slime slime : corners) {
                        slime.setCollidable(false);
                        slime.setGravity(false);
                        slime.setSize(1);
                        slime.setAI(false);
                        team.addEntry(slime.getUniqueId().toString());
                        slime.setGlowing(true);
                        slime.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 2, true, false));

                        //todo eventhandle this
                        slime.setInvulnerable(true);
                    }

                }
                //todo temp
                if (overlapping.size() == 0) {
                    plugin.getClaimManager().addClaim(newClaim);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.7f);

                    player.sendMessage("Claim created successfully!");
                    return;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, .7f);
                player.sendMessage("Mission failed.");


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
