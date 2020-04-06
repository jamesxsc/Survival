package uk.tethys.survival.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

public class SerializableLocation implements Serializable {

    private static long serialVersionUID = 2L;

    private double  x,
                    y,
                    z;

    private UUID world;

    public SerializableLocation(double x, double y, double z, UUID world) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.world = world;
    }

    public SerializableLocation(Location l) {
        this.x = l.getBlockX();
        this.y = l.getBlockY();
        this.z = l.getBlockZ();

        this.world = l.getWorld().getUID();
    }

    public Location getLocation() {
        return new Location(Bukkit.getServer().getWorld(world), x, y, z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public UUID getWorld() {
        return world;
    }

}
