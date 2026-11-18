package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ParticipationDao;
import com.littlehorizons.erp.model.Participation;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.util.SqlExecutor;

import java.util.List;

/** JDBC implementation of {@link ParticipationDao} (table "participantes_evento"). */
public class ParticipationDaoImpl implements ParticipationDao {

    private final SqlExecutor sql;

    public ParticipationDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public Participation save(Participation participation) {
        int id = sql.insert("INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, "
                + "posicion_obtenida) VALUES (?, ?, ?, ?)", ps -> {
            ps.setInt(1, participation.eventId());
            ps.setInt(2, participation.studentId());
            ps.setString(3, participation.role().getDbValue());
            ps.setString(4, participation.position());
        });
        return new Participation(id, participation.eventId(), participation.studentId(), participation.studentName(),
                participation.role(), participation.position());
    }

    @Override
    public boolean exists(int eventId, int studentId) {
        return sql.query("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND id_estudiante = ?", ps -> {
            ps.setInt(1, eventId);
            ps.setInt(2, studentId);
        }, rs -> rs.getInt(1)).get(0) > 0;
    }

    @Override
    public boolean isPositionTaken(int eventId, String position) {
        return sql.query("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND posicion_obtenida = ?", ps -> {
            ps.setInt(1, eventId);
            ps.setString(2, position);
        }, rs -> rs.getInt(1)).get(0) > 0;
    }

    @Override
    public List<Participation> findByEvent(int eventId) {
        return sql.query("SELECT p.id_participacion, p.id_evento, p.id_estudiante, CONCAT_WS(' ', e.primer_nombre, "
                + "e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre, p.rol_participacion, "
                + "p.posicion_obtenida FROM participantes_evento p JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
                + "WHERE p.id_evento = ? ORDER BY FIELD(p.posicion_obtenida, '1er Lugar', '2do Lugar', '3er Lugar') = 0, "
                + "FIELD(p.posicion_obtenida, '1er Lugar', '2do Lugar', '3er Lugar'), "
                + "e.primer_apellido", ps -> ps.setInt(1, eventId),
                rs -> new Participation(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4),
                        ParticipationRole.fromDbValue(rs.getString(5)), rs.getString(6)));
    }
}
