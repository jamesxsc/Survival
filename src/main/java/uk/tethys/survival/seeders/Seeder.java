package uk.tethys.survival.seeders;

import java.sql.SQLException;

public interface Seeder {

    String id();

    void seed() throws SQLException;

    void clear();

    default void reseed() throws SQLException {
        clear();
        seed();
    }

}
