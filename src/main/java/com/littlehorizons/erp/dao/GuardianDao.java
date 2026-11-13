package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;

import java.util.List;
import java.util.Optional;

public interface GuardianDao extends BaseDao<Guardian, Integer> {

    Optional<Guardian> findByDocument(String documentNumber);

    List<Guardian> findByStudentId(int studentId);
}
