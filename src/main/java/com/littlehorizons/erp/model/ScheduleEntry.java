package com.littlehorizons.erp.model;

import java.time.LocalTime;

/** A row of a professor's weekly timetable (what is taught, when). */
public record ScheduleEntry(Weekday day, LocalTime start, LocalTime end, String subjectName, String section) {
}
