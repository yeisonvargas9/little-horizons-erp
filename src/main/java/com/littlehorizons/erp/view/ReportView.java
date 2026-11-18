package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.ConceptRevenue;
import com.littlehorizons.erp.model.DashboardData;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.GradeCount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.util.ConsoleInput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Console screens for reports and statistics. */
public class ReportView {

    private static final Locale CO = Locale.forLanguageTag("es-CO");

    private final ReportController controller;
    private final ConsoleInput input;

    public ReportView(ReportController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 7)) {
                    case 1 -> showDashboard();
                    case 2 -> showStudentsPerGrade();
                    case 3 -> showStudentsAtRisk();
                    case 4 -> showTopStudents();
                    case 5 -> showRevenue();
                    case 6 -> showExtracurricular();
                    case 7 -> showResources();
                    case 0 -> back = true;
                    default -> { }
                }
            } catch (OperationCancelledException e) {
                System.out.println("  Operación cancelada.");
            } catch (BusinessException e) {
                System.out.println("  ⚠ " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("  ✖ " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== REPORTES Y ESTADÍSTICAS ===");
        System.out.println("1. Resumen general del colegio");
        System.out.println("2. Estudiantes por grado");
        System.out.println("3. Estudiantes que necesitan apoyo académico (promedio bajo 3.0)");
        System.out.println("4. Mejores promedios");
        System.out.println("5. Recaudo por concepto de pago");
        System.out.println("6. Participación extracurricular (clubes y eventos)");
        System.out.println("7. Estado de los recursos");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    private void showDashboard() {
        DashboardData d = controller.dashboard();
        System.out.println("\n--- Resumen general (" + LocalDate.now() + ") ---");
        System.out.printf("  Estudiantes activos:            %d%n", d.activeStudents());
        System.out.printf("  Profesores activos:             %d%n", d.activeProfessors());
        System.out.printf("  Clubes activos:                 %d%n", d.activeClubs());
        System.out.printf("  Estudiantes en algún club:      %d (%.1f%%)%n", d.studentsInClubs(),
                d.activeStudents() == 0 ? 0 : 100.0 * d.studentsInClubs() / d.activeStudents());
        System.out.printf("  Recaudo del mes:                %s%n", money(d.monthRevenue()));
        System.out.printf("  Estudiantes morosos (>1 mes):   %d%n", d.delinquentStudents());
        System.out.printf("  Préstamos vencidos:             %d%n", d.overdueLoans());
    }

    private void showStudentsPerGrade() {
        List<GradeCount> counts = controller.studentsPerGrade();
        System.out.println("\nEstudiantes matriculados en " + controller.currentAcademicYear() + ":");
        counts.forEach(c -> System.out.printf("  %-12s %3d  %s%n", c.gradeName(), c.students(), "█".repeat((int) c.students())));
        System.out.println("  Total: " + counts.stream().mapToLong(GradeCount::students).sum());
    }

    private void showStudentsAtRisk() {
        Period period = askPeriod();
        List<StudentAverage> atRisk = controller.studentsAtRisk(period);
        System.out.println("\nEstudiantes con promedio inferior a " + ReportController.PASSING_AVERAGE + " ("
                + (period == null ? "todo el año" : period.getLabel()) + "):");
        if (atRisk.isEmpty()) {
            System.out.println("  Ningún estudiante está por debajo del mínimo.");
            return;
        }
        atRisk.forEach(s -> System.out.printf("  %-36s %-12s %.2f%n", s.studentName(), s.gradeName(), s.average()));
        System.out.println("Total que necesitan apoyo: " + atRisk.size());
    }

    private void showTopStudents() {
        Period period = askPeriod();
        System.out.println("\nMejores promedios (" + (period == null ? "todo el año" : period.getLabel()) + "):");
        List<StudentAverage> top = controller.topStudents(period, 5);
        for (int i = 0; i < top.size(); i++) {
            StudentAverage s = top.get(i);
            System.out.printf("  %d. %-36s %-12s %.2f%n", i + 1, s.studentName(), s.gradeName(), s.average());
        }
        System.out.printf("Promedio general del colegio: %.2f%n", controller.schoolAverage(period));
    }

    private void showRevenue() {
        LocalDate from = input.readOptionalDate("Desde (AAAA-MM-DD, Enter = inicio del año)");
        LocalDate to = input.readOptionalDate("Hasta (AAAA-MM-DD, Enter = hoy)");
        LocalDate start = from == null ? LocalDate.of(LocalDate.now().getYear(), 1, 1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (end.isBefore(start)) {
            throw new BusinessException("La fecha final no puede ser anterior a la inicial.");
        }
        List<ConceptRevenue> revenue = controller.revenueByConcept(start, end);
        System.out.println("\nRecaudo del " + start + " al " + end + ":");
        revenue.forEach(r -> System.out.printf("  %-26s %5d pagos %18s%n", r.conceptName(), r.payments(), money(r.total())));
        System.out.println("  TOTAL RECAUDADO: " + money(controller.totalRevenue(revenue)));
    }

    private void showExtracurricular() {
        System.out.println("\nMiembros activos por club:");
        List<NamedCount> clubs = controller.clubMembers();
        clubs.stream().limit(10).forEach(c -> System.out.printf("  %-32s %3d%n", c.name(), c.count()));
        System.out.println("  (se muestran los 10 clubes con más miembros de " + clubs.size() + ")");
        System.out.println("\nParticipantes por evento:");
        List<EventParticipation> events = controller.eventParticipation();
        events.forEach(e -> System.out.printf("  %-34s %s %3d%n", e.eventName(), e.date(), e.participants()));
    }

    private void showResources() {
        System.out.println("\nRecursos por estado:");
        List<NamedCount> counts = controller.resourcesByStatus();
        counts.forEach(c -> System.out.printf("  %-18s %3d%n", c.name(), c.count()));
        System.out.println("  Total: " + counts.stream().mapToLong(NamedCount::count).sum());
    }

    private Period askPeriod() {
        String answer = input.readOptionalValidated("Período (1=I, 2=II, 3=III, 4=IV, Enter = todo el año)",
                s -> s.matches("[1-4]"), "Ingrese un número de 1 a 4.");
        return answer == null ? null : Period.regular().get(Integer.parseInt(answer) - 1);
    }

    private String money(BigDecimal value) {
        return String.format(CO, "$ %,.0f", value);
    }
}
