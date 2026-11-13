package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Student gender (dbValue matches the ENUM stored in MySQL). */
public enum Gender {
    MALE("M", "Masculino"),
    FEMALE("F", "Femenino"),
    OTHER("Otro", "Otro");

    private final String dbValue;
    private final String label;

    Gender(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static Gender fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(gender -> gender.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown gender: " + value));
    }
}
