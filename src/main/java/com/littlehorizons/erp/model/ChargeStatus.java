package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Status of a charge (dbValue matches the ENUM stored in MySQL). */
public enum ChargeStatus {
    PENDING("Pendiente"),
    PAID("Pagado"),
    OVERDUE("Vencido");

    private final String dbValue;

    ChargeStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static ChargeStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(s -> s.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown charge status: " + value));
    }
}
