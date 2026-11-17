package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BillingResult;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;

import java.util.List;

public interface PaymentDao {

    /**
     * In ONE transaction: saves the payments (assigning their receipt numbers), marks their charges as paid and
     * saves the invoice with its lines. Lines and payments correspond one to one, in the same order.
     */
    BillingResult registerPayments(List<Payment> payments, Invoice invoice);

    List<Payment> findByStudent(int studentId);

    List<Payment> findByGuardian(int guardianId);
}
