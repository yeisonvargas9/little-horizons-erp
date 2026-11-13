package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link GuardianDao} (table "acudientes"). */
public class GuardianDaoImpl implements GuardianDao {

    private static final String SELECT_BASE =
            "SELECT a.id_acudiente, a.tipo_documento, a.numero_documento, a.primer_nombre, a.segundo_nombre, "
                    + "a.primer_apellido, a.segundo_apellido, a.parentesco, a.telefono_principal, "
                    + "a.telefono_secundario, a.correo_electronico, a.direccion, a.ocupacion, "
                    + "a.es_responsable_financiero FROM acudientes a";

    private static final String INSERT =
            "INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
                    + "primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario, "
                    + "correo_electronico, direccion, ocupacion, es_responsable_financiero) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final DatabaseConnection database;

    public GuardianDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Guardian save(Guardian guardian) {
        try (Connection connection = database.getConnection()) {
            return insert(connection, guardian);
        } catch (SQLException e) {
            throw fail("guardar el acudiente", e);
        }
    }

    @Override
    public Optional<Guardian> findById(Integer id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASE + " WHERE a.id_acudiente = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw fail("consultar el acudiente", e);
        }
    }

    @Override
    public List<Guardian> findAll() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_BASE + " ORDER BY a.primer_apellido, a.segundo_apellido, a.primer_nombre");
             ResultSet rs = statement.executeQuery()) {
            List<Guardian> guardians = new ArrayList<>();
            while (rs.next()) {
                guardians.add(mapRow(rs));
            }
            return guardians;
        } catch (SQLException e) {
            throw fail("listar los acudientes", e);
        }
    }

    @Override
    public boolean update(Guardian guardian) {
        String sql = "UPDATE acudientes SET telefono_principal = ?, telefono_secundario = ?, "
                + "correo_electronico = ?, direccion = ?, ocupacion = ? WHERE id_acudiente = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guardian.getPrimaryPhone());
            statement.setString(2, guardian.getSecondaryPhone());
            statement.setString(3, guardian.getEmail());
            statement.setString(4, guardian.getAddress());
            statement.setString(5, guardian.getOccupation());
            statement.setInt(6, guardian.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail("actualizar el acudiente", e);
        }
    }

    @Override
    public Optional<Guardian> findByDocument(String documentNumber) {
        try (Connection connection = database.getConnection()) {
            return findByDocument(connection, documentNumber);
        } catch (SQLException e) {
            throw fail("buscar el acudiente por documento", e);
        }
    }

    @Override
    public List<Guardian> findByStudentId(int studentId) {
        String sql = SELECT_BASE + " JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente "
                + "WHERE ea.id_estudiante = ? ORDER BY ea.es_principal DESC, a.primer_apellido";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Guardian> guardians = new ArrayList<>();
                while (rs.next()) {
                    guardians.add(mapRow(rs));
                }
                return guardians;
            }
        } catch (SQLException e) {
            throw fail("consultar los acudientes del estudiante", e);
        }
    }

    // ---- package-private helpers that reuse an existing connection (used for transactions) ----

    static Guardian insert(Connection connection, Guardian guardian) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, guardian.getDocumentType().getDbValue());
            statement.setString(2, guardian.getDocumentNumber());
            statement.setString(3, guardian.getFirstName());
            statement.setString(4, guardian.getMiddleName());
            statement.setString(5, guardian.getFirstSurname());
            statement.setString(6, guardian.getSecondSurname());
            statement.setString(7, guardian.getRelationship());
            statement.setString(8, guardian.getPrimaryPhone());
            statement.setString(9, guardian.getSecondaryPhone());
            statement.setString(10, guardian.getEmail());
            statement.setString(11, guardian.getAddress());
            statement.setString(12, guardian.getOccupation());
            statement.setBoolean(13, guardian.isFinanciallyResponsible());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    guardian.setId(keys.getInt(1));
                }
            }
            return guardian;
        }
    }

    static Optional<Guardian> findByDocument(Connection connection, String documentNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BASE + " WHERE a.numero_documento = ?")) {
            statement.setString(1, documentNumber);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private static Guardian mapRow(ResultSet rs) throws SQLException {
        Guardian guardian = new Guardian();
        guardian.setId(rs.getInt("id_acudiente"));
        guardian.setDocumentType(DocumentType.fromDbValue(rs.getString("tipo_documento")));
        guardian.setDocumentNumber(rs.getString("numero_documento"));
        guardian.setFirstName(rs.getString("primer_nombre"));
        guardian.setMiddleName(rs.getString("segundo_nombre"));
        guardian.setFirstSurname(rs.getString("primer_apellido"));
        guardian.setSecondSurname(rs.getString("segundo_apellido"));
        guardian.setRelationship(rs.getString("parentesco"));
        guardian.setPrimaryPhone(rs.getString("telefono_principal"));
        guardian.setSecondaryPhone(rs.getString("telefono_secundario"));
        guardian.setEmail(rs.getString("correo_electronico"));
        guardian.setAddress(rs.getString("direccion"));
        guardian.setOccupation(rs.getString("ocupacion"));
        guardian.setFinanciallyResponsible(rs.getBoolean("es_responsable_financiero"));
        return guardian;
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("GuardianDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
