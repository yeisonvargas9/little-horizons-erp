package com.littlehorizons.erp.model;

import java.util.Arrays;

/** ParticipationRole (dbValue matches the ENUM stored in MySQL). */
public enum ParticipationRole {
    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String dbValue;

    ParticipationRole(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static ParticipationRole fromDbValue(String value) {
        return Arrays.stream(values()).filter(v -> v.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ParticipationRole: " + value));
    }
}
