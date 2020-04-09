package uk.tethys.survival.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import uk.tethys.survival.util.SerializableLocation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Claim implements Serializable {

    private static long serialVersionUID = 1L;

    private Set<Flag> flags;
    private UUID owner;

    public UUID getOwner() {
        return owner;
    }

    public SerializableLocation getCorner1() {
        return corner1;
    }

    public SerializableLocation getCorner2() {
        return corner2;
    }

    private SerializableLocation corner1;
    private SerializableLocation corner2;

    public Claim(Player owner, Location corner1, Location corner2) {
        this(owner.getUniqueId(), corner1, corner2);
    }

    public Claim(UUID owner, Location corner1, Location corner2) {
        this.flags = Flag.DEFAULT_FLAGSET;
        this.owner = owner;
        this.corner1 = new SerializableLocation(corner1);
        this.corner2 = new SerializableLocation(corner2);
    }

    public enum Flag {

        ;

        @SuppressWarnings({"FieldCanBeLocal", "rawtypes"})
        private final Class type;

        <T> Flag(Class<T> type) {
            this.type = type;
        }

        public static final Set<Flag> DEFAULT_FLAGSET = new HashSet<>();
    }

    public boolean overlaps(Claim claim) {
        return this.overlapsX(claim) && this.overlapsZ(claim);
    }

    public boolean overlapsX(Claim claim) {
        SerializableLocation min1 = claim.corner1;
        SerializableLocation max1 = claim.corner2;
        SerializableLocation min2 = corner1;
        SerializableLocation max2 = corner2;
        return !(min1.getX() > max2.getX() && max1.getX() > min2.getX());
    }

    public boolean overlapsZ(Claim claim) {
        SerializableLocation min1 = claim.corner1;
        SerializableLocation max1 = claim.corner2;
        SerializableLocation min2 = corner1;
        SerializableLocation max2 = corner2;
        return !(min1.getZ() > max2.getZ() && max1.getZ() > min2.getZ());
    }

    public void tempSetCorners(Material material) {
        Bukkit.getWorld(this.corner1.getWorld()).getBlockAt(corner1.getLocation()).setType(material);
        Bukkit.getWorld(this.corner1.getWorld()).getBlockAt(corner2.getLocation()).setType(material);
    }

}
