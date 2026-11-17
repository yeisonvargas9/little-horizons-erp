package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.BillingResult;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.Period;
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

/** JDBC implementation of {@link PaymentDao} (tables "pagos", "cargos", "facturas" and "detalle_factura"). */
public class PaymentDaoImpl implements PaymentDao {

    private static final String SELECT_BASE =
            "SELECT p.id_pago, p.id_estudiante, CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, "
                    + "e.primer_apellido, e.segundo_apellido) AS nombre, p.id_acudiente, p.id_concepto, "
                    + "cp.nombre_concepto, p.id_cargo, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.numero_recibo, "
                    + "p.anio_academico, p.periodo_academico, p.notas FROM pagos p "
                    + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
                    + "JOIN conceptos_pago cp ON cp.id_concepto = p.id_concepto";

    private final DatabaseConnection database;

    public PaymentDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public BillingResult registerPayments(List<Payment> payments, Invoice invoice) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Payment> saved = new ArrayList<>();
                for (Payment payment : payments) {
                    int id = insertPayment(connection, payment);
                    String receipt = String.format("REC-%06d", id);
                    setReceipt(connection, id, receipt);
                    if (payment.chargeId() != null) {
                        markChargePaid(connection, payment.chargeId());
                    }
                    saved.add(payment.withIdAndReceipt(id, receipt));
                }
                int invoiceId = insertInvoice(connection, invoice);
                List<InvoiceLine> lines = new ArrayList<>();
                for (int i = 0; i < invoice.lines().size(); i++) {
                    InvoiceLine line = invoice.lines().get(i).withPaymentId(saved.get(i).id());
                    insertLine(connection, invoiceId, line);
                    lines.add(line);
                }
                connection.commit();
                return new BillingResult(invoice.withId(invoiceId, lines), saved);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("registrar el pago y la factura", e);
        }
    }

    @Override
    public List<Payment> findByStudent(int studentId) {
        return query(SELECT_BASE + " WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC", studentId);
    }

    @Override
    public List<Payment> findByGuardian(int guardianId) {
        return query(SELECT_BASE + " WHERE p.id_acudiente = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC", guardianId);
    }

    private int insertPayment(Connection connection, Payment payment) throws SQLException {
        String sql = "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, id_cargo, monto_pagado, fecha_pago, "
                + "metodo_pago, anio_academico, periodo_academico, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, payment.studentId());
            statement.setInt(2, payment.guardianId());
            statement.setInt(3, payment.conceptId());
            if (payment.chargeId() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, payment.chargeId());
            }
            statement.setBigDecimal(5, payment.amount());
            statement.setDate(6, JdbcUtil.toSqlDate(payment.paymentDate()));
            statement.setString(7, payment.method().getDbValue());
            statement.setInt(8, payment.academicYear());
            statement.setString(9, payment.period() == null ? null : payment.period().getDbValue());
            statement.setString(10, payment.notes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void setReceipt(Connection connection, int paymentId, String receipt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE pagos SET numero_recibo = ? WHERE id_pago = ?")) {
            statement.setString(1, receipt);
            statement.setInt(2, paymentId);
            statement.executeUpdate();
        }
    }

    private void markChargePaid(Connection connection, int chargeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cargos SET estado_cargo = 'Pagado' WHERE id_cargo = ? AND estado_cargo <> 'Pagado'")) {
            statement.setInt(1, chargeId);
            if (statement.executeUpdate() == 0) {
                throw new DataAccessException("El cargo " + chargeId + " ya fue pagado por otro usuario.");
            }
        }
    }

    private int insertInvoice(Connection connection, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO facturas (numero_factura, id_acudiente, fecha_emision, subtotal, iva, total, cufe, "
                + "contenido_qr) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, invoice.number());
            statement.setInt(2, invoice.guardianId());
            statement.setDate(3, JdbcUtil.toSqlDate(invoice.issueDate()));
            statement.setBigDecimal(4, invoice.subtotal());
            statement.setBigDecimal(5, invoice.vat());
            statement.setBigDecimal(6, invoice.total());
            statement.setString(7, invoice.cufe());
            statement.setString(8, invoice.qrContent());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertLine(Connection connection, int invoiceId, InvoiceLine line) throws SQLException {
        String sql = "INSERT INTO detalle_factura (id_factura, id_pago, descripcion, cantidad, valor_unitario, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, invoiceId);
            statement.setInt(2, line.paymentId());
            statement.setString(3, line.description());
            statement.setInt(4, line.quantity());
            statement.setBigDecimal(5, line.unitPrice());
            statement.setBigDecimal(6, line.subtotal());
            statement.executeUpdate();
        }
    }

    private List<Payment> query(String sql, int parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parameter);
            try (ResultSet rs = statement.executeQuery()) {
                List<Payment> payments = new ArrayList<>();
                while (rs.next()) {
                    int chargeId = rs.getInt("id_cargo");
                    Integer charge = rs.wasNull() ? null : chargeId;
                    String period = rs.getString("periodo_academico");
                    payments.add(new Payment(rs.getInt("id_pago"), rs.getInt("id_estudiante"), rs.getString("nombre"),
                            rs.getInt("id_acudiente"), rs.getInt("id_concepto"), rs.getString("nombre_concepto"), charge,
                            rs.getBigDecimal("monto_pagado"), JdbcUtil.getLocalDate(rs, "fecha_pago"),
                            PaymentMethod.fromDbValue(rs.getString("metodo_pago")), rs.getString("numero_recibo"),
                            rs.getInt("anio_academico"), period == null ? null : Period.fromDbValue(period),
                            rs.getString("notas")));
                }
                return payments;
            }
        } catch (SQLException e) {
            throw fail("consultar los pagos", e);
        }
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("PaymentDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
