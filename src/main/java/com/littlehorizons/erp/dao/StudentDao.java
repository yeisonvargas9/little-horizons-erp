package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.util.List;
import java.util.Optional;

public interface StudentDao extends BaseDao<Student, Integer> {

    Optional<Student> findByDocument(String documentNumber);

    List<Student> searchByName(String term);

    boolean updateStatus(int studentId, StudentStatus status);

    /**
     * Saves the student and links the guardian in ONE transaction: if the guardian does not exist yet
     * it is created, otherwise the existing one is reused. Either everything is saved or nothing is.
     */
    Student registerWithGuardian(Student student, Guardian guardian);
}
