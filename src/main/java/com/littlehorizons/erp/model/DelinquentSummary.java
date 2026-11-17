package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A student with charges unpaid for more than one month. */
public record DelinquentSummary(int studentId, String studentName, String studentDocument, int pendingCharges,
                                BigDecimal totalDue, LocalDate oldestDueDate) {
}
