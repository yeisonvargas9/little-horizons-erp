package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Status of a yearly grade enrollment (dbValue matches the ENUM stored in MySQL). */
public enum EnrollmentStatus {
    ACTIVE("Activa"),
    COMPLETED("Completada"),
    WITHDRAWN("Retirada");

    private final String dbValue;

    EnrollmentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static EnrollmentStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(s -> s.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enrollment status: " + value));
    }
}
