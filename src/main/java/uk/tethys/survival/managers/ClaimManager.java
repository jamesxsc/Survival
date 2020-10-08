package uk.tethys.survival.managers;

import org.bukkit.Bukkit;
import uk.tethys.survival.Survival;
import uk.tethys.survival.objects.Claim;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ClaimManager {

    private final Survival plugin;

    public ClaimManager(Survival plugin) {
        this.plugin = plugin;
    }

    public void addClaim(Claim claim) throws SQLException {
        try (Connection connection = plugin.getDBConnection()) {
            PreparedStatement st = connection.prepareStatement(String.format(
                    "INSERT INTO `claims` (`owner`, `x1`, `y1`, `z1`, `x2`, `y2`, `z2`, `world`) VALUES ('%s', %d, %d, %d, %d, %d, %d, '%s')",
                    claim.getOwner().toString(),
                    claim.getCorner1().getLocation().getBlockX(), claim.getCorner1().getLocation().getBlockY(), claim.getCorner1().getLocation().getBlockZ(),
                    claim.getCorner2().getLocation().getBlockX(), claim.getCorner2().getLocation().getBlockY(), claim.getCorner2().getLocation().getBlockZ(),
                    claim.getCorner1().getWorld()));
            st.execute();
            st.close();
        }
    }

}
