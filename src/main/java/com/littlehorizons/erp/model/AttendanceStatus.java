package com.littlehorizons.erp.model;

import java.util.Arrays;

/** Daily attendance status (dbValue matches the ENUM stored in MySQL). */
public enum AttendanceStatus {
    PRESENT("Presente"),
    ABSENT("Ausente"),
    LATE("Tardanza"),
    MEDICAL_EXCUSE("Excusa Médica");

    private final String dbValue;

    AttendanceStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static AttendanceStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(s -> s.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown attendance status: " + value));
    }
}
