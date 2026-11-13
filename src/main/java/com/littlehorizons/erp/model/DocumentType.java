package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Identity document types (dbValue matches the ENUM stored in MySQL). */
public enum DocumentType {
    CIVIL_REGISTRY("RC", "Registro Civil"),
    IDENTITY_CARD("TI", "Tarjeta de Identidad"),
    CITIZENSHIP_CARD("CC", "Cédula de Ciudadanía"),
    FOREIGNER_CARD("CE", "Cédula de Extranjería"),
    PASSPORT("Pasaporte", "Pasaporte");

    private final String dbValue;
    private final String label;

    DocumentType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static DocumentType fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document type: " + value));
    }
}
