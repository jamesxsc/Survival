package uk.tethys.survival.objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import uk.tethys.survival.Survival;
import uk.tethys.survival.util.SerializableLocation;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Claim implements Serializable {

    private static final long serialVersionUID = 1L;

    public static NamespacedKey IS_CLAIM_TOOL;
    public static NamespacedKey CLAIM_TOOL_MODE;
    public static NamespacedKey CLAIM_FLAG_NAME;
    public static NamespacedKey CLAIM_FLAG_AUTH_LEVEL;

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private final UUID owner;

    public UUID getOwner() {
        return owner;
    }

    public SerializableLocation getCorner1() {
        return corner1;
    }

    public SerializableLocation getCorner2() {
        return corner2;
    }

    private final SerializableLocation corner1;
    private final SerializableLocation corner2;

    public Claim(Player owner, Location corner1, Location corner2) {
        this(owner.getUniqueId(), corner1, corner2);
    }

    public Claim(UUID uniqueId, Location corner1, Location corner2) {
        this(uniqueId, new SerializableLocation(corner1), new SerializableLocation(corner2));
    }

    public Claim(UUID owner, SerializableLocation corner1, SerializableLocation corner2) {
        this.owner = owner;
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public Claim(UUID owner, SerializableLocation corner1, SerializableLocation corner2, int id) {
        this(owner, corner1, corner2);
        this.setId(id);
    }

    public static class AccessFlag {

        private final Flag flag;
        private final Flag.AuthLevel authLevel;
        private final boolean value;

        public AccessFlag(boolean value) {
            this(null, null, value);
        }

        public Flag getFlag() {
            return flag;
        }

        public Flag.AuthLevel getAuthLevel() {
            return authLevel;
        }

        public boolean getValue() {
            return value;
        }

        public AccessFlag(Flag flag, Flag.AuthLevel authLevel, boolean value) {
            this.flag = flag;
            this.authLevel = authLevel;
            this.value = value;
        }

    }

    public enum Flag {

        BREAK(BlockBreakEvent.class, null, Material.NETHERITE_PICKAXE, "Break Blocks", true, false, false),
        PLACE(BlockPlaceEvent.class, null, Material.BRICKS, "Place Blocks", true, false, false),
        // TODO input others
        ;

        private final Class<? extends Event> type;
        private final String data;
        private final Material icon;
        private final String displayName;
        private final boolean defaultPartner;
        private final boolean defaultLocal;
        private final boolean defaultWanderer;

        <T extends Event> Flag(Class<T> type, String data, Material icon, String displayName, boolean defaultPartner, boolean defaultLocal, boolean defaultWanderer) {
            this.type = type;
            this.data = data;
            this.icon = icon;
            this.displayName = displayName;
            this.defaultPartner = defaultPartner;
            this.defaultLocal = defaultLocal;
            this.defaultWanderer = defaultWanderer;
        }

        public Class<? extends Event> getType() {
            return type;
        }

        public String getData() {
            return data;
        }

        public Material getIcon() {
            return icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isDefaultPartner() {
            return defaultPartner;
        }

        public boolean isDefaultLocal() {
            return defaultLocal;
        }

        public boolean isDefaultWanderer() {
            return defaultWanderer;
        }

        public boolean isDefault(AuthLevel authLevel) {
            switch (authLevel) {
                case LOCAL:
                    return isDefaultLocal();
                case PARTNER:
                    return isDefaultPartner();
                case WANDERER:
                    return isDefaultWanderer();
            }
            throw new IllegalArgumentException("That AuthLevel does not have default values.");
        }

        public enum AuthLevel {
            LANDLORD(1),
            PARTNER(2),
            LOCAL(3),
            WANDERER(4),
            ;

            private final int id;

            AuthLevel(int id) {
                this.id = id;
            }

            public int getId() {
                return id;
            }
        }
    }

    public boolean overlaps(Claim claim) {
        return this.overlapsX(claim) && this.overlapsZ(claim);
    }

    public boolean overlapsX(Claim claim) {
        return !(claim.corner1.getX() > corner2.getX() && claim.corner2.getX() > corner1.getX());
    }

    public boolean overlapsZ(Claim claim) {
        return !(claim.corner1.getZ() > corner2.getZ() && claim.corner2.getZ() > corner1.getZ());
    }

    public Set<AccessFlag> getFlags() throws IllegalStateException {
        if (this.id == 0)
            throw new IllegalStateException("Claim#getFlags() cannot be used as id has not been set!");
        Set<AccessFlag> flags = new HashSet<>();
        try (ResultSet overlappingClaim = Survival.INSTANCE.getDBConnection().prepareStatement(String.format(
                "SELECT * FROM `claim_flags` WHERE `claim_id` = %d",
                this.id)).executeQuery()) {

            while (overlappingClaim.next()) {
                flags.add(new AccessFlag(Flag.valueOf(overlappingClaim.getString("flag")),
                        Flag.AuthLevel.valueOf(overlappingClaim.getString("auth_level")),
                        overlappingClaim.getBoolean("value")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB Error whilst getting flags", e);
        }
        return flags;
    }

    public void putFlag(AccessFlag flag) throws SQLException, IllegalStateException {
        if (this.id == 0)
            throw new IllegalStateException("Claim#putFlag() cannot be used as id has not been set!");

        try (Connection connection = Survival.INSTANCE.getDBConnection()) {
            try (ResultSet retrievedFlag = connection.prepareStatement(String.format(
                    "SELECT value FROM `claim_flags` WHERE `claim_id` = %d && `flag` = '%s' && `auth_level` = '%s'",
                    this.id,
                    flag.getFlag().name(),
                    flag.getAuthLevel().name()
            )).executeQuery()) {

                if (retrievedFlag.next()) {
                    if (retrievedFlag.getBoolean("value") != flag.getValue()) {
                        PreparedStatement st = connection.prepareStatement(String.format(
                                "UPDATE `claim_flags` SET `value` = %b WHERE `claim_id` = %d && `flag` = '%s' && `auth_level` = '%s'",
                                flag.getValue(),
                                this.id,
                                flag.getFlag().name(),
                                flag.getAuthLevel().name()
                        ));
                        st.execute();
                        st.close();
                    }
                } else {
                    PreparedStatement st = connection.prepareStatement(String.format(
                            "INSERT INTO `claim_flags` (`claim_id`, `flag`, `auth_level`, `value`) VALUES (%d, '%s', '%s', %b)",
                            this.id,
                            flag.getFlag().name(),
                            flag.getAuthLevel().name(),
                            flag.getValue()
                    ));
                    st.execute();
                    st.close();
                }
            }
        }
    }

    public Flag.AuthLevel getPlayerAuthLevel(Player player) throws SQLException, IllegalStateException {
        if (this.id == 0)
            throw new IllegalStateException("Claim#getPlayerAuthLevel() cannot be used as id has not been set!");

        if (player.getUniqueId().equals(this.owner)) {
            return Flag.AuthLevel.LANDLORD;
        }

        try (ResultSet resultSet = Survival.INSTANCE.getDBConnection().prepareStatement(String.format(
                "SELECT `auth_level` FROM `claim_access` WHERE `claim_id` = %d && `player` = '%s'",
                this.id,
                player.getUniqueId().toString())).executeQuery()) {
            if (resultSet.next()) {
                return Flag.AuthLevel.valueOf(resultSet.getString(1));
            } else {
                return Flag.AuthLevel.WANDERER;
            }
        }
    }

    public static Optional<Claim> getClaim(Location location) throws SQLException {
        ResultSet claim = null;
        try (Connection connection = Survival.INSTANCE.getDBConnection()) {
            claim = connection.prepareStatement(String.format(
                    "SELECT * FROM claims WHERE `world` = '%s' && ((`x1` <= %d && %d <= `x2`) && (`z1` <= %d && %d <= `z2`)) LIMIT 1",
                    Objects.requireNonNull(location.getWorld()).getUID().toString(),
                    location.getBlockX(),
                    location.getBlockX(),
                    location.getBlockZ(),
                    location.getBlockZ())).executeQuery();
            boolean inClaim = claim.next();
            if (inClaim)
                return Optional.of(new Claim(
                        UUID.fromString(claim.getString("owner")),
                        new SerializableLocation(
                                claim.getInt("x1"),
                                0,
                                claim.getInt("z1"),
                                UUID.fromString(claim.getString("world"))
                        ),
                        new SerializableLocation(
                                claim.getInt("x2"),
                                0,
                                claim.getInt("z2"),
                                UUID.fromString(claim.getString("world"))
                        ), claim.getInt("id")
                ));
            else
                return Optional.empty();
        } finally {
            if (claim != null) claim.close();
        }
    }

}
