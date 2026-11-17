package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.StudentSummary;

import java.util.List;
import java.util.Optional;

public interface EnrollmentDao {

    Enrollment save(Enrollment enrollment);

    /** All enrollments of a student, newest year first. */
    List<Enrollment> findByStudent(int studentId);

    Optional<Enrollment> findByStudentAndYear(int studentId, int academicYear);

    /** Active students with an active enrollment in the grade for that year. */
    List<StudentSummary> findStudentsByGrade(int gradeId, int academicYear);
}
