package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AbsenceSummary;
import com.littlehorizons.erp.model.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceDao {

    /** Saves (or corrects) all the marks of a class in ONE transaction. */
    void saveAll(List<AttendanceRecord> records);

    /** Students with at least {@code minDays} different absent days between the two dates (inclusive). */
    List<AbsenceSummary> findAbsenceSummary(LocalDate from, LocalDate to, int minDays);
}
