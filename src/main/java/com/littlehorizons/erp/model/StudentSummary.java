package com.littlehorizons.erp.model;

/** Lightweight student view (id, name, document) used in class lists. */
public record StudentSummary(int id, String fullName, String documentNumber) {
}
