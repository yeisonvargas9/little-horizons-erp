package com.littlehorizons.erp.model;

/** A student's general average (all subjects) for a year or period. */
public record StudentAverage(int studentId, String studentName, String gradeName, double average) {
}
