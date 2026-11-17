package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** One line of an invoice (table "detalle_factura"). */
public record InvoiceLine(String description, int quantity, BigDecimal unitPrice, BigDecimal subtotal,
                          Integer paymentId) {

    public InvoiceLine withPaymentId(int id) {
        return new InvoiceLine(description, quantity, unitPrice, subtotal, id);
    }
}
