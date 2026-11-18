package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Number of students that took part in an event. */
public record EventParticipation(String eventName, LocalDate date, long participants) {
}
