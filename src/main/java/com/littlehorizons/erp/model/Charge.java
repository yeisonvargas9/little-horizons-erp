package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Something a student owes (table "cargos"): the base to detect who is behind with payments. */
public record Charge(int id, int studentId, String studentName, String studentDocument, int conceptId,
                     String conceptName, String description, BigDecimal amount, LocalDate issueDate,
                     LocalDate dueDate, ChargeStatus status, int academicYear, Period period) {
}
