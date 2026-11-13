package com.littlehorizons.erp.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic contract shared by every DAO (Data Access Object).
 * Controllers depend on these interfaces, never on the JDBC implementations (Dependency Inversion).
 */
public interface BaseDao<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean update(T entity);
}
