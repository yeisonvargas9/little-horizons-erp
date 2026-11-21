package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** One academic record joined with its subject (result of the registros_academicos + materias query). */
public record SubjectPerformanceRecord(int recordId, BigDecimal value, Period period, int subjectId,
                                       String subjectCode, String subjectName) {
}
