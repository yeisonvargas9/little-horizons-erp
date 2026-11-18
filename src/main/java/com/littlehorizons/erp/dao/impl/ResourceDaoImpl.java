package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.util.JdbcUtil;
import com.littlehorizons.erp.util.SqlExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ResourceDao} (tables "recursos" and "categorias_recurso"). */
public class ResourceDaoImpl implements ResourceDao {

    private static final String SELECT =
            "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.id_categoria, c.nombre_categoria, r.descripcion, "
                    + "r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso FROM recursos r "
                    + "JOIN categorias_recurso c ON c.id_categoria = r.id_categoria";

    private final SqlExecutor sql;

    public ResourceDaoImpl(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public Resource save(Resource resource) {
        int id = sql.insert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
                + "fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, resource.code());
            ps.setString(2, resource.name());
            ps.setInt(3, resource.categoryId());
            ps.setString(4, resource.description());
            ps.setDate(5, JdbcUtil.toSqlDate(resource.acquisitionDate()));
            ps.setString(6, resource.location());
            ps.setString(7, resource.status().getDbValue());
        });
        return new Resource(id, resource.code(), resource.name(), resource.categoryId(), resource.categoryName(),
                resource.description(), resource.acquisitionDate(), resource.location(), resource.status());
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return sql.query(SELECT + " WHERE r.codigo_recurso = ?", ps -> ps.setString(1, code), this::map).stream().findFirst();
    }

    @Override
    public List<Resource> findAll() {
        return sql.query(SELECT + " ORDER BY r.codigo_recurso", SqlExecutor.NO_PARAMS, this::map);
    }

    @Override
    public List<ResourceCategory> findCategories() {
        return sql.query("SELECT id_categoria, nombre_categoria FROM categorias_recurso ORDER BY id_categoria",
                SqlExecutor.NO_PARAMS, rs -> new ResourceCategory(rs.getInt(1), rs.getString(2)));
    }

    @Override
    public boolean updateStatus(int resourceId, ResourceStatus status) {
        return sql.update("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", ps -> {
            ps.setString(1, status.getDbValue());
            ps.setInt(2, resourceId);
        }) > 0;
    }

    private Resource map(ResultSet rs) throws SQLException {
        return new Resource(rs.getInt("id_recurso"), rs.getString("codigo_recurso"), rs.getString("nombre_recurso"),
                rs.getInt("id_categoria"), rs.getString("nombre_categoria"), rs.getString("descripcion"),
                JdbcUtil.getLocalDate(rs, "fecha_adquisicion"), rs.getString("ubicacion_fisica"),
                ResourceStatus.fromDbValue(rs.getString("estado_recurso")));
    }
}
