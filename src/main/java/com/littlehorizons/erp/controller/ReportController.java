package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.ConceptRevenue;
import com.littlehorizons.erp.model.DashboardData;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.GradeCount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.StudentAverage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Reports and statistics. The DAO runs the aggregate queries; this class filters and summarizes with Streams. */
public class ReportController {

    /** Colombian passing grade: students below this average need extra support. */
    public static final double PASSING_AVERAGE = 3.0;

    private final ReportDao reportDao;

    public ReportController(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public int currentAcademicYear() {
        return Year.now().getValue();
    }

    public DashboardData dashboard() {
        LocalDate today = LocalDate.now();
        return reportDao.dashboard(today.withDayOfMonth(1), today);
    }

    public List<GradeCount> studentsPerGrade() {
        return reportDao.studentsPerGrade(currentAcademicYear());
    }

    /** Students whose general average is below the passing grade, the weakest first. */
    public List<StudentAverage> studentsAtRisk(Period period) {
        return reportDao.studentAverages(currentAcademicYear(), period).stream()
                .filter(s -> s.average() < PASSING_AVERAGE)
                .sorted(Comparator.comparingDouble(StudentAverage::average))
                .collect(Collectors.toList());
    }

    public List<StudentAverage> topStudents(Period period, int limit) {
        return reportDao.studentAverages(currentAcademicYear(), period).stream()
                .sorted(Comparator.comparingDouble(StudentAverage::average).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public double schoolAverage(Period period) {
        return reportDao.studentAverages(currentAcademicYear(), period).stream()
                .mapToDouble(StudentAverage::average).average().orElse(0);
    }

    public List<ConceptRevenue> revenueByConcept(LocalDate from, LocalDate to) {
        return reportDao.revenueByConcept(from, to);
    }

    public BigDecimal totalRevenue(List<ConceptRevenue> revenue) {
        return revenue.stream().map(ConceptRevenue::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<NamedCount> clubMembers() {
        return reportDao.clubMembers();
    }

    public List<EventParticipation> eventParticipation() {
        return reportDao.eventParticipation();
    }

    public List<NamedCount> resourcesByStatus() {
        return reportDao.resourcesByStatus();
    }
}
