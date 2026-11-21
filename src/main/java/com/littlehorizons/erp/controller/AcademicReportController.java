package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicPerformanceDao;
import com.littlehorizons.erp.model.PerformanceReport;
import com.littlehorizons.erp.model.SubjectAverage;
import com.littlehorizons.erp.model.SubjectPerformanceRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Computes the academic performance statistics per subject using the Stream API. */
public class AcademicReportController {

    private final AcademicPerformanceDao performanceDao;

    public AcademicReportController(AcademicPerformanceDao performanceDao) {
        this.performanceDao = performanceDao;
    }

    /**
     * Groups all the grades by subject, averages them (one decimal, ignoring student and period), sorts the
     * subjects from highest to lowest average and finds the best one(s): every subject tied at the top is returned.
     */
    public PerformanceReport generateReport() {
        List<SubjectPerformanceRecord> records = performanceDao.findAllRecordsWithSubject();
        if (records.isEmpty()) {
            return PerformanceReport.empty();
        }

        Map<Integer, List<SubjectPerformanceRecord>> bySubject = records.stream()
                .collect(Collectors.groupingBy(SubjectPerformanceRecord::subjectId));

        List<SubjectAverage> ranking = bySubject.values().stream()
                .map(this::toAverage)
                .sorted(Comparator.comparing(SubjectAverage::average).reversed()
                        .thenComparing(SubjectAverage::subjectCode))
                .collect(Collectors.toList());

        BigDecimal best = ranking.get(0).average();
        List<SubjectAverage> bestSubjects = ranking.stream()
                .filter(subject -> subject.average().compareTo(best) == 0)
                .collect(Collectors.toList());
        return new PerformanceReport(ranking, bestSubjects);
    }

    private SubjectAverage toAverage(List<SubjectPerformanceRecord> grades) {
        SubjectPerformanceRecord first = grades.get(0);
        BigDecimal sum = grades.stream().map(SubjectPerformanceRecord::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(grades.size()), 1, RoundingMode.HALF_UP);
        return new SubjectAverage(first.subjectId(), first.subjectCode(), first.subjectName(), average, grades.size());
    }
}
