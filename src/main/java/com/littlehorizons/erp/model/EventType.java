package com.littlehorizons.erp.model;

import java.util.Arrays;

/** EventType (dbValue matches the ENUM stored in MySQL). */
public enum EventType {
    ACADEMIC("Académico"),
    CULTURAL("Cultural"),
    SPORTS("Deportivo"),
    SOCIAL("Social"),
    SCIENTIFIC("Científico");

    private final String dbValue;

    EventType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static EventType fromDbValue(String value) {
        return Arrays.stream(values()).filter(v -> v.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown EventType: " + value));
    }
}
