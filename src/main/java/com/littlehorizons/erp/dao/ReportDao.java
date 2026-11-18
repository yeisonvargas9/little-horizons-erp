package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ConceptRevenue;
import com.littlehorizons.erp.model.DashboardData;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.GradeCount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.StudentAverage;

import java.time.LocalDate;
import java.util.List;

/** Read-only queries that feed the reports and statistics screens. */
public interface ReportDao {

    DashboardData dashboard(LocalDate monthStart, LocalDate today);

    List<GradeCount> studentsPerGrade(int academicYear);

    /** General average of every student with grades; a null period means all periods of the year. */
    List<StudentAverage> studentAverages(int academicYear, Period period);

    List<ConceptRevenue> revenueByConcept(LocalDate from, LocalDate to);

    /** Active members per club, most popular first. */
    List<NamedCount> clubMembers();

    List<EventParticipation> eventParticipation();

    List<NamedCount> resourcesByStatus();
}
