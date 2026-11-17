package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ConceptDao} (table "conceptos_pago"). */
public class ConceptDaoImpl implements ConceptDao {

    private static final String SELECT_BASE =
            "SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, "
                    + "aplica_iva, esta_activo FROM conceptos_pago";

    private final DatabaseConnection database;

    public ConceptDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public PaymentConcept save(PaymentConcept concept) {
        String sql = "INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, "
                + "es_recurrente, aplica_iva, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, concept.code());
            statement.setString(2, concept.name());
            statement.setString(3, concept.description());
            statement.setBigDecimal(4, concept.baseAmount());
            statement.setBoolean(5, concept.recurring());
            statement.setBoolean(6, concept.appliesVat());
            statement.setBoolean(7, concept.active());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new PaymentConcept(id, concept.code(), concept.name(), concept.description(),
                        concept.baseAmount(), concept.recurring(), concept.appliesVat(), concept.active());
            }
        } catch (SQLException e) {
            throw fail("guardar el concepto de pago", e);
        }
    }

    @Override
    public Optional<PaymentConcept> findById(int id) {
        return query(SELECT_BASE + " WHERE id_concepto = ?", String.valueOf(id), true).stream().findFirst();
    }

    @Override
    public Optional<PaymentConcept> findByCode(String code) {
        return query(SELECT_BASE + " WHERE codigo_concepto = ?", code, false).stream().findFirst();
    }

    @Override
    public List<PaymentConcept> findAll() {
        return query(SELECT_BASE + " ORDER BY id_concepto", null, false);
    }

    @Override
    public boolean updateBaseAmount(int conceptId, BigDecimal newAmount) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE conceptos_pago SET monto_base = ? WHERE id_concepto = ?")) {
            statement.setBigDecimal(1, newAmount);
            statement.setInt(2, conceptId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail("actualizar el valor del concepto", e);
        }
    }

    private List<PaymentConcept> query(String sql, String parameter, boolean numeric) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                if (numeric) {
                    statement.setInt(1, Integer.parseInt(parameter));
                } else {
                    statement.setString(1, parameter);
                }
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<PaymentConcept> concepts = new ArrayList<>();
                while (rs.next()) {
                    concepts.add(new PaymentConcept(rs.getInt("id_concepto"), rs.getString("codigo_concepto"),
                            rs.getString("nombre_concepto"), rs.getString("descripcion"), rs.getBigDecimal("monto_base"),
                            rs.getBoolean("es_recurrente"), rs.getBoolean("aplica_iva"), rs.getBoolean("esta_activo")));
                }
                return concepts;
            }
        } catch (SQLException e) {
            throw fail("consultar los conceptos de pago", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("ConceptDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
