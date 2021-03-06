package uk.tethys.survival.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import uk.tethys.survival.Survival;
import uk.tethys.survival.message.Messages;
import uk.tethys.survival.objects.Claim;
import uk.tethys.survival.tasks.ClaimTask;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static uk.tethys.survival.listeners.DamageInternalEntityListener.INTERNAL_ENTITY;

public class ClaimListener implements Listener {

    private final Survival plugin;

    public ClaimListener(Survival plugin) {
        this.plugin = plugin;
        claimCorners = new HashMap<>();
        particleTasks = new HashMap<>();
    }

    private final Map<UUID, Location> claimCorners;
    private final Map<UUID, Integer> particleTasks;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getItemMeta() != null
                && (item.getItemMeta().getPersistentDataContainer().has(Claim.IS_CLAIM_TOOL, PersistentDataType.BYTE))
                && (item.getItemMeta().getPersistentDataContainer().get(Claim.IS_CLAIM_TOOL, PersistentDataType.BYTE) != null)
        ) {
            if (!item.getItemMeta().getPersistentDataContainer().has(Claim.CLAIM_TOOL_MODE, PersistentDataType.STRING))
                return;
            switch (item.getItemMeta().getPersistentDataContainer().get(Claim.CLAIM_TOOL_MODE, PersistentDataType.STRING)) {
                case "claim":
                    handleClaim(player, event.getClickedBlock(), action);
                    break;
                case "view":
                    break;
                case "flag":
                    handleFlag(player);
                    break;
                default:
                    return;
            }
            event.setCancelled(true);
        }
    }

    private static final LinkedList<String> modes = new LinkedList<String>() {{
        add("claim");
        add("view");
        add("flag");
    }};

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ItemStack itemStack = event.getItem();
            if (itemStack != null && itemStack.getItemMeta() != null) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta.getPersistentDataContainer().has(Claim.IS_CLAIM_TOOL, PersistentDataType.BYTE) &&
                        meta.getPersistentDataContainer().get(Claim.IS_CLAIM_TOOL, PersistentDataType.BYTE) == (byte) 1) {
                    String currentMode = meta.getPersistentDataContainer().get(Claim.CLAIM_TOOL_MODE, PersistentDataType.STRING);
                    String newMode;
                    int index = modes.indexOf(currentMode);
                    if (index + 1 == modes.size())
                        newMode = modes.get(0);
                    else
                        newMode = modes.get(index + 1);
                    meta.getPersistentDataContainer().set(Claim.CLAIM_TOOL_MODE, PersistentDataType.STRING, newMode);
                    itemStack.setItemMeta(meta);

                    event.setCancelled(true);

                    event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Messages.CLAIM_TOOL_MODE(newMode)));
                }
            }
        }
    }

    private void handleClaim(Player player, Block block, Action action) {
        if (action != Action.RIGHT_CLICK_BLOCK)
            return;

        UUID uuid = player.getUniqueId();
        if (!claimCorners.containsKey(uuid) || claimCorners.get(uuid).getWorld() != player.getWorld()) {
            Location corner = block.getLocation();
            claimCorners.put(uuid, corner);
            particleTasks.put(uuid, new ClaimTask.SustainParticle(player, 6 / 24D, corner)
                    .runTaskTimer(plugin, 0, 8).getTaskId());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.5f);
        } else {
            Location corner1 = claimCorners.get(uuid);
            Location corner2 = block.getLocation();

            if (!particleTasks.containsKey(uuid))
                throw new RuntimeException("Particle task not found for player " + uuid.toString());
            else
                Bukkit.getScheduler().cancelTask(particleTasks.get(uuid));

            player.spawnParticle(Particle.NOTE, corner1.getBlockX() + .5, corner1.getBlockY() + 1.5,
                    corner1.getBlockZ() + .5, 0, 22 / 24D, 0, 0, 1);
            player.spawnParticle(Particle.NOTE, corner2.getBlockX() + .5, corner2.getBlockY() + 1.5,
                    corner2.getBlockZ() + .5, 0, 22 / 24D, 0, 0, 1);

            claimCorners.remove(uuid);

            // figure out claim size
            int lengthX = Math.abs(corner1.getBlockX() - corner2.getBlockX());
            int lengthZ = Math.abs(corner1.getBlockZ() - corner2.getBlockZ());
            int area = lengthX * lengthZ;

            // todo add const for this values
            if (area < 50) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, .7f);
                player.sendMessage(Messages.CLAIM_AREA_SIZE(50, -1));
                return;
            }


            int new1X = Math.min(corner1.getBlockX(), corner2.getBlockX());
            int new1Z = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
            int new2X = Math.max(corner1.getBlockX(), corner2.getBlockX());
            int new2Z = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

            corner1.setX(new1X);
            corner1.setZ(new1Z);
            corner2.setX(new2X);
            corner2.setZ(new2Z);

            Set<Claim> sameWorld = new HashSet<>();

            try (ResultSet overlappingClaims = plugin.getDBConnection().prepareStatement(String.format(
                    "SELECT `owner`, `x1`, `z1`, `x2`, `z2`, `world` FROM claims WHERE `world` = '%s'",
                    corner1.getWorld().getUID())).executeQuery()) {
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
                player.sendMessage(Messages.DATABASE_ERROR("sql.claims", e.getMessage()));
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

                Location c1 = c.getCorner1().getLocation();
                c1.setX(c1.getBlockX() + .5);
                c1.setZ(c1.getBlockZ() + .5);
                Location c2 = c.getCorner2().getLocation();
                c2.setX(c2.getBlockX() + .5);
                c2.setZ(c2.getBlockZ() + .5);
                c1.setY(playerY);
                c2.setY(playerY);
                Location c3 = new Location(world, c1.getX(), playerY, c2.getZ());
                Location c4 = new Location(world, c2.getX(), playerY, c1.getZ());

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
                    slime.setInvulnerable(true);
                    slime.getPersistentDataContainer().set(INTERNAL_ENTITY, PersistentDataType.LONG, System.currentTimeMillis() + 30000);
                }

            }

            if (overlapping.size() == 0) {
                try {
                    plugin.getClaimManager().addClaim(newClaim);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.7f);
                    player.sendMessage(Messages.CLAIM_CREATE_SUCCESS);
                } catch (SQLException e) {
                    player.sendMessage(Messages.CLAIM_CREATE_FAIL);
                }
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, .7f);
                player.sendMessage(Messages.CLAIM_CREATE_FAIL);
            }
        }
    }

