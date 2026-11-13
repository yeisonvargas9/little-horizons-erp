package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Optional;

/** Read-only DAO: subjects are a fixed catalog, so this interface has no save/update (Interface Segregation). */
public interface SubjectDao {

    Optional<Subject> findById(int id);

    List<Subject> findAll();

    List<Subject> findByGradeId(int gradeId);
}
