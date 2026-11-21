package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.AcademicPerformanceDao;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.SubjectPerformanceRecord;
import com.littlehorizons.erp.util.SqlExecutor;

import java.util.List;

/** JDBC implementation: joins "registros_academicos" with "materias" through the id_materia foreign key. */
public class AcademicPerformanceDaoImpl implements AcademicPerformanceDao {

    private static final String QUERY =
            "SELECT r.id_registro, r.valor_nota, r.periodo_academico, m.id_materia, m.codigo_materia, m.nombre_materia "
                    + "FROM registros_academicos r JOIN materias m ON m.id_materia = r.id_materia "
                    + "ORDER BY m.codigo_materia, r.id_registro";

    private final SqlExecutor sql;

    public AcademicPerformanceDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<SubjectPerformanceRecord> findAllRecordsWithSubject() {
        return sql.query(QUERY, SqlExecutor.NO_PARAMS, rs -> new SubjectPerformanceRecord(
                rs.getInt("id_registro"), rs.getBigDecimal("valor_nota"),
                Period.fromDbValue(rs.getString("periodo_academico")), rs.getInt("id_materia"),
                rs.getString("codigo_materia"), rs.getString("nombre_materia")));
    }
}
