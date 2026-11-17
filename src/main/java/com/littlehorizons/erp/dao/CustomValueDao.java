package com.littlehorizons.erp.dao;

import java.math.BigDecimal;
import java.util.Optional;

/** Per-student price adjustments (sibling discounts, scholarships). */
public interface CustomValueDao {

    Optional<BigDecimal> findAmount(int studentId, int conceptId, int academicYear);

    void saveOrUpdate(int studentId, int conceptId, BigDecimal amount, String reason, int academicYear);
}
