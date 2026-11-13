package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Student lifecycle status (dbValue matches the ENUM stored in MySQL). */
public enum StudentStatus {
    ACTIVE("Activo", "Activo"),
    GRADUATED("Graduado", "Graduado"),
    WITHDRAWN("Retirado", "Retirado"),
    SUSPENDED("Suspendido", "Suspendido");

    private final String dbValue;
    private final String label;

    StudentStatus(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static StudentStatus fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown student status: " + value));
    }
}
