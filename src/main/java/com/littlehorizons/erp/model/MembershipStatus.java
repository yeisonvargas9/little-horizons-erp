package com.littlehorizons.erp.model;

import java.util.Arrays;

/** MembershipStatus (dbValue matches the ENUM stored in MySQL). */
public enum MembershipStatus {
    ACTIVE("Activa"),
    WITHDRAWN("Retirada");

    private final String dbValue;

    MembershipStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return dbValue;
    }

    public static MembershipStatus fromDbValue(String value) {
        return Arrays.stream(values()).filter(v -> v.dbValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MembershipStatus: " + value));
    }
}
