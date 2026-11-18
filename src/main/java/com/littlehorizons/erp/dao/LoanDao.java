package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanDao {

    /** Saves the loan and marks the resource as "Prestado" in ONE transaction. */
    Loan register(Loan loan);

    /** Saves the return and marks the resource as "Disponible" in ONE transaction. */
    void registerReturn(Loan loan, LocalDate returnDate, String notes);

    Optional<Loan> findOpenByResource(int resourceId);

    /** Loans not returned yet (active or overdue), students and professors together. */
    List<Loan> findOpen();

    int countOpenByBorrower(BorrowerType type, int borrowerId);

    /** Marks as overdue every active loan whose expected return date has passed. Returns how many changed. */
    int markOverdue(LocalDate today);
}
