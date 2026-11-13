package com.littlehorizons.erp.model;

import java.time.Duration;
import java.time.LocalTime;

/** One weekly class block: a day with a start and end time. Immutable value object. */
public record ScheduleSlot(Weekday day, LocalTime start, LocalTime end) {

    public boolean overlaps(ScheduleSlot other) {
        return day == other.day && start.isBefore(other.end) && end.isAfter(other.start);
    }

    public double hours() {
        return Duration.between(start, end).toMinutes() / 60.0;
    }
}
