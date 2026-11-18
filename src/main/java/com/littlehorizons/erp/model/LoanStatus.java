package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Loan status (dbValue matches the ENUM stored in MySQL). */
public enum LoanStatus {
    ACTIVE("Activo"),
    RETURNED("Devuelto"),
    OVERDUE("Vencido");

    private final String dbValue;

    LoanStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static LoanStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(v -> v.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown loan status: " + value));
    }
}
