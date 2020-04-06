package uk.tethys.survival.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import uk.tethys.survival.Constants;
import uk.tethys.survival.Survival;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class SQLManager {

    private final HikariDataSource dataSource;
    private final Survival plugin;

    public SQLManager(Survival plugin) throws SQLException, IOException {
        this.plugin = plugin;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + Constants.DB_HOST + ":" + Constants.DB_PORT + "/" + Constants.DB_SCHEMA);
        config.setUsername(Constants.DB_USER);
        config.setPassword(Constants.DB_PASS);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        init();
    }

    private void init() throws SQLException, IOException {
        applySchema();
    }

    private void applySchema() {
        List<String> statements;
        try (InputStream is = plugin.getResource("mysql.sql")) {
            if (is == null) {
                throw new IOException("Schema file not found");
            }
            statements = getStatements(is);
            try (Statement statement = getConnection().createStatement()) {
                for (String s : statements)
                    statement.addBatch(s);
                statement.executeBatch();
            }
        } catch (SQLException | IOException e) {
            Bukkit.getLogger().severe("Error applying schema");
            e.printStackTrace();
        }
    }

    //todo do we need this with the exists checks in schema file?
    @Deprecated
    private boolean tableExists(Connection c, String table) throws SQLException {
        try (ResultSet set = c.getMetaData().getTables(null, null, "%", null)) {
            while (set.next()) {
                if (set.getString("TABLE_NAME").equalsIgnoreCase(table)) {
                    return true;
                }
            }
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static List<String> getStatements(InputStream is) throws IOException {
        List<String> queries = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("--"))
                    continue;
                builder.append(line);
                if (line.endsWith(";")) {
                    builder.deleteCharAt(builder.length() - 1);
                    String result = builder.toString().trim();
                    if (!result.isEmpty())
                        queries.add(result);
                    builder = new StringBuilder();
                }
            }
        }

        return queries;
    }

}
