package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link AcademicRecordDao} (table "registros_academicos"). */
public class AcademicRecordDaoImpl implements AcademicRecordDao {

    private final DatabaseConnection database;

    public AcademicRecordDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public void saveOrUpdate(AcademicRecord record) {
        String sql = "INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, anio_academico, "
                + "periodo_academico, valor_nota, observaciones, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE valor_nota = VALUES(valor_nota), observaciones = VALUES(observaciones), "
                + "id_profesor = VALUES(id_profesor), fecha_registro = VALUES(fecha_registro)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, record.studentId());
            statement.setInt(2, record.subjectId());
            statement.setInt(3, record.professorId());
            statement.setInt(4, record.academicYear());
            statement.setString(5, record.period().getDbValue());
            statement.setBigDecimal(6, record.value());
            statement.setString(7, record.observations());
            statement.setDate(8, JdbcUtil.toSqlDate(record.recordDate()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw fail("guardar la nota", e);
        }
    }

    @Override
    public List<AcademicRecord> findByStudent(int studentId) {
        String sql = "SELECT r.id_registro, r.id_estudiante, r.id_materia, m.nombre_materia, r.id_profesor, "
                + "r.anio_academico, r.periodo_academico, r.valor_nota, r.observaciones, r.fecha_registro "
                + "FROM registros_academicos r JOIN materias m ON m.id_materia = r.id_materia "
                + "WHERE r.id_estudiante = ? ORDER BY r.anio_academico, "
                + "FIELD(r.periodo_academico, 'I', 'II', 'III', 'IV', 'Final'), m.nombre_materia";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                List<AcademicRecord> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(new AcademicRecord(rs.getInt("id_registro"), rs.getInt("id_estudiante"),
                            rs.getInt("id_materia"), rs.getString("nombre_materia"), rs.getInt("id_profesor"),
                            rs.getInt("anio_academico"), Period.fromDbValue(rs.getString("periodo_academico")),
                            rs.getBigDecimal("valor_nota"), rs.getString("observaciones"),
                            JdbcUtil.getLocalDate(rs, "fecha_registro")));
                }
                return records;
            }
        } catch (SQLException e) {
            throw fail("consultar el historial académico", e);
        }
    }

    @Override
    public Optional<Double> averageBySubject(int subjectId, int academicYear, Period period) {
        String sql = "SELECT AVG(valor_nota) FROM registros_academicos WHERE id_materia = ? AND anio_academico = ?"
                + (period == null ? " AND periodo_academico <> 'Final'" : " AND periodo_academico = ?");
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
            if (period != null) {
                statement.setString(3, period.getDbValue());
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    double average = rs.getDouble(1);
                    return rs.wasNull() ? Optional.empty() : Optional.of(average);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw fail("calcular el promedio de la materia", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("AcademicRecordDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
