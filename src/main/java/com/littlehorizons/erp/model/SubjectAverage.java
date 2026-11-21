package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** General average of one subject (all its grades, any period or student), with one decimal. */
public record SubjectAverage(int subjectId, String subjectCode, String subjectName, BigDecimal average,
                             int gradesCount) {
}
