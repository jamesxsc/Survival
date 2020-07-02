package uk.tethys.survival.listeners;

import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
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
                corner2.setX(new2X);
                corner2.setZ(new2Z);

                Set<Claim> sameWorld = new HashSet<>();

                try (Connection connection = plugin.getDBConnection()) {
                    ResultSet overlappingClaims = connection.prepareStatement(String.format(
                            "SELECT `owner`, `x1`, `z1`, `x2`, `z2`, `world` FROM claims WHERE `world` = '%s'",
                            corner1.getWorld().getUID())).executeQuery();

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

    // check for unauthorised breaking of blocks
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getLocation(), BlockBreakEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised placing of blocks
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.SHULKER_BOX) {
            //todo shulker box logic
        } else {
            if (isDenied(event.getPlayer(), event.getBlockAgainst().getLocation(), BlockPlaceEvent.class)) {
                event.setCancelled(true);
            }
        }
    }

    // check for unauthorised container usage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getState() instanceof Container &&
                isDenied(event.getPlayer(), event.getClickedBlock().getLocation(), PlayerInteractEvent.class)) { // todo do we need to further refine queries of this event to seperate permissions between eg containers and redstone?
            event.setCancelled(true);
        }
    }

    // check for unauthorised redstone usage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstoneUse(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getBlockData() instanceof Powerable &&
                isDenied(event.getPlayer(), event.getClickedBlock().getLocation(), PlayerInteractEvent.class)) {
            event.setCancelled(true);
        }
    }

    // prevent unauthorised triggering of raids
    @EventHandler
    public void onBreak(RaidTriggerEvent event) {
        if (isDenied(event.getPlayer(), event.getRaid().getLocation(), RaidTriggerEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised vehicle destruction
    @EventHandler
    public void onDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player &&
                isDenied((Player) event.getAttacker(), event.getVehicle().getLocation(), VehicleDestroyEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised vehicle usage
    @EventHandler
    public void onEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player &&
                isDenied((Player) event.getEntered(), event.getVehicle().getLocation(), VehicleEnterEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised entity damaging
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player &&
                isDenied((Player) event.getDamager(), event.getEntity().getLocation(), EntityDamageByEntityEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for book robbery
    @EventHandler
    public void onTake(PlayerTakeLecternBookEvent event) {
        if (isDenied(event.getPlayer(), event.getLectern().getLocation(), PlayerTakeLecternBookEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised bed usage
    @EventHandler
    public void onEnter(PlayerBedEnterEvent event) {
        if (isDenied(event.getPlayer(), event.getBed().getLocation(), PlayerBedEnterEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for fluid robbery
    @EventHandler
    public void onFill(PlayerBucketFillEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getLocation(), PlayerBucketFillEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised entity interactions
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (isDenied(event.getPlayer(), event.getRightClicked().getLocation(), PlayerInteractEntityEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised leashing
    @EventHandler
    public void onLeash(PlayerLeashEntityEvent event) {
        if (isDenied(event.getPlayer(), event.getEntity().getLocation(), PlayerLeashEntityEvent.class)) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised shearing
    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {
        if (isDenied(event.getPlayer(), event.getEntity().getLocation(), PlayerShearEntityEvent.class)) {
            event.setCancelled(true);
        }
    }

    // fallback interact catch
    @EventHandler
    public void onGenericInteract(PlayerInteractEvent event) {
        // todo we will use this to catch exceptions found in beta testing should they not warrant their own method
    }

    private boolean isDenied(Player player, Location location, Class<? extends Event> eventType) {

        try (Connection connection = plugin.getDBConnection()) {
            ResultSet overlappingClaim = connection.prepareStatement(String.format(
                    "SELECT `owner`, `id` FROM claims WHERE `world` = '%s' && ((`x1` <= %d && %d <= `x2`) && (`z1` <= %d && %d <= `z2`)) LIMIT 1",
                    Objects.requireNonNull(location.getWorld()).getUID().toString(),
                    location.getBlockX(),
                    location.getBlockX(),
                    location.getBlockZ(),
                    location.getBlockZ()
            )).executeQuery();
            boolean inClaim = overlappingClaim.next();
            if (inClaim)
                player.sendMessage(ChatColor.AQUA + "Rejected by system. With EventType of " + eventType.getCanonicalName() + ChatColor.LIGHT_PURPLE + " In claim owned by " + overlappingClaim.getString("owner"));
            return inClaim;
        } catch (SQLException e) {
            //todo player feedback
            plugin.getLogger().severe("Error obtaining overlapping claim from DB");
            e.printStackTrace();
            // ensure that if there's an issue we don't allow griefing
            return true;
        }
        //todo flags
    }

}
