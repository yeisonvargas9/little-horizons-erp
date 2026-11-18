package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.MembershipDao;
import com.littlehorizons.erp.model.Membership;
import com.littlehorizons.erp.model.MembershipStatus;
import com.littlehorizons.erp.util.JdbcUtil;
import com.littlehorizons.erp.util.SqlExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link MembershipDao} (table "membresias_club"). */
public class MembershipDaoImpl implements MembershipDao {

    private static final String SELECT =
            "SELECT m.id_membresia, m.id_estudiante, CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                    + "e.segundo_apellido) AS nombre, m.id_club, m.fecha_ingreso, m.estado_membresia, m.fecha_retiro, "
                    + "m.motivo_retiro FROM membresias_club m JOIN estudiantes e ON e.id_estudiante = m.id_estudiante";

    private final SqlExecutor sql;

    public MembershipDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public boolean enrollIfRoom(int studentId, int clubId, LocalDate joinDate) {
        String insert = "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia) "
                + "SELECT ?, c.id_club, ?, 'Activa' FROM clubes c WHERE c.id_club = ? AND "
                + "(SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') "
                + "< c.capacidad_maxima";
        return sql.update(insert, ps -> {
            ps.setInt(1, studentId);
            ps.setDate(2, JdbcUtil.toSqlDate(joinDate));
            ps.setInt(3, clubId);
        }) > 0;
    }

    @Override
    public Optional<Membership> findActive(int studentId, int clubId) {
        return sql.query(SELECT + " WHERE m.id_estudiante = ? AND m.id_club = ? AND m.estado_membresia = 'Activa'", ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, clubId);
        }, this::map).stream().findFirst();
    }

    @Override
    public boolean withdraw(int membershipId, LocalDate leaveDate, String reason) {
        return sql.update("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, motivo_retiro = ? "
                + "WHERE id_membresia = ? AND estado_membresia = 'Activa'", ps -> {
            ps.setDate(1, JdbcUtil.toSqlDate(leaveDate));
            ps.setString(2, reason);
            ps.setInt(3, membershipId);
        }) > 0;
    }

    @Override
    public List<Membership> findByClub(int clubId, boolean activeOnly) {
        String where = " WHERE m.id_club = ?" + (activeOnly ? " AND m.estado_membresia = 'Activa'" : "");
        return sql.query(SELECT + where + " ORDER BY e.primer_apellido, e.primer_nombre", ps -> ps.setInt(1, clubId), this::map);
    }

    private Membership map(ResultSet rs) throws SQLException {
        return new Membership(rs.getInt("id_membresia"), rs.getInt("id_estudiante"), rs.getString("nombre"),
                rs.getInt("id_club"), JdbcUtil.getLocalDate(rs, "fecha_ingreso"),
                MembershipStatus.fromDbValue(rs.getString("estado_membresia")), JdbcUtil.getLocalDate(rs, "fecha_retiro"),
                rs.getString("motivo_retiro"));
    }
}
