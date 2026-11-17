package com.littlehorizons.erp.model;

/** Average note of a whole course in one subject. */
public record CourseAverage(String subjectName, Double average, int gradedStudents) {
}
