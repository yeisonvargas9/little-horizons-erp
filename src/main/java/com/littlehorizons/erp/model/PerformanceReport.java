package com.littlehorizons.erp.model;

import java.util.List;

/** Result of the performance analysis: every subject ranked, and the best one(s) (several when tied). */
public record PerformanceReport(List<SubjectAverage> ranking, List<SubjectAverage> bestSubjects) {

    public static PerformanceReport empty() {
        return new PerformanceReport(List.of(), List.of());
    }

    public boolean isEmpty() {
        return ranking.isEmpty();
    }
}
