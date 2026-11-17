package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.AbsenceSummary;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation of {@link AttendanceDao} (table "asistencias"). */
public class AttendanceDaoImpl implements AttendanceDao {

    private final DatabaseConnection database;

    public AttendanceDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public void saveAll(List<AttendanceRecord> records) {
        String sql = "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, "
                + "observaciones, registrado_por) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "estado_asistencia = VALUES(estado_asistencia), observaciones = VALUES(observaciones), "
                + "registrado_por = VALUES(registrado_por)";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (AttendanceRecord record : records) {
                    statement.setInt(1, record.studentId());
                    statement.setInt(2, record.subjectId());
                    statement.setDate(3, JdbcUtil.toSqlDate(record.date()));
                    statement.setString(4, record.status().getDbValue());
                    statement.setString(5, record.notes());
                    if (record.registeredBy() == null) {
                        statement.setNull(6, Types.INTEGER);
                    } else {
                        statement.setInt(6, record.registeredBy());
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("guardar la asistencia", e);
        }
    }

    @Override
    public List<AbsenceSummary> findAbsenceSummary(LocalDate from, LocalDate to, int minDays) {
        String sql = "SELECT e.id_estudiante, CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                + "e.segundo_apellido) AS nombre, COUNT(DISTINCT a.fecha_asistencia) AS dias FROM asistencias a "
                + "JOIN estudiantes e ON e.id_estudiante = a.id_estudiante "
                + "WHERE a.estado_asistencia = 'Ausente' AND a.fecha_asistencia BETWEEN ? AND ? "
                + "GROUP BY e.id_estudiante, e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido "
                + "HAVING COUNT(DISTINCT a.fecha_asistencia) >= ? ORDER BY dias DESC, nombre";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, JdbcUtil.toSqlDate(from));
            statement.setDate(2, JdbcUtil.toSqlDate(to));
            statement.setInt(3, minDays);
            try (ResultSet rs = statement.executeQuery()) {
                List<AbsenceSummary> summaries = new ArrayList<>();
                while (rs.next()) {
                    summaries.add(new AbsenceSummary(rs.getInt(1), rs.getString(2), rs.getLong(3)));
                }
                return summaries;
            }
        } catch (SQLException e) {
            throw fail("generar el reporte de inasistencias", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("AttendanceDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
