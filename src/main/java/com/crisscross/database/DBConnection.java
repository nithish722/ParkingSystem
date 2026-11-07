package com.crisscross.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

/**
 * JDBC Database Connection Manager for CrissCross.
 * Handles MySQL connection pooling, schema initialization, and fallback mode.
 */
public class DBConnection {
    private static DBConnection instance;
    private Connection connection;
    private final Properties config = new Properties();

    private boolean mySQLConnected = false;
    private String connectionStatusMessage = "Initializing...";

    private DBConnection() {
        loadConfig();
        connect();
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in != null) {
                config.load(in);
            } else {
                // Default fallback properties
                config.setProperty("db.host", "localhost");
                config.setProperty("db.port", "3306");
                config.setProperty("db.name", "crisscross_db");
                config.setProperty("db.user", "root");
                config.setProperty("db.password", "");
            }
        } catch (Exception e) {
            System.err.println("[CrissCross DB] Could not read config.properties: " + e.getMessage());
        }
    }

    public synchronized boolean connect() {
        try {
            // Load MySQL Connector/J driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            String host = config.getProperty("db.host", "localhost");
            String port = config.getProperty("db.port", "3306");
            String dbName = config.getProperty("db.name", "crisscross_db");
            String user = config.getProperty("db.user", "root");
            String pass = config.getProperty("db.password", "");

            // Connect to server (first create DB if needed)
            String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            
            try (Connection serverConn = DriverManager.getConnection(serverUrl, user, pass);
                 Statement stmt = serverConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            }

            // Now connect directly to the database
            String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            connection = DriverManager.getConnection(dbUrl, user, pass);
            mySQLConnected = true;
            connectionStatusMessage = "Connected to MySQL (" + host + ":" + port + "/" + dbName + ")";
            System.out.println("[CrissCross DB] " + connectionStatusMessage);

            // Initialize tables if needed
            initializeSchemaIfEmpty();
            return true;
        } catch (ClassNotFoundException e) {
            mySQLConnected = false;
            connectionStatusMessage = "MySQL JDBC Driver not found. Using resilient local store.";
            System.err.println("[CrissCross DB] " + connectionStatusMessage);
            return false;
        } catch (SQLException e) {
            mySQLConnected = false;
            connectionStatusMessage = "MySQL offline (" + e.getMessage() + "). Running in seamless in-memory mode.";
            System.out.println("[CrissCross DB] " + connectionStatusMessage);
            return false;
        }
    }

    private void initializeSchemaIfEmpty() {
        if (!mySQLConnected || connection == null) return;
        try (Statement stmt = connection.createStatement()) {
            // Check if users table exists
            try {
                stmt.executeQuery("SELECT 1 FROM users LIMIT 1");
                System.out.println("[CrissCross DB] Schema already exists and verified.");
            } catch (SQLException e) {
                // Table doesn't exist, execute schema.sql
                System.out.println("[CrissCross DB] Initializing tables from schema.sql...");
                executeSchemaSql();
            }
        } catch (Exception e) {
            System.err.println("[CrissCross DB] Schema check failed: " + e.getMessage());
        }
    }

    public void executeSchemaSql() {
        try (InputStream in = getClass().getResourceAsStream("/sql/schema.sql")) {
            if (in == null) return;
            Scanner scanner = new Scanner(in).useDelimiter(";");
            try (Statement stmt = connection.createStatement()) {
                while (scanner.hasNext()) {
                    String sql = scanner.next().trim();
                    if (!sql.isEmpty() && !sql.startsWith("--")) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException ignored) {
                        }
                    }
                }
            }
            System.out.println("[CrissCross DB] Schema initialization completed successfully.");
        } catch (Exception e) {
            System.err.println("[CrissCross DB] Error executing schema.sql: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            mySQLConnected = false;
        }
        return connection;
    }

    public boolean isMySQLConnected() {
        try {
            return mySQLConnected && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public String getConnectionStatusMessage() {
        return connectionStatusMessage;
    }

    public Properties getConfig() {
        return config;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
