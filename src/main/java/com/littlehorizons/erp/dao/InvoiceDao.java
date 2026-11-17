package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceDao {

    /** Next consecutive invoice sequence number (1, 2, 3...). */
    int nextSequence();

    Optional<Invoice> findByNumber(String number);

    /** Invoices of a guardian, newest first (lines not loaded). */
    List<Invoice> findByGuardian(int guardianId);

    void updatePdfLocation(int invoiceId, String fileName, String path);
}
