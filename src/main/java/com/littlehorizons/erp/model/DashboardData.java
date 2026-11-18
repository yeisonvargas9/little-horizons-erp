package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Key figures of the school shown in the general summary. */
public record DashboardData(long activeStudents, long activeProfessors, long activeClubs, long studentsInClubs,
                            BigDecimal monthRevenue, long delinquentStudents, long overdueLoans) {
}
