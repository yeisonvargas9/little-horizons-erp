package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.LoanStatus;
import com.littlehorizons.erp.util.JdbcUtil;
import com.littlehorizons.erp.util.SqlExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link LoanDao}. Students and professors have separate loan tables. */
public class LoanDaoImpl implements LoanDao {

    private final SqlExecutor sql;

    public LoanDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public Loan register(Loan loan) {
        return sql.inTransaction(connection -> {
            int id = SqlExecutor.insert(connection, "INSERT INTO " + table(loan.borrowerType()) + " (id_recurso, "
                    + column(loan.borrowerType()) + ", fecha_prestamo, fecha_devolucion_esperada, estado_prestamo, "
                    + "observaciones) VALUES (?, ?, ?, ?, 'Activo', ?)", ps -> {
                ps.setInt(1, loan.resourceId());
                ps.setInt(2, loan.borrowerId());
                ps.setDate(3, JdbcUtil.toSqlDate(loan.loanDate()));
                ps.setDate(4, JdbcUtil.toSqlDate(loan.dueDate()));
                ps.setString(5, loan.notes());
            });
            int changed = SqlExecutor.update(connection,
                    "UPDATE recursos SET estado_recurso = 'Prestado' WHERE id_recurso = ? AND estado_recurso = 'Disponible'",
                    ps -> ps.setInt(1, loan.resourceId()));
            if (changed == 0) {
                throw new DataAccessException("El recurso ya no está disponible.");
            }
            return new Loan(id, loan.resourceId(), loan.resourceCode(), loan.resourceName(), loan.borrowerType(),
                    loan.borrowerId(), loan.borrowerName(), loan.loanDate(), loan.dueDate(), null, LoanStatus.ACTIVE,
                    loan.notes());
        });
    }

    @Override
    public void registerReturn(Loan loan, LocalDate returnDate, String notes) {
        sql.inTransaction(connection -> {
            SqlExecutor.update(connection, "UPDATE " + table(loan.borrowerType()) + " SET fecha_devolucion_real = ?, "
                    + "estado_prestamo = 'Devuelto', observaciones = COALESCE(?, observaciones) WHERE id_prestamo = ?", ps -> {
                ps.setDate(1, JdbcUtil.toSqlDate(returnDate));
                ps.setString(2, notes);
                ps.setInt(3, loan.id());
            });
            SqlExecutor.update(connection, "UPDATE recursos SET estado_recurso = 'Disponible' WHERE id_recurso = ?",
                    ps -> ps.setInt(1, loan.resourceId()));
            return null;
        });
    }

    @Override
    public Optional<Loan> findOpenByResource(int resourceId) {
        String query = selectOpen("E", "prestamos_estudiante", "id_estudiante", "estudiantes", " AND p.id_recurso = ?")
                + " UNION ALL "
                + selectOpen("P", "prestamos_profesor", "id_profesor", "profesores", " AND p.id_recurso = ?");
        return sql.query(query, ps -> {
            ps.setInt(1, resourceId);
            ps.setInt(2, resourceId);
        }, this::map).stream().findFirst();
    }

    @Override
    public List<Loan> findOpen() {
        String query = selectOpen("E", "prestamos_estudiante", "id_estudiante", "estudiantes", "")
                + " UNION ALL "
                + selectOpen("P", "prestamos_profesor", "id_profesor", "profesores", "")
                + " ORDER BY fecha_devolucion_esperada";
        return sql.query(query, SqlExecutor.NO_PARAMS, this::map);
    }

    @Override
    public int countOpenByBorrower(BorrowerType type, int borrowerId) {
        return sql.query("SELECT COUNT(*) FROM " + table(type) + " WHERE " + column(type)
                        + " = ? AND estado_prestamo IN ('Activo', 'Vencido')", ps -> ps.setInt(1, borrowerId),
                rs -> rs.getInt(1)).get(0);
    }

    @Override
    public int markOverdue(LocalDate today) {
        int changed = 0;
        for (BorrowerType type : BorrowerType.values()) {
            changed += sql.update("UPDATE " + table(type) + " SET estado_prestamo = 'Vencido' "
                    + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?",
                    ps -> ps.setDate(1, JdbcUtil.toSqlDate(today)));
        }
        return changed;
    }

    // ---------------------------------------------------------------- helpers

    private static String table(BorrowerType type) {
        return type == BorrowerType.STUDENT ? "prestamos_estudiante" : "prestamos_profesor";
    }

    private static String column(BorrowerType type) {
        return type == BorrowerType.STUDENT ? "id_estudiante" : "id_profesor";
    }

    /** SELECT of the open loans of one borrower type; the id column of the borrower table equals the loan column. */
    private static String selectOpen(String typeCode, String loanTable, String borrowerColumn, String borrowerTable,
                                     String extraWhere) {
        return "SELECT '" + typeCode + "' AS tipo, p.id_prestamo, p.id_recurso, r.codigo_recurso, r.nombre_recurso, p."
                + borrowerColumn + " AS id_prestatario, CONCAT_WS(' ', b.primer_nombre, b.primer_apellido) AS prestatario, "
                + "p.fecha_prestamo, p.fecha_devolucion_esperada, p.fecha_devolucion_real, p.estado_prestamo, p.observaciones "
                + "FROM " + loanTable + " p JOIN recursos r ON r.id_recurso = p.id_recurso JOIN " + borrowerTable
                + " b ON b." + borrowerColumn + " = p." + borrowerColumn
                + " WHERE p.estado_prestamo IN ('Activo', 'Vencido')" + extraWhere;
    }

    private Loan map(ResultSet rs) throws SQLException {
        return new Loan(rs.getInt("id_prestamo"), rs.getInt("id_recurso"), rs.getString("codigo_recurso"),
                rs.getString("nombre_recurso"), "E".equals(rs.getString("tipo")) ? BorrowerType.STUDENT : BorrowerType.PROFESSOR,
                rs.getInt("id_prestatario"), rs.getString("prestatario"), JdbcUtil.getLocalDate(rs, "fecha_prestamo"),
                JdbcUtil.getLocalDate(rs, "fecha_devolucion_esperada"), JdbcUtil.getLocalDate(rs, "fecha_devolucion_real"),
                LoanStatus.fromDbValue(rs.getString("estado_prestamo")), rs.getString("observaciones"));
    }
}
