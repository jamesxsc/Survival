package uk.tethys.survival.objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import uk.tethys.survival.util.SerializableLocation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Claim implements Serializable {

    private static long serialVersionUID = 00001L;

    private Set<Flag> flags;
    private UUID owner;
    private SerializableLocation corner1;
    private SerializableLocation corner2;

    public Claim(Player owner, Location corner1, Location corner2) {
        this.flags = Flag.DEFAULT_FLAGSET;
        this.owner = owner.getUniqueId();
        this.corner1 = new SerializableLocation(corner1);
        this.corner2 = new SerializableLocation(corner2);
    }

    public enum Flag {

        KILLFLAG(Boolean.class)
        ;

        @SuppressWarnings({"FieldCanBeLocal", "rawtypes"})
        private final Class type;

        < T > Flag(Class<T> type) {
            this.type = type;
        }

        public static final Set<Flag> DEFAULT_FLAGSET = new HashSet<>();
    }

}
