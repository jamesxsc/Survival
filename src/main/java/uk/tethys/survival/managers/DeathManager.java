package uk.tethys.survival.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import uk.tethys.survival.Survival;
import uk.tethys.survival.util.InventorySerializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DeathManager {

    private final Survival plugin;
    private final Set<Death> deaths;

    public DeathManager(Survival plugin) {
        this.plugin = plugin;
        deaths = new HashSet<>();
    }

    public Set<Death> getDeaths() {
        return deaths;
    }

    public boolean storeDeath(ItemStack[] inv, ItemStack[] armor, int exp, UUID player, long timestamp) {
        return this.deaths.add(new Death(inv, armor, exp, player, timestamp));
    }

    public Optional<Death> getLatestDeath(Player player) {
        return this.getLatestDeath(player.getUniqueId());
    }

    public Optional<Death> getLatestDeath(UUID player) {
        // TODO may need inverting
        return this.deaths.stream().filter((d) -> d.getPlayer().equals(player)).min((o1, o2) -> (int) (o1.getTimestamp() - o2.getTimestamp()));
    }

    public boolean summonGrave(Player player) {
        // we begin by retrieving data about the death
        Location deathLoc;
        ItemStack[] inv;
        ItemStack[] armor;
        int exp;
        long millis;
        try (Connection connection = Survival.INSTANCE.getDBConnection()) {
            ResultSet rs = connection.prepareStatement(String.format("SELECT * FROM `inventory_cache` WHERE `holder` = '%s'", player.getUniqueId().toString())).executeQuery();
            if (rs.next()) {
                deathLoc = new Location(Bukkit.getWorld(UUID.fromString(rs.getString("world"))), rs.getInt("x"), rs.getInt("y"), rs.getInt("x"));
                inv = InventorySerializer.itemStackArrayFromBase64(rs.getString("contents"));
                armor = InventorySerializer.itemStackArrayFromBase64(rs.getString("armor"));
                exp = rs.getInt("exp");
                millis = rs.getTimestamp("last_stored").getTime();
            } else return false;
        } catch (SQLException | IOException ex) {
            // catch db exception or any other weird error whilst decoding base64
            return false;
        }

        // ensure we are not replacing any other block with the grave as this could open griefing exploits
        int newHeight = deathLoc.getWorld().getHighestBlockYAt(deathLoc) + 1;
        while (newHeight > 256) {
            // todo implement omnidirectional scan rather than just x+
            deathLoc.setX(deathLoc.getX() + 1);
            newHeight = deathLoc.getWorld().getHighestBlockYAt(deathLoc) + 1;
        }
        deathLoc.setY(newHeight);

        // now we can actually summon the grave
        deathLoc.getBlock().setType(Material.CHEST);
        deathLoc.getBlock().setMetadata("is_grave", new FixedMetadataValue(plugin, true));
        deathLoc.getBlock().setMetadata("grave_owner", new FixedMetadataValue(plugin, player.getUniqueId()));
        deathLoc.getBlock().setMetadata("grave_contents", new FixedMetadataValue(plugin, inv));
        deathLoc.getBlock().setMetadata("grave_armor", new FixedMetadataValue(plugin, armor));
        deathLoc.getBlock().setMetadata("grave_exp", new FixedMetadataValue(plugin, exp));
        deathLoc.getBlock().setMetadata("grave_millis", new FixedMetadataValue(plugin, millis));

        return true;
        // todo listen for right click on this chest then return contents
    }

    public static class Death {

        private final ItemStack[] inv;
        private final ItemStack[] armor;
        private final int exp;
        private final UUID player;
        private final long timestamp;

        private Death(ItemStack[] inv, ItemStack[] armor, int exp, UUID player, long timestamp) {
            this.inv = inv;
            this.armor = armor;
            this.exp = exp;
            this.player = player;
            this.timestamp = timestamp;
        }

        public ItemStack[] getInv() {
            return inv;
        }

        public ItemStack[] getArmor() {
            return armor;
        }

        public int getExp() {
            return exp;
        }

        public UUID getPlayer() {
            return player;
        }

        public long getTimestamp() {
            return timestamp;
        }

    }

}
