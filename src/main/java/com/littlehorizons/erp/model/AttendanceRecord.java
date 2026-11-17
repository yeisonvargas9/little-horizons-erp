package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** One attendance mark for a student in a subject on a day (table "asistencias"). */
public record AttendanceRecord(int studentId, int subjectId, LocalDate date, AttendanceStatus status,
                               String notes, Integer registeredBy) {
}
