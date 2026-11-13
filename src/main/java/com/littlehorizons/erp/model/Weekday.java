package com.littlehorizons.erp.model;

import java.util.Arrays;

/** School days (dbValue matches the ENUM stored in MySQL). The declaration order is the week order. */
public enum Weekday {
    MONDAY("Lunes"),
    TUESDAY("Martes"),
    WEDNESDAY("Miércoles"),
    THURSDAY("Jueves"),
    FRIDAY("Viernes");

    private final String dbValue;

    Weekday(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static Weekday fromDbValue(String value) {
        return Arrays.stream(values()).filter(d -> d.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown weekday: " + value));
    }
}
