package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.StudentSummary;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link EnrollmentDao} (table "matriculas"). */
public class EnrollmentDaoImpl implements EnrollmentDao {

    private static final String SELECT_BASE =
            "SELECT m.id_matricula, m.id_estudiante, m.id_grado, g.nombre_grado, m.anio_academico, "
                    + "m.estado_matricula, m.fecha_matricula FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado";

    private final DatabaseConnection database;

    public EnrollmentDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Enrollment save(Enrollment enrollment) {
        String sql = "INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, estado_matricula, "
                + "fecha_matricula) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, enrollment.studentId());
            statement.setInt(2, enrollment.gradeId());
            statement.setInt(3, enrollment.academicYear());
            statement.setString(4, enrollment.status().getDbValue());
            statement.setDate(5, JdbcUtil.toSqlDate(enrollment.enrollmentDate()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new Enrollment(id, enrollment.studentId(), enrollment.gradeId(), enrollment.gradeName(),
                        enrollment.academicYear(), enrollment.status(), enrollment.enrollmentDate());
            }
        } catch (SQLException e) {
            throw fail("guardar la matrícula", e);
        }
    }

    @Override
    public List<Enrollment> findByStudent(int studentId) {
        return query(SELECT_BASE + " WHERE m.id_estudiante = ? ORDER BY m.anio_academico DESC", studentId);
    }

    @Override
    public Optional<Enrollment> findByStudentAndYear(int studentId, int academicYear) {
        return query(SELECT_BASE + " WHERE m.id_estudiante = ? AND m.anio_academico = ?", studentId, academicYear)
                .stream().findFirst();
    }

    @Override
    public List<StudentSummary> findStudentsByGrade(int gradeId, int academicYear) {
        String sql = "SELECT e.id_estudiante, CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                + "e.segundo_apellido) AS nombre, e.numero_documento FROM matriculas m "
                + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante WHERE m.id_grado = ? AND m.anio_academico = ? "
                + "AND m.estado_matricula = 'Activa' AND e.estado = 'Activo' ORDER BY e.primer_apellido, e.primer_nombre";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gradeId);
            statement.setInt(2, academicYear);
            try (ResultSet rs = statement.executeQuery()) {
                List<StudentSummary> students = new ArrayList<>();
                while (rs.next()) {
                    students.add(new StudentSummary(rs.getInt(1), rs.getString(2), rs.getString(3)));
                }
                return students;
            }
        } catch (SQLException e) {
            throw fail("listar los estudiantes del grado", e);
        }
    }

    private List<Enrollment> query(String sql, int... parameters) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setInt(i + 1, parameters[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Enrollment> enrollments = new ArrayList<>();
                while (rs.next()) {
                    enrollments.add(new Enrollment(rs.getInt("id_matricula"), rs.getInt("id_estudiante"),
                            rs.getInt("id_grado"), rs.getString("nombre_grado"), rs.getInt("anio_academico"),
                            EnrollmentStatus.fromDbValue(rs.getString("estado_matricula")),
                            JdbcUtil.getLocalDate(rs, "fecha_matricula")));
                }
                return enrollments;
            }
        } catch (SQLException e) {
            throw fail("consultar las matrículas", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("EnrollmentDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
