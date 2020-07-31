package uk.tethys.survival.managers;

import org.bukkit.Bukkit;
import uk.tethys.survival.Survival;
import uk.tethys.survival.objects.Claim;

import java.sql.Connection;
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
            connection.prepareStatement(String.format(
                    "INSERT INTO `claims` (`owner`, `x1`, `y1`, `z1`, `x2`, `y2`, `z2`, `world`) VALUES ('%s', %d, %d, %d, %d, %d, %d, '%s')",
                    claim.getOwner().toString(),
                    (int) claim.getCorner1().getX(), (int) claim.getCorner1().getY(), (int) claim.getCorner1().getZ(),
                    (int) claim.getCorner2().getX(), (int) claim.getCorner2().getY(), (int) claim.getCorner2().getZ(),
                    claim.getCorner1().getWorld())).execute();
        }
    }

}
