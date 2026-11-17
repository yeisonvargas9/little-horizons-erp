package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** JDBC implementation of {@link SubjectEnrollmentDao} (table "inscripciones_materia"). */
public class SubjectEnrollmentDaoImpl implements SubjectEnrollmentDao {

    private final DatabaseConnection database;

    public SubjectEnrollmentDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public boolean isEnrolled(int studentId, int subjectId, int academicYear, Period period) {
        String sql = "SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? "
                + "AND anio_academico = ?" + (period == null ? "" : " AND periodo_academico = ?");
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, subjectId);
            statement.setInt(3, academicYear);
            if (period != null) {
                statement.setString(4, period.getDbValue());
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw fail("verificar la inscripción", e);
        }
    }

    @Override
    public void enrollMany(int studentId, List<Integer> subjectIds, int academicYear, Period period, String section) {
        String sql = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, anio_academico, "
                + "periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, 'Activa')";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int subjectId : subjectIds) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    statement.setString(4, period.getDbValue());
                    statement.setString(5, section);
                    statement.setDate(6, Date.valueOf(LocalDate.now()));
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("inscribir al estudiante en las materias", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("SubjectEnrollmentDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
