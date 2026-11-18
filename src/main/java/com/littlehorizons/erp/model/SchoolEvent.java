package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A school event such as a science fair or sports day (table "eventos"). */
public record SchoolEvent(int id, String name, String description, EventType type, LocalDate date, String place,
                          boolean hasCompetition, Integer organizerId, String organizerName, boolean active) {
}
