package uk.tethys.survival.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

public class SerializableLocation implements Serializable {

    private static long serialVersionUID = 00001;

    private int x,
            y,
            z;

    private UUID world;

    public SerializableLocation(int x, int y, int z, UUID world) {
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
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

}
