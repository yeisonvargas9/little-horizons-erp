package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ProfessorStatus;
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

/** JDBC implementation of {@link ProfessorDao} (table "profesores"). */
public class ProfessorDaoImpl implements ProfessorDao {

    private static final String SELECT_BASE =
            "SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, "
                    + "segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado "
                    + "FROM profesores";

    private final DatabaseConnection database;

    public ProfessorDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Professor save(Professor professor) {
        String sql = "INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
                + "primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, "
                + "fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, professor.getDocumentType().getDbValue());
            statement.setString(2, professor.getDocumentNumber());
            statement.setString(3, professor.getFirstName());
            statement.setString(4, professor.getMiddleName());
            statement.setString(5, professor.getFirstSurname());
            statement.setString(6, professor.getSecondSurname());
            statement.setString(7, professor.getEmail());
            statement.setString(8, professor.getPhone());
            statement.setString(9, professor.getSpecialization());
            statement.setDate(10, JdbcUtil.toSqlDate(professor.getHireDate()));
            statement.setString(11, professor.getStatus().getDbValue());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    professor.setId(keys.getInt(1));
                }
            }
            return professor;
        } catch (SQLException e) {
            throw fail("guardar el profesor", e);
        }
    }

    @Override
    public Optional<Professor> findById(Integer id) {
        return findOne(SELECT_BASE + " WHERE id_profesor = ?", String.valueOf(id));
    }

    @Override
    public Optional<Professor> findByDocument(String documentNumber) {
        return findOne(SELECT_BASE + " WHERE numero_documento = ?", documentNumber);
    }

    @Override
    public List<Professor> findAll() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_BASE + " ORDER BY primer_apellido, segundo_apellido, primer_nombre");
             ResultSet rs = statement.executeQuery()) {
            List<Professor> professors = new ArrayList<>();
            while (rs.next()) {
                professors.add(mapRow(rs));
            }
            return professors;
        } catch (SQLException e) {
            throw fail("listar los profesores", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM profesores WHERE correo_electronico = ?")) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw fail("verificar el correo del profesor", e);
        }
    }

    /** Updates email, phone and specialization (identity data is not editable). */
    @Override
    public boolean update(Professor professor) {
        String sql = "UPDATE profesores SET correo_electronico = ?, telefono = ?, especializacion = ? WHERE id_profesor = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, professor.getEmail());
            statement.setString(2, professor.getPhone());
            statement.setString(3, professor.getSpecialization());
            statement.setInt(4, professor.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail("actualizar el profesor", e);
        }
    }

    @Override
    public boolean updateStatus(int professorId, ProfessorStatus status) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE profesores SET estado = ? WHERE id_profesor = ?")) {
            statement.setString(1, status.getDbValue());
            statement.setInt(2, professorId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail("cambiar el estado del profesor", e);
        }
    }

    private Optional<Professor> findOne(String sql, String parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw fail("consultar el profesor", e);
        }
    }

    private Professor mapRow(ResultSet rs) throws SQLException {
        Professor professor = new Professor();
        professor.setId(rs.getInt("id_profesor"));
        professor.setDocumentType(DocumentType.fromDbValue(rs.getString("tipo_documento")));
        professor.setDocumentNumber(rs.getString("numero_documento"));
        professor.setFirstName(rs.getString("primer_nombre"));
        professor.setMiddleName(rs.getString("segundo_nombre"));
        professor.setFirstSurname(rs.getString("primer_apellido"));
        professor.setSecondSurname(rs.getString("segundo_apellido"));
        professor.setEmail(rs.getString("correo_electronico"));
        professor.setPhone(rs.getString("telefono"));
        professor.setSpecialization(rs.getString("especializacion"));
        professor.setHireDate(JdbcUtil.getLocalDate(rs, "fecha_contratacion"));
        professor.setStatus(ProfessorStatus.fromDbValue(rs.getString("estado")));
        return professor;
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("ProfessorDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
