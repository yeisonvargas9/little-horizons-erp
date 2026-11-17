package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
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

/** JDBC implementation of {@link InvoiceDao} (tables "facturas" and "detalle_factura"). */
public class InvoiceDaoImpl implements InvoiceDao {

    private static final String SELECT_BASE =
            "SELECT id_factura, numero_factura, id_acudiente, fecha_emision, subtotal, iva, total, cufe, "
                    + "contenido_qr, nombre_archivo_pdf, ruta_archivo FROM facturas";

    private final DatabaseConnection database;

    public InvoiceDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public int nextSequence() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(id_factura), 0) + 1 FROM facturas");
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            throw fail("calcular el consecutivo de la factura", e);
        }
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASE + " WHERE numero_factura = ?")) {
            statement.setString(1, number);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Invoice header = mapRow(rs);
                return Optional.of(header.withId(header.id(), loadLines(connection, header.id())));
            }
        } catch (SQLException e) {
            throw fail("consultar la factura", e);
        }
    }

    @Override
    public List<Invoice> findByGuardian(int guardianId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_BASE + " WHERE id_acudiente = ? ORDER BY fecha_emision DESC, id_factura DESC")) {
            statement.setInt(1, guardianId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Invoice> invoices = new ArrayList<>();
                while (rs.next()) {
                    invoices.add(mapRow(rs));
                }
                return invoices;
            }
        } catch (SQLException e) {
            throw fail("listar las facturas del acudiente", e);
        }
    }

    @Override
    public void updatePdfLocation(int invoiceId, String fileName, String path) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE facturas SET nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?")) {
            statement.setString(1, fileName);
            statement.setString(2, path);
            statement.setInt(3, invoiceId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw fail("guardar la ruta de la factura", e);
        }
    }

    private List<InvoiceLine> loadLines(Connection connection, int invoiceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT descripcion, cantidad, valor_unitario, subtotal, id_pago FROM detalle_factura "
                        + "WHERE id_factura = ? ORDER BY id_detalle")) {
            statement.setInt(1, invoiceId);
            try (ResultSet rs = statement.executeQuery()) {
                List<InvoiceLine> lines = new ArrayList<>();
                while (rs.next()) {
                    int paymentId = rs.getInt("id_pago");
                    lines.add(new InvoiceLine(rs.getString("descripcion"), rs.getInt("cantidad"),
                            rs.getBigDecimal("valor_unitario"), rs.getBigDecimal("subtotal"),
                            rs.wasNull() ? null : paymentId));
                }
                return lines;
            }
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        String number = rs.getString("numero_factura");
        int sequence = Integer.parseInt(number.substring(number.lastIndexOf('-') + 1));
        return new Invoice(rs.getInt("id_factura"), sequence, number, rs.getInt("id_acudiente"),
                JdbcUtil.getLocalDate(rs, "fecha_emision"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("iva"),
                rs.getBigDecimal("total"), rs.getString("cufe"), rs.getString("contenido_qr"),
                rs.getString("nombre_archivo_pdf"), rs.getString("ruta_archivo"), List.of());
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("InvoiceDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
