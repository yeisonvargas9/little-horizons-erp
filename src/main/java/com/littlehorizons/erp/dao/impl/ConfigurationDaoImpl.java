package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ConfigurationDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** JDBC implementation of {@link ConfigurationDao} (table "configuracion_colegio"). */
public class ConfigurationDaoImpl implements ConfigurationDao {

    private final DatabaseConnection database;

    public ConfigurationDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Map<String, String> findAll() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT clave, valor FROM configuracion_colegio");
             ResultSet rs = statement.executeQuery()) {
            Map<String, String> values = new HashMap<>();
            while (rs.next()) {
                values.put(rs.getString(1), rs.getString(2));
            }
            return values;
        } catch (SQLException e) {
            AppLogger.error("ConfigurationDao: failed to read the school configuration", e);
            throw new DataAccessException("Error de base de datos al leer la configuración del colegio.", e);
        }
    }
}
