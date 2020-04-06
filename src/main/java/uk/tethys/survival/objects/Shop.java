package uk.tethys.survival.objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import uk.tethys.survival.util.SerializableLocation;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

public class Shop implements Serializable {

    private static long serialVersionUid = 5L;

    private Material material;

    private int buy;
    private int sell;

    private SerializableLocation location;
    private UUID owner;

    private Optional<String> doubleDirection;

    public Shop() {
    }

    public Shop(Material material, int buy, int sell, Location location, Player owner) {
        this(material, buy, sell, location, owner, Optional.empty());
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public int getBuy() {
        return buy;
    }

    public void setBuy(int buy) {
        this.buy = buy;
    }

    public int getSell() {
        return sell;
    }

    public void setSell(int sell) {
        this.sell = sell;
    }

    public SerializableLocation getLocation() {
        return location;
    }

    public void setLocation(SerializableLocation location) {
        this.location = location;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Optional<String> getDoubleDirection() {
        return doubleDirection;
    }

    public void setDoubleDirection(Optional<String> doubleDirection) {
        this.doubleDirection = doubleDirection;
    }

    public Shop(Material material, int buy, int sell, Location location, Player owner, Optional doubleDirection) {
        this.material = material;
        this.buy = buy;
        this.sell = sell;
        this.location = new SerializableLocation(location);
        this.owner = owner.getUniqueId();
        this.doubleDirection = doubleDirection;
    }

}
