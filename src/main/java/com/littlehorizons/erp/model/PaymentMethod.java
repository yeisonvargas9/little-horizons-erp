package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Payment methods (dbValue matches the ENUM stored in MySQL). */
public enum PaymentMethod {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DEBIT_CARD("Tarjeta Débito"),
    CREDIT_CARD("Tarjeta Crédito"),
    CHECK("Cheque");

    private final String dbValue;

    PaymentMethod(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static PaymentMethod fromDbValue(String value) {
        return Arrays.stream(values()).filter(m -> m.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment method: " + value));
    }
}
