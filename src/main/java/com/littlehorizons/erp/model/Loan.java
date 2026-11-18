package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A loan of a resource to a student or a professor (tables "prestamos_estudiante" / "prestamos_profesor"). */
public record Loan(int id, int resourceId, String resourceCode, String resourceName, BorrowerType borrowerType,
                   int borrowerId, String borrowerName, LocalDate loanDate, LocalDate dueDate, LocalDate returnDate,
                   LoanStatus status, String notes) {
}
