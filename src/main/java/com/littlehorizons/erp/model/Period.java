package com.littlehorizons.erp.model;

import java.util.Arrays;
import java.util.List;

/** Academic periods (dbValue matches the ENUM stored in MySQL). Declaration order is the school-year order. */
public enum Period {
    FIRST("I", "Período I"),
    SECOND("II", "Período II"),
    THIRD("III", "Período III"),
    FOURTH("IV", "Período IV"),
    FINAL("Final", "Final"),
    ANNUAL("Anual", "Anual");

    private final String dbValue;
    private final String label;

    Period(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    /** Periods that can receive a grade: the four regular ones plus the final grade (ANNUAL is for payments only). */
    public static List<Period> gradePeriods() {
        return List.of(FIRST, SECOND, THIRD, FOURTH, FINAL);
    }

    /** The four regular periods (subject enrollment does not accept FINAL). */
    public static List<Period> regular() {
        return List.of(FIRST, SECOND, THIRD, FOURTH);
    }

    public static Period fromDbValue(String value) {
        return Arrays.stream(values()).filter(p -> p.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown period: " + value));
    }
}
