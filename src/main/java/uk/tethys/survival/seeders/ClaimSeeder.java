package uk.tethys.survival.seeders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import uk.tethys.survival.Survival;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ClaimSeeder implements Seeder {

    @Override
    public String id() {
        return "claim";
    }

    @Override
    public void seed() throws SQLException {
        try (Connection connection = Survival.INSTANCE.getDBConnection()) {
            Bukkit.getWorlds().forEach((world -> {
                Location center = world.getSpawnLocation();
                int r = 200;

                Location corner1 = center;
                corner1.setX(corner1.getX() - (double) r / 2);
                corner1.setY(0);
                corner1.setZ(corner1.getZ() - (double) r / 2);

                Location corner2 = center;
                corner2.setX(corner2.getX() + (double) r / 2);
                corner2.setY(256);
                corner2.setZ(corner2.getZ() + (double) r / 2);

                try {
                    PreparedStatement st = connection.prepareStatement(String.format(
                            "INSERT INTO `claims` (`owner`, `x1`, `y1`, `z1`, `x2`, `y2`, `z2`, `world`) VALUES ('%s', %d, %d, %d, %d, %d, %d, '%s')",
                            "ffffffff-ffff-ffff-ffff-ffffffffffff",
                            corner1.getBlockX(), corner1.getBlockY(), corner1.getBlockZ(),
                            corner2.getBlockX(), corner2.getBlockY(), corner2.getBlockZ(),
                            world));
                    st.execute();
                    st.close();
                } catch (SQLException ex) {
                    Survival.INSTANCE.getLogger().severe("SQLException thrown during execution of claim seeder.");
                    ex.printStackTrace();
                }
            }));
        }
    }

    @Override
    public void clear() {
        // todo clear
    }

}