//    @Deprecated
//    @EventHandler
//    public void onDamageIndicator(EntityDamageEvent event) {
//        PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
//
//        if (pdc.has(Claim.CLAIM_SLIME_IDENTIFIER, PersistentDataType.BYTE)) {
//            event.setCancelled(true);
//        }
//    }

    private void handleView() {

    }

    private void handleFlag(Player player) {
        try {
            if (Claim.getClaim(player.getLocation()).isPresent()) {
                Inventory selectFlag = Bukkit.createInventory(null, 54, "Select Flag to Modify");
                selectFlag.setContents(getSelectFlagContents());
                player.openInventory(selectFlag);
            } else {
                player.sendMessage(Messages.NOT_IN_CLAIM);
            }
        } catch (SQLException e) {
            player.sendMessage(Messages.DATABASE_ERROR("sql.claims" + " " + "handleFlag", e.getMessage()));
        }
    }

    private ItemStack[] getSelectFlagContents() {
        ItemStack[] stacks = new ItemStack[54];
        int i = 9 * 2 + 2;
        for (Claim.Flag flag : Claim.Flag.values()) {
            ItemStack selectFlag = new ItemStack(flag.getIcon());
            ItemMeta meta = selectFlag.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + flag.getDisplayName());
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(Claim.CLAIM_FLAG_NAME, PersistentDataType.STRING, flag.name());
            selectFlag.setItemMeta(meta);

            if (i > 54)
                throw new RuntimeException("Too many flags exist!");

            if (i % 9 == 0)
                i += 2;

            stacks[i - 1] = selectFlag; // lgtm [java/index-out-of-bounds]

            i++;
        }

        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.setDisplayName(ChatColor.BLACK + "" + ChatColor.MAGIC + "xxx");
        fillMeta.setLocalizedName("survival.internal.fill");
        fill.setItemMeta(fillMeta);

        for (int j = 0; j < stacks.length; j++) {
            if (stacks[j] == null) {
                stacks[j] = fill;
            }
        }

        return stacks;
    }

    @EventHandler
    public void onSelectFlag(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        event.setCancelled(onSelectFlag(item, (Player) event.getWhoClicked()));
    }

    @EventHandler
    public void onSelectFlag(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        event.setCancelled(onSelectFlag(item, (Player) event.getInitiator().getViewers().get(0)));
    }

    private boolean onSelectFlag(ItemStack itemStack, Player player) {
        if (itemStack != null && itemStack.getItemMeta() != null) {
            ItemMeta meta = itemStack.getItemMeta();

            if (meta.getPersistentDataContainer().has(Claim.CLAIM_FLAG_NAME, PersistentDataType.STRING)) {

                final String flagName = meta.getPersistentDataContainer().get(Claim.CLAIM_FLAG_NAME, PersistentDataType.STRING);

                Optional<Claim> claimOptional = Optional.empty();
                try {
                    claimOptional = Claim.getClaim(player.getLocation());
                } catch (SQLException e) {
                    player.closeInventory();
                    player.sendMessage(Messages.DATABASE_ERROR("sql.claims" + " " + "onSelectFlag", e.getMessage()));
                }

                if (!claimOptional.isPresent())
                    return true;

                Claim claim = claimOptional.get();

                if (meta.getPersistentDataContainer().has(Claim.CLAIM_FLAG_AUTH_LEVEL, PersistentDataType.STRING)) {
                    final String authLevelName = meta.getPersistentDataContainer().get(Claim.CLAIM_FLAG_AUTH_LEVEL, PersistentDataType.STRING);

                    Claim.Flag.AuthLevel authLevel = Claim.Flag.AuthLevel.valueOf(authLevelName);

                    try {
                        claim.putFlag(new Claim.AccessFlag(
                                Claim.Flag.valueOf(flagName),
                                authLevel,
                                !(claim.getFlags().stream().filter(
                                        af -> af.getFlag().name().equals(flagName)
                                                && af.getAuthLevel() == authLevel).findFirst()
                                        .orElseGet(() -> new Claim.AccessFlag(Claim.Flag.valueOf(flagName)
                                                .isDefault(authLevel))).getValue())
                        ));

                    } catch (SQLException e) {
                        player.closeInventory();
                        e.printStackTrace();
                        player.sendMessage(Messages.DATABASE_ERROR("sql.claims" + " " + "toggleFlag", e.getMessage()));
                    }
                }

                Inventory modifyFlag = Bukkit.createInventory(null, 54, "Modify Flag - " + Claim.Flag.valueOf(flagName).getDisplayName());

                ItemStack[] contents = new ItemStack[54];
                ItemStack partner = new ItemStack(Material.CHAIN);
                ItemMeta partnerMeta = partner.getItemMeta();

                partnerMeta.setDisplayName(ChatColor.RESET + "Partner");
                boolean partnerAllowed = claim.getFlags().stream().filter(
                        af -> af.getFlag().name().equals(flagName)
                                && af.getAuthLevel() == Claim.Flag.AuthLevel.PARTNER).findFirst()
                        .orElseGet(() -> new Claim.AccessFlag(Claim.Flag.valueOf(flagName).isDefaultPartner())).getValue();
                PersistentDataContainer partnerPDC = partnerMeta.getPersistentDataContainer();
                partnerPDC.set(Claim.CLAIM_FLAG_NAME, PersistentDataType.STRING, flagName);
                partnerPDC.set(Claim.CLAIM_FLAG_AUTH_LEVEL, PersistentDataType.STRING, "PARTNER");
                partnerMeta.setLore(Arrays.asList("Currently " + (partnerAllowed ? "allowed" : "denied"), "Click to toggle"));

                partner.setItemMeta(partnerMeta);
                contents[9 * 2 + 3] = partner;
                ItemStack local = new ItemStack(Material.SOUL_CAMPFIRE);
                ItemMeta localMeta = local.getItemMeta();

                localMeta.setDisplayName(ChatColor.RESET + "Local");
                boolean localAllowed = claim.getFlags().stream().filter(
                        af -> af.getFlag().name().equals(flagName)
                                && af.getAuthLevel() == Claim.Flag.AuthLevel.LOCAL).findFirst()
                        .orElseGet(() -> new Claim.AccessFlag(Claim.Flag.valueOf(flagName).isDefaultLocal())).getValue();
                PersistentDataContainer localPDC = localMeta.getPersistentDataContainer();
                localPDC.set(Claim.CLAIM_FLAG_NAME, PersistentDataType.STRING, flagName);
                localPDC.set(Claim.CLAIM_FLAG_AUTH_LEVEL, PersistentDataType.STRING, "LOCAL");
                localMeta.setLore(Arrays.asList("Currently " + (localAllowed ? "allowed" : "denied"), "Click to toggle"));


                local.setItemMeta(localMeta);
                contents[9 * 2 + 4] = local;
                ItemStack wanderer = new ItemStack(Material.LEATHER_BOOTS);
                ItemMeta wandererMeta = wanderer.getItemMeta();

                wandererMeta.setDisplayName(ChatColor.RESET + "Wanderer");
                boolean wandererAllowed = claim.getFlags().stream().filter(
                        af -> af.getFlag().name().equals(flagName)
                                && af.getAuthLevel() == Claim.Flag.AuthLevel.WANDERER).findFirst()
                        .orElseGet(() -> new Claim.AccessFlag(Claim.Flag.valueOf(flagName).isDefaultWanderer())).getValue();
                PersistentDataContainer wandererPDC = wandererMeta.getPersistentDataContainer();
                wandererPDC.set(Claim.CLAIM_FLAG_NAME, PersistentDataType.STRING, flagName);
                wandererPDC.set(Claim.CLAIM_FLAG_AUTH_LEVEL, PersistentDataType.STRING, "WANDERER");
                wandererMeta.setLore(Arrays.asList("Currently " + (wandererAllowed ? "allowed" : "denied"), "Click to toggle"));

                wanderer.setItemMeta(wandererMeta);
                contents[9 * 2 + 5] = wanderer;

                ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta fillMeta = fill.getItemMeta();
                fillMeta.setDisplayName(ChatColor.BLACK + "" + ChatColor.MAGIC + "xxx");
                fillMeta.setLocalizedName("survival.internal.fill");
                fill.setItemMeta(fillMeta);

                for (int j = 0; j < contents.length; j++) {
                    if (contents[j] == null) {
                        contents[j] = fill;
                    }
                }

                modifyFlag.setContents(contents);

                player.openInventory(modifyFlag);

                return true;
            }
        }
        return false;
    }


    //todo generified tool prevention!! nbt!
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(PrepareItemCraftEvent event) {
        Arrays.asList(event.getView().getTopInventory().getContents()).forEach(stack -> {
            if (stack != null && stack.getItemMeta() != null &&
                    stack.getItemMeta().getLocalizedName()
                            .equals("survival.items.tools.claim")) {
                event.getView().getTopInventory().removeItem(stack);
                event.getView().getBottomInventory().addItem(stack);
                ((Player) event.getView().getPlayer()).updateInventory();
            }
        });
    }

    // check for unauthorised breaking of blocks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getLocation(), "BREAK")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised placing of blocks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (isDenied(event.getPlayer(), event.getBlockPlaced().getLocation(), "PLACE")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised container usage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getState() instanceof Container &&
                isDenied(event.getPlayer(), event.getClickedBlock().getLocation(), "OPEN_CONTAINER")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised redstone usage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstoneUse(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null &&
                event.getClickedBlock().getBlockData() instanceof Powerable &&
                isDenied(event.getPlayer(), event.getClickedBlock().getLocation(), "REDSTONE")) {
            event.setCancelled(true);
        }
    }

    // prevent unauthorised triggering of raids
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(RaidTriggerEvent event) {
        if (isDenied(event.getPlayer(), event.getRaid().getLocation(), "TRIGGER_RAID")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised vehicle destruction
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player &&
                isDenied((Player) event.getAttacker(), event.getVehicle().getLocation(), "DESTROY_VEHICLE")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised vehicle usage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player &&
                isDenied((Player) event.getEntered(), event.getVehicle().getLocation(), "ENTER_VEHICLE")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised entity damaging
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player &&
                isDenied((Player) event.getDamager(), event.getEntity().getLocation(), "DAMAGE_ENTITY")) {
            event.setCancelled(true);
        }
    }

    // check for book robbery
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTake(PlayerTakeLecternBookEvent event) {
        if (isDenied(event.getPlayer(), event.getLectern().getLocation(), "TAKE_LECTERN_BOOK")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised bed usage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnter(PlayerBedEnterEvent event) {
        if (isDenied(event.getPlayer(), event.getBed().getLocation(), "USE_BED")) {
            event.setCancelled(true);
        }
    }

    // check for fluid robbery
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFill(PlayerBucketFillEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getLocation(), "FILL_BUCKETS")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised entity interactions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (isDenied(event.getPlayer(), event.getRightClicked().getLocation(), "INTERACT_ENTITY")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised leashing
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (isDenied(event.getPlayer(), event.getEntity().getLocation(), "LEASH_ENTITY")) {
            event.setCancelled(true);
        }
    }

    // check for unauthorised shearing
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShear(PlayerShearEntityEvent event) {
        if (isDenied(event.getPlayer(), event.getEntity().getLocation(), "SHEAR_ENTITY")) {
            event.setCancelled(true);
        }
    }

    // check for arson
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArson(PlayerInteractEvent event) {
        if (event.getItem() != null) {
            Material itemType = event.getItem().getType();

            if (itemType == Material.FLINT_AND_STEEL || itemType == Material.FIRE_CHARGE) {
                if (isDenied(event.getPlayer(), event.getClickedBlock() == null ? event.getPlayer().getLocation() : event.getClickedBlock().getLocation(), "IGNITE")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // TODO check for nearby explosions
    @EventHandler
    public void onPrime(ExplosionPrimeEvent event) {
        // todo find igniter here
    }

    // check for unauthorised launching of fireworks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLaunch(PlayerInteractEvent event) {
        if (event.getItem() != null) {
            Material itemType = event.getItem().getType();

            if (itemType == Material.FIREWORK_ROCKET) {
                if (isDenied(event.getPlayer(), event.getClickedBlock() == null ? event.getPlayer().getLocation() : event.getClickedBlock().getLocation(), "FIREWORK")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // fallback interact catch
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGenericInteract(PlayerInteractEvent event) {
        // todo we will use this to catch exceptions found in beta testing should they not warrant their own method
    }

    public static boolean isDenied(Player player, Location location, String flagName) {
        Optional<Claim> claimOptional;
        try {
            claimOptional = Claim.getClaim(location);
        } catch (SQLException e) {
            player.sendMessage(Messages.DENIED_DUE_TO_DB_FAIL);
            Survival.INSTANCE.getLogger().severe("Error obtaining player auth level from DB");
            e.printStackTrace();
            // ensure that if there's an issue we don't allow griefing
            return true;
        }

        if (claimOptional.isPresent()) {

            // if the player owns the claim they have access
            if (claimOptional.get().getOwner().equals(player.getUniqueId()))
                return false;

            Claim.Flag.AuthLevel authLevel;
            try {
                authLevel = claimOptional.get().getPlayerAuthLevel(player);
            } catch (SQLException e) {
                player.sendMessage(Messages.DENIED_DUE_TO_DB_FAIL);
                Survival.INSTANCE.getLogger().severe("Error obtaining player auth level from DB");
                e.printStackTrace();
                // ensure that if there's an issue we don't allow griefing
                return true;
            }

            // search db flag overrides
            for (Claim.AccessFlag flag : claimOptional.get().getFlags()) {
                if (flag.getFlag().name().equals(flagName) && flag.getAuthLevel().equals(authLevel)) {
                    return !flag.getValue();
                }
            }

            // if the flag is not stored we need to use the default
            return !Claim.Flag.valueOf(flagName).isDefault(authLevel);
        } else {
            return false;
        }
    }

}
