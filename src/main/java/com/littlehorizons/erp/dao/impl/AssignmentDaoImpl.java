package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Weekday;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JDBC implementation of {@link AssignmentDao} (tables "asignaciones_profesor" and "horarios_clase"). */
public class AssignmentDaoImpl implements AssignmentDao {

    private static final String OVERLAP =
            "h.dia_semana = ? AND h.hora_inicio < ? AND h.hora_fin > ?";

    private final DatabaseConnection database;

    public AssignmentDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Assignment save(Assignment assignment) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertAssignment(connection, assignment);
                insertSlots(connection, assignment);
                connection.commit();
                return assignment;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("guardar la asignación", e);
        }
    }

    @Override
    public boolean exists(int professorId, int subjectId, int academicYear, String section) {
        String sql = "SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? "
                + "AND anio_academico = ? AND seccion = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, professorId);
            statement.setInt(2, subjectId);
            statement.setInt(3, academicYear);
            statement.setString(4, section);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw fail("verificar la asignación", e);
        }
    }

    @Override
    public List<Assignment> findByProfessor(int professorId, int academicYear) {
        String assignmentsSql = "SELECT a.id_asignacion, a.id_profesor, a.id_materia, a.anio_academico, a.seccion, "
                + "m.codigo_materia, m.nombre_materia, g.nombre_grado FROM asignaciones_profesor a "
                + "JOIN materias m ON m.id_materia = a.id_materia JOIN grados g ON g.id_grado = m.id_grado "
                + "WHERE a.id_profesor = ? AND a.anio_academico = ? ORDER BY g.nivel_numerico, m.nombre_materia";
        String slotsSql = "SELECT h.id_asignacion, h.dia_semana, h.hora_inicio, h.hora_fin FROM horarios_clase h "
                + "JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion "
                + "WHERE a.id_profesor = ? AND a.anio_academico = ?";
        try (Connection connection = database.getConnection()) {
            Map<Integer, Assignment> byId = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(assignmentsSql)) {
                statement.setInt(1, professorId);
                statement.setInt(2, academicYear);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Assignment assignment = new Assignment();
                        assignment.setId(rs.getInt("id_asignacion"));
                        assignment.setProfessorId(rs.getInt("id_profesor"));
                        assignment.setSubjectId(rs.getInt("id_materia"));
                        assignment.setAcademicYear(rs.getInt("anio_academico"));
                        assignment.setSection(rs.getString("seccion"));
                        assignment.setSubjectCode(rs.getString("codigo_materia"));
                        assignment.setSubjectName(rs.getString("nombre_materia"));
                        assignment.setGradeName(rs.getString("nombre_grado"));
                        byId.put(assignment.getId(), assignment);
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(slotsSql)) {
                statement.setInt(1, professorId);
                statement.setInt(2, academicYear);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Assignment owner = byId.get(rs.getInt("id_asignacion"));
                        if (owner != null) {
                            owner.getSlots().add(new ScheduleSlot(Weekday.fromDbValue(rs.getString("dia_semana")),
                                    rs.getTime("hora_inicio").toLocalTime(), rs.getTime("hora_fin").toLocalTime()));
                        }
                    }
                }
            }
            return new ArrayList<>(byId.values());
        } catch (SQLException e) {
            throw fail("consultar las asignaciones del profesor", e);
        }
    }

    @Override
    public boolean hasProfessorConflict(int professorId, int academicYear, ScheduleSlot slot) {
        String sql = "SELECT COUNT(*) FROM horarios_clase h JOIN asignaciones_profesor a "
                + "ON a.id_asignacion = h.id_asignacion WHERE a.id_profesor = ? AND a.anio_academico = ? AND " + OVERLAP;
        return count(sql, professorId, academicYear, slot);
    }

    @Override
    public boolean hasGroupConflict(int gradeId, int academicYear, String section, ScheduleSlot slot) {
        String sql = "SELECT COUNT(*) FROM horarios_clase h JOIN asignaciones_profesor a "
                + "ON a.id_asignacion = h.id_asignacion JOIN materias m ON m.id_materia = a.id_materia "
                + "WHERE m.id_grado = ? AND a.anio_academico = ? AND a.seccion = ? AND " + OVERLAP;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gradeId);
            statement.setInt(2, academicYear);
            statement.setString(3, section);
            statement.setString(4, slot.day().getDbValue());
            statement.setTime(5, Time.valueOf(slot.end()));
            statement.setTime(6, Time.valueOf(slot.start()));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw fail("verificar choques de horario del grupo", e);
        }
    }

    private boolean count(String sql, int ownerId, int academicYear, ScheduleSlot slot) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            statement.setInt(2, academicYear);
            statement.setString(3, slot.day().getDbValue());
            statement.setTime(4, Time.valueOf(slot.end()));
            statement.setTime(5, Time.valueOf(slot.start()));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw fail("verificar choques de horario del profesor", e);
        }
    }

    private void insertAssignment(Connection connection, Assignment assignment) throws SQLException {
        String sql = "INSERT INTO asignaciones_profesor (id_profesor, id_materia, anio_academico, seccion) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, assignment.getProfessorId());
            statement.setInt(2, assignment.getSubjectId());
            statement.setInt(3, assignment.getAcademicYear());
            statement.setString(4, assignment.getSection());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    assignment.setId(keys.getInt(1));
                }
            }
        }
    }

    private void insertSlots(Connection connection, Assignment assignment) throws SQLException {
        String sql = "INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ScheduleSlot slot : assignment.getSlots()) {
                statement.setInt(1, assignment.getId());
                statement.setString(2, slot.day().getDbValue());
                statement.setTime(3, Time.valueOf(slot.start()));
                statement.setTime(4, Time.valueOf(slot.end()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("AssignmentDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
