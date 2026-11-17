package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A student's enrollment in one grade for one academic year (table "matriculas"). */
public record Enrollment(int id, int studentId, int gradeId, String gradeName, int academicYear,
                         EnrollmentStatus status, LocalDate enrollmentDate) {
}
