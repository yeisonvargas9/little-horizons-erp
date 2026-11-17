package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.ChargeStatus;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ChargeDao} (table "cargos"). */
public class ChargeDaoImpl implements ChargeDao {

    private static final String SELECT_BASE =
            "SELECT c.id_cargo, c.id_estudiante, CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, "
                    + "e.primer_apellido, e.segundo_apellido) AS nombre, e.numero_documento, c.id_concepto, "
                    + "cp.nombre_concepto, c.descripcion, c.monto_cargo, c.fecha_emision, c.fecha_vencimiento, "
                    + "c.estado_cargo, c.anio_academico, c.periodo_academico FROM cargos c "
                    + "JOIN estudiantes e ON e.id_estudiante = c.id_estudiante "
                    + "JOIN conceptos_pago cp ON cp.id_concepto = c.id_concepto";

    private final DatabaseConnection database;

    public ChargeDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Charge save(Charge charge) {
        String sql = "INSERT INTO cargos (id_estudiante, id_concepto, descripcion, monto_cargo, fecha_emision, "
                + "fecha_vencimiento, estado_cargo, anio_academico, periodo_academico) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, charge.studentId());
            statement.setInt(2, charge.conceptId());
            statement.setString(3, charge.description());
            statement.setBigDecimal(4, charge.amount());
            statement.setDate(5, JdbcUtil.toSqlDate(charge.issueDate()));
            statement.setDate(6, JdbcUtil.toSqlDate(charge.dueDate()));
            statement.setString(7, charge.status().getDbValue());
            statement.setInt(8, charge.academicYear());
            statement.setString(9, charge.period() == null ? null : charge.period().getDbValue());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new Charge(id, charge.studentId(), charge.studentName(), charge.studentDocument(),
                        charge.conceptId(), charge.conceptName(), charge.description(), charge.amount(),
                        charge.issueDate(), charge.dueDate(), charge.status(), charge.academicYear(), charge.period());
            }
        } catch (SQLException e) {
            throw fail("guardar el cargo", e);
        }
    }

    @Override
    public Optional<Charge> findById(int chargeId) {
        return query(SELECT_BASE + " WHERE c.id_cargo = ?", chargeId).stream().findFirst();
    }

    @Override
    public boolean existsByDescription(int studentId, int conceptId, String description) {
        String sql = "SELECT COUNT(*) FROM cargos WHERE id_estudiante = ? AND id_concepto = ? AND descripcion = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, conceptId);
            statement.setString(3, description);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw fail("verificar el cargo", e);
        }
    }

    @Override
    public List<Charge> findUnpaidByStudent(int studentId) {
        return query(SELECT_BASE + " WHERE c.id_estudiante = ? AND c.estado_cargo <> 'Pagado' "
                + "ORDER BY c.fecha_vencimiento, c.id_cargo", studentId);
    }

    @Override
    public List<Charge> findUnpaidDueBefore(LocalDate date) {
        String sql = SELECT_BASE + " WHERE c.estado_cargo <> 'Pagado' AND c.fecha_vencimiento < ? "
                + "ORDER BY c.id_estudiante, c.fecha_vencimiento";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, JdbcUtil.toSqlDate(date));
            try (ResultSet rs = statement.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw fail("consultar los cargos vencidos", e);
        }
    }

    @Override
    public int markOverdue(LocalDate today) {
        String sql = "UPDATE cargos SET estado_cargo = 'Vencido' WHERE estado_cargo = 'Pendiente' AND fecha_vencimiento < ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, JdbcUtil.toSqlDate(today));
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw fail("actualizar los cargos vencidos", e);
        }
    }

    private List<Charge> query(String sql, int parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parameter);
            try (ResultSet rs = statement.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw fail("consultar los cargos", e);
        }
    }

    private List<Charge> mapAll(ResultSet rs) throws SQLException {
        List<Charge> charges = new ArrayList<>();
        while (rs.next()) {
            String period = rs.getString("periodo_academico");
            charges.add(new Charge(rs.getInt("id_cargo"), rs.getInt("id_estudiante"), rs.getString("nombre"),
                    rs.getString("numero_documento"), rs.getInt("id_concepto"), rs.getString("nombre_concepto"),
                    rs.getString("descripcion"), rs.getBigDecimal("monto_cargo"),
                    JdbcUtil.getLocalDate(rs, "fecha_emision"), JdbcUtil.getLocalDate(rs, "fecha_vencimiento"),
                    ChargeStatus.fromDbValue(rs.getString("estado_cargo")), rs.getInt("anio_academico"),
                    period == null ? null : Period.fromDbValue(period)));
        }
        return charges;
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("ChargeDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
