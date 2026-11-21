package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectPerformanceRecord;

import java.util.List;

public interface AcademicPerformanceDao {

    /** Every academic record with its subject data (id, code and name). */
    List<SubjectPerformanceRecord> findAllRecordsWithSubject();
}
