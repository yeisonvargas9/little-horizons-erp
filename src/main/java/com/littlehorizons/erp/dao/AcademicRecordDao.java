package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Period;

import java.util.List;
import java.util.Optional;

public interface AcademicRecordDao {

    /** Inserts the grade, or corrects it if that student/subject/year/period already has one. */
    void saveOrUpdate(AcademicRecord record);

    /** Complete academic history of a student, ordered by year, period and subject. */
    List<AcademicRecord> findByStudent(int studentId);

    /** Average of all grades of a subject in a year; a null period means all periods. */
    Optional<Double> averageBySubject(int subjectId, int academicYear, Period period);
}
