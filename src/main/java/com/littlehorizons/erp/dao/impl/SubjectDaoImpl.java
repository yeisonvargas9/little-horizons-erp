package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link SubjectDao} (tables "materias" and "grados"). */
public class SubjectDaoImpl implements SubjectDao {

    private static final String SELECT_BASE =
            "SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, m.id_grado, g.nombre_grado, "
                    + "m.horas_semanales, m.esta_activa FROM materias m JOIN grados g ON g.id_grado = m.id_grado";
    private static final String ORDER = " ORDER BY g.nivel_numerico, m.codigo_materia";

    private final DatabaseConnection database;

    public SubjectDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Optional<Subject> findById(int id) {
        List<Subject> result = query(SELECT_BASE + " WHERE m.id_materia = ?", id);
        return result.stream().findFirst();
    }

    @Override
    public List<Subject> findAll() {
        return query(SELECT_BASE + ORDER, null);
    }

    @Override
    public List<Subject> findByGradeId(int gradeId) {
        return query(SELECT_BASE + " WHERE m.id_grado = ?" + ORDER, gradeId);
    }

    private List<Subject> query(String sql, Integer parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setInt(1, parameter);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Subject> subjects = new ArrayList<>();
                while (rs.next()) {
                    Subject subject = new Subject();
                    subject.setId(rs.getInt("id_materia"));
                    subject.setCode(rs.getString("codigo_materia"));
                    subject.setName(rs.getString("nombre_materia"));
                    subject.setDescription(rs.getString("descripcion"));
                    subject.setGradeId(rs.getInt("id_grado"));
                    subject.setGradeName(rs.getString("nombre_grado"));
                    subject.setWeeklyHours(rs.getInt("horas_semanales"));
                    subject.setActive(rs.getBoolean("esta_activa"));
                    subjects.add(subject);
                }
                return subjects;
            }
        } catch (SQLException e) {
            AppLogger.error("SubjectDao: failed to query subjects", e);
            throw new DataAccessException("Error de base de datos al consultar las materias.", e);
        }
    }
}
