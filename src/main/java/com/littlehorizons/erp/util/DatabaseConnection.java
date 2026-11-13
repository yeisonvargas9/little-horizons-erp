package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that holds the database settings (read once from "db.properties") and hands out
 * a fresh JDBC connection on request. Callers close each connection with try-with-resources.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "db.properties";

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        Properties properties = loadProperties();
        this.url = properties.getProperty("db.url");
        this.user = properties.getProperty("db.user");
        this.password = properties.getProperty("db.password", "");
        if (url == null || user == null) {
            throw new DataAccessException("Faltan 'db.url' o 'db.user' en " + CONFIG_FILE + ".");
        }
    }

    /** Lazy, thread-safe initialization (initialization-on-demand holder idiom). */
    private static final class Holder {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    }

    public static DatabaseConnection getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Opens and closes one connection to confirm the database is reachable. */
    public void verifyConnection() {
        try (Connection ignored = getConnection()) {
            AppLogger.info("Database connection verified");
        } catch (SQLException e) {
            AppLogger.error("Could not connect to the database", e);
            throw new DataAccessException("No fue posible conectarse a la base de datos. Revise " + CONFIG_FILE
                    + " y que MySQL esté encendido.", e);
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path file = Path.of(CONFIG_FILE);
        try {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    properties.load(in);
                }
            } else {
                try (InputStream in = DatabaseConnection.class.getResourceAsStream("/" + CONFIG_FILE)) {
                    if (in == null) {
                        throw new DataAccessException("No se encontró " + CONFIG_FILE
                                + ". Copie db.properties.example como db.properties y complete sus credenciales.");
                    }
                    properties.load(in);
                }
            }
        } catch (IOException e) {
            AppLogger.error("Could not read " + CONFIG_FILE, e);
            throw new DataAccessException("No fue posible leer " + CONFIG_FILE + ".", e);
        }
        return properties;
    }
}
