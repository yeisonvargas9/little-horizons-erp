package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade (nota) given to a student in a subject for a period (table "registros_academicos"). */
public record AcademicRecord(int id, int studentId, int subjectId, String subjectName, int professorId,
                             int academicYear, Period period, BigDecimal value, String observations,
                             LocalDate recordDate) {
}
