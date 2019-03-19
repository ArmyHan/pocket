package org.hunter.pocket.connect;

import org.hunter.pocket.config.DatabaseNodeConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wujianchuan 2018/12/31
 */
public class DatabaseManager {
    private static final Map<String, DatabaseManager> managerMap = new HashMap<>(2);
    private final DatabaseNodeConfig config;
    private static Connection conn = null;
    private static AtomicInteger connectionCount = new AtomicInteger(0);

    private DatabaseManager(DatabaseNodeConfig config) {
        this.config = config;
        try {
            Class.forName(config.getDriverName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance(DatabaseNodeConfig config) {
        DatabaseManager databaseManager = managerMap.get(config.getNodeName());
        if (databaseManager == null) {
            managerMap.put(config.getNodeName(), new DatabaseManager(config));
        }
        return managerMap.get(config.getNodeName());
    }

    Connection newConnection() {
        try {
            conn = DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    Boolean isValidConnection(Connection connection) throws SQLException {
        return connection != null && !connection.isClosed();
    }
}
