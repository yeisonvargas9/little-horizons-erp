package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A payment made by a guardian for one student's charge (table "pagos"). */
public record Payment(int id, int studentId, String studentName, int guardianId, int conceptId, String conceptName,
                      Integer chargeId, BigDecimal amount, LocalDate paymentDate, PaymentMethod method,
                      String receiptNumber, int academicYear, Period period, String notes) {

    public Payment withIdAndReceipt(int newId, String newReceipt) {
        return new Payment(newId, studentId, studentName, guardianId, conceptId, conceptName, chargeId, amount,
                paymentDate, method, newReceipt, academicYear, period, notes);
    }
}
