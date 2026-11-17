package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.CustomValueDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** JDBC implementation of {@link CustomValueDao} (table "valores_concepto_estudiante"). */
public class CustomValueDaoImpl implements CustomValueDao {

    private final DatabaseConnection database;

    public CustomValueDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Optional<BigDecimal> findAmount(int studentId, int conceptId, int academicYear) {
        String sql = "SELECT monto_personalizado FROM valores_concepto_estudiante "
                + "WHERE id_estudiante = ? AND id_concepto = ? AND anio_academico = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, conceptId);
            statement.setInt(3, academicYear);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw fail("consultar el valor personalizado", e);
        }
    }

    @Override
    public void saveOrUpdate(int studentId, int conceptId, BigDecimal amount, String reason, int academicYear) {
        String sql = "INSERT INTO valores_concepto_estudiante (id_estudiante, id_concepto, monto_personalizado, "
                + "motivo, anio_academico) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "monto_personalizado = VALUES(monto_personalizado), motivo = VALUES(motivo)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, conceptId);
            statement.setBigDecimal(3, amount);
            statement.setString(4, reason);
            statement.setInt(5, academicYear);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw fail("guardar el valor personalizado", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("CustomValueDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
