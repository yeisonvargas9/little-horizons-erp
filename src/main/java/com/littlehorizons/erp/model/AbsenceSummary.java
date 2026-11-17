package com.littlehorizons.erp.model;

/** Number of days a student was absent in a date range. */
public record AbsenceSummary(int studentId, String fullName, long absentDays) {
}
