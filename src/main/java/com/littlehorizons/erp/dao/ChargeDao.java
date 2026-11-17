package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Charge;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChargeDao {

    Charge save(Charge charge);

    Optional<Charge> findById(int chargeId);

    boolean existsByDescription(int studentId, int conceptId, String description);

    /** Unpaid charges (pending or overdue) of a student, oldest due date first. */
    List<Charge> findUnpaidByStudent(int studentId);

    /** Unpaid charges of every student whose due date is before the given date. */
    List<Charge> findUnpaidDueBefore(LocalDate date);

    /** Marks as overdue every pending charge whose due date has already passed. Returns how many changed. */
    int markOverdue(LocalDate today);
}
