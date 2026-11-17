package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.util.Map;

/** A subject's notes by period plus its average for the year (one row of a student's report card). */
public record SubjectGrades(String subjectName, Map<Period, BigDecimal> notes, double average) {
}
