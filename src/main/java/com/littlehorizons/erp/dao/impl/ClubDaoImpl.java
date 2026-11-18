package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.util.SqlExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ClubDao} (table "clubes"). */
public class ClubDaoImpl implements ClubDao {

    private static final String SELECT =
            "SELECT c.id_club, c.nombre_club, c.descripcion, c.id_profesor_supervisor, "
                    + "CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS supervisor, c.capacidad_maxima, "
                    + "c.horario_reunion, c.esta_activo, (SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club "
                    + "AND m.estado_membresia = 'Activa') AS miembros FROM clubes c "
                    + "LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor";

    private final SqlExecutor sql;

    public ClubDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public Club save(Club club) {
        int id = sql.insert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, "
                + "horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, club.name());
            ps.setString(2, club.description());
            if (club.supervisorId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, club.supervisorId());
            }
            ps.setInt(4, club.capacity());
            ps.setString(5, club.meetingSchedule());
            ps.setBoolean(6, club.active());
        });
        return new Club(id, club.name(), club.description(), club.supervisorId(), club.supervisorName(), club.capacity(),
                club.meetingSchedule(), club.active(), 0);
    }

    @Override
    public Optional<Club> findById(int id) {
        return sql.query(SELECT + " WHERE c.id_club = ?", ps -> ps.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public Optional<Club> findByName(String name) {
        return sql.query(SELECT + " WHERE c.nombre_club = ?", ps -> ps.setString(1, name), this::map).stream().findFirst();
    }

    @Override
    public List<Club> findAll() {
        return sql.query(SELECT + " ORDER BY c.nombre_club", SqlExecutor.NO_PARAMS, this::map);
    }

    private Club map(ResultSet rs) throws SQLException {
        int supervisorId = rs.getInt("id_profesor_supervisor");
        Integer supervisor = rs.wasNull() ? null : supervisorId;
        return new Club(rs.getInt("id_club"), rs.getString("nombre_club"), rs.getString("descripcion"), supervisor,
                rs.getString("supervisor"), rs.getInt("capacidad_maxima"), rs.getString("horario_reunion"),
                rs.getBoolean("esta_activo"), rs.getInt("miembros"));
    }
}
