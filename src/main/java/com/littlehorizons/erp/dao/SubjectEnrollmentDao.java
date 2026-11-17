package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Period;

import java.util.List;

public interface SubjectEnrollmentDao {

    /** True if the student is enrolled in the subject; a null period means "in any period of the year". */
    boolean isEnrolled(int studentId, int subjectId, int academicYear, Period period);

    /** Enrolls the student in all the given subjects in ONE transaction. */
    void enrollMany(int studentId, List<Integer> subjectIds, int academicYear, Period period, String section);
}
