package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ProfessorStatus;

import java.util.Optional;

public interface ProfessorDao extends BaseDao<Professor, Integer> {

    Optional<Professor> findByDocument(String documentNumber);

    boolean existsByEmail(String email);

    boolean updateStatus(int professorId, ProfessorStatus status);
}
