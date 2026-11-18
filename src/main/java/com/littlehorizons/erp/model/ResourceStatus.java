package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Resource status (dbValue matches the ENUM stored in MySQL). */
public enum ResourceStatus {
    AVAILABLE("Disponible"),
    LOANED("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    DECOMMISSIONED("Dado de Baja");

    private final String dbValue;

    ResourceStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static ResourceStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(v -> v.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resource status: " + value));
    }
}
