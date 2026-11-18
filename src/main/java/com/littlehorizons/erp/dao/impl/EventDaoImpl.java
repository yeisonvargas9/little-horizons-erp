package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.JdbcUtil;
import com.littlehorizons.erp.util.SqlExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link EventDao} (table "eventos"). */
public class EventDaoImpl implements EventDao {

    private static final String SELECT =
            "SELECT v.id_evento, v.nombre_evento, v.descripcion, v.tipo_evento, v.fecha_evento, v.lugar, "
                    + "v.tiene_competencia, v.id_organizador, CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS organizador, "
                    + "v.esta_activo FROM eventos v LEFT JOIN profesores p ON p.id_profesor = v.id_organizador";

    private final SqlExecutor sql;

    public EventDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public SchoolEvent save(SchoolEvent event) {
        int id = sql.insert("INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
                + "tiene_competencia, id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, event.name());
            ps.setString(2, event.description());
            ps.setString(3, event.type().getDbValue());
            ps.setDate(4, JdbcUtil.toSqlDate(event.date()));
            ps.setString(5, event.place());
            ps.setBoolean(6, event.hasCompetition());
            if (event.organizerId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, event.organizerId());
            }
            ps.setBoolean(8, event.active());
        });
        return new SchoolEvent(id, event.name(), event.description(), event.type(), event.date(), event.place(),
                event.hasCompetition(), event.organizerId(), event.organizerName(), event.active());
    }

    @Override
    public Optional<SchoolEvent> findById(int id) {
        return sql.query(SELECT + " WHERE v.id_evento = ?", ps -> ps.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public List<SchoolEvent> findAll() {
        return sql.query(SELECT + " ORDER BY v.fecha_evento", SqlExecutor.NO_PARAMS, this::map);
    }

    private SchoolEvent map(ResultSet rs) throws SQLException {
        int organizerId = rs.getInt("id_organizador");
        Integer organizer = rs.wasNull() ? null : organizerId;
        return new SchoolEvent(rs.getInt("id_evento"), rs.getString("nombre_evento"), rs.getString("descripcion"),
                EventType.fromDbValue(rs.getString("tipo_evento")), JdbcUtil.getLocalDate(rs, "fecha_evento"),
                rs.getString("lugar"), rs.getBoolean("tiene_competencia"), organizer, rs.getString("organizador"),
                rs.getBoolean("esta_activo"));
    }
}
