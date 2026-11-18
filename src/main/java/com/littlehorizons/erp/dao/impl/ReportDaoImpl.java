package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.ConceptRevenue;
import com.littlehorizons.erp.model.DashboardData;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.GradeCount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.util.JdbcUtil;
import com.littlehorizons.erp.util.SqlExecutor;

import java.time.LocalDate;
import java.util.List;

/** JDBC implementation of {@link ReportDao}: aggregate queries (COUNT, SUM, AVG with GROUP BY). */
public class ReportDaoImpl implements ReportDao {

    private final SqlExecutor sql;

    public ReportDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public DashboardData dashboard(LocalDate monthStart, LocalDate today) {
        String query = "SELECT (SELECT COUNT(*) FROM estudiantes WHERE estado = 'Activo'), "
                + "(SELECT COUNT(*) FROM profesores WHERE estado = 'Activo'), "
                + "(SELECT COUNT(*) FROM clubes WHERE esta_activo = TRUE), "
                + "(SELECT COUNT(DISTINCT id_estudiante) FROM membresias_club WHERE estado_membresia = 'Activa'), "
                + "(SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE fecha_pago BETWEEN ? AND ?), "
                + "(SELECT COUNT(DISTINCT id_estudiante) FROM cargos WHERE estado_cargo <> 'Pagado' AND fecha_vencimiento < ?), "
                + "(SELECT COUNT(*) FROM prestamos_estudiante WHERE estado_prestamo IN ('Activo', 'Vencido') "
                + "AND fecha_devolucion_esperada < ?) + (SELECT COUNT(*) FROM prestamos_profesor "
                + "WHERE estado_prestamo IN ('Activo', 'Vencido') AND fecha_devolucion_esperada < ?)";
        return sql.query(query, ps -> {
            ps.setDate(1, JdbcUtil.toSqlDate(monthStart));
            ps.setDate(2, JdbcUtil.toSqlDate(today));
            ps.setDate(3, JdbcUtil.toSqlDate(today.minusMonths(1)));
            ps.setDate(4, JdbcUtil.toSqlDate(today));
            ps.setDate(5, JdbcUtil.toSqlDate(today));
        }, rs -> new DashboardData(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getBigDecimal(5),
                rs.getLong(6), rs.getLong(7))).get(0);
    }

    @Override
    public List<GradeCount> studentsPerGrade(int academicYear) {
        return sql.query("SELECT g.nombre_grado, COUNT(m.id_matricula) FROM grados g LEFT JOIN matriculas m "
                        + "ON m.id_grado = g.id_grado AND m.anio_academico = ? AND m.estado_matricula = 'Activa' "
                        + "GROUP BY g.id_grado, g.nombre_grado, g.nivel_numerico ORDER BY g.nivel_numerico",
                ps -> ps.setInt(1, academicYear), rs -> new GradeCount(rs.getString(1), rs.getLong(2)));
    }

    @Override
    public List<StudentAverage> studentAverages(int academicYear, Period period) {
        String query = "SELECT e.id_estudiante, CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                + "e.segundo_apellido) AS nombre, g.nombre_grado, AVG(r.valor_nota) AS promedio "
                + "FROM registros_academicos r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
                + "JOIN matriculas m ON m.id_estudiante = r.id_estudiante AND m.anio_academico = r.anio_academico "
                + "JOIN grados g ON g.id_grado = m.id_grado WHERE r.anio_academico = ? AND "
                + (period == null ? "r.periodo_academico <> 'Final'" : "r.periodo_academico = ?")
                + " GROUP BY e.id_estudiante, e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido, "
                + "g.nombre_grado ORDER BY promedio DESC";
        return sql.query(query, ps -> {
            ps.setInt(1, academicYear);
            if (period != null) {
                ps.setString(2, period.getDbValue());
            }
        }, rs -> new StudentAverage(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4)));
    }

    @Override
    public List<ConceptRevenue> revenueByConcept(LocalDate from, LocalDate to) {
        return sql.query("SELECT cp.nombre_concepto, COUNT(*), SUM(p.monto_pagado) FROM pagos p "
                        + "JOIN conceptos_pago cp ON cp.id_concepto = p.id_concepto WHERE p.fecha_pago BETWEEN ? AND ? "
                        + "GROUP BY cp.id_concepto, cp.nombre_concepto ORDER BY SUM(p.monto_pagado) DESC", ps -> {
                    ps.setDate(1, JdbcUtil.toSqlDate(from));
                    ps.setDate(2, JdbcUtil.toSqlDate(to));
                }, rs -> new ConceptRevenue(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)));
    }

    @Override
    public List<NamedCount> clubMembers() {
        return sql.query("SELECT c.nombre_club, COUNT(m.id_membresia) AS miembros FROM clubes c LEFT JOIN membresias_club m "
                        + "ON m.id_club = c.id_club AND m.estado_membresia = 'Activa' GROUP BY c.id_club, c.nombre_club "
                        + "ORDER BY miembros DESC, c.nombre_club", SqlExecutor.NO_PARAMS,
                rs -> new NamedCount(rs.getString(1), rs.getLong(2)));
    }

    @Override
    public List<EventParticipation> eventParticipation() {
        return sql.query("SELECT v.nombre_evento, v.fecha_evento, COUNT(p.id_participacion) FROM eventos v "
                        + "LEFT JOIN participantes_evento p ON p.id_evento = v.id_evento GROUP BY v.id_evento, "
                        + "v.nombre_evento, v.fecha_evento ORDER BY v.fecha_evento", SqlExecutor.NO_PARAMS,
                rs -> new EventParticipation(rs.getString(1), JdbcUtil.getLocalDate(rs, "fecha_evento"), rs.getLong(3)));
    }

    @Override
    public List<NamedCount> resourcesByStatus() {
        return sql.query("SELECT estado_recurso, COUNT(*) FROM recursos GROUP BY estado_recurso ORDER BY COUNT(*) DESC",
                SqlExecutor.NO_PARAMS, rs -> new NamedCount(rs.getString(1), rs.getLong(2)));
    }
}
