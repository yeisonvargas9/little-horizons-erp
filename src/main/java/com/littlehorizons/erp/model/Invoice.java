package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Electronic invoice header plus its lines (tables "facturas" and "detalle_factura"). */
public record Invoice(int id, int sequence, String number, int guardianId, LocalDate issueDate, BigDecimal subtotal,
                      BigDecimal vat, BigDecimal total, String cufe, String qrContent, String pdfFileName,
                      String pdfPath, List<InvoiceLine> lines) {

    public Invoice withId(int newId, List<InvoiceLine> newLines) {
        return new Invoice(newId, sequence, number, guardianId, issueDate, subtotal, vat, total, cufe, qrContent,
                pdfFileName, pdfPath, newLines);
    }

    public Invoice withPdf(String fileName, String path) {
        return new Invoice(id, sequence, number, guardianId, issueDate, subtotal, vat, total, cufe, qrContent,
                fileName, path, lines);
    }
}
