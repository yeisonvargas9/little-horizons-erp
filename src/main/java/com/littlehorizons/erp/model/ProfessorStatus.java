package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Professor employment status (dbValue matches the ENUM stored in MySQL). */
public enum ProfessorStatus {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    ON_LEAVE("Licencia");

    private final String dbValue;

    ProfessorStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static ProfessorStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(s -> s.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown professor status: " + value));
    }
}
