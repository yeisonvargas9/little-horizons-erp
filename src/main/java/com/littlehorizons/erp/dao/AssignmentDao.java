package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ScheduleSlot;

import java.util.List;

public interface AssignmentDao {

    /** Saves the assignment and all its schedule slots in one transaction. */
    Assignment save(Assignment assignment);

    boolean exists(int professorId, int subjectId, int academicYear, String section);

    List<Assignment> findByProfessor(int professorId, int academicYear);

    /** True if the professor already teaches something that overlaps the slot. */
    boolean hasProfessorConflict(int professorId, int academicYear, ScheduleSlot slot);

    /** True if the same grade and section already has a class that overlaps the slot. */
    boolean hasGroupConflict(int gradeId, int academicYear, String section, ScheduleSlot slot);
}
