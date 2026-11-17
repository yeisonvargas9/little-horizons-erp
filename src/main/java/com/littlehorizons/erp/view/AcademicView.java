package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.AbsenceSummary;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.CourseAverage;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentSummary;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectGrades;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Console screens for the academic module (enrollment, grades, history and attendance). */
public class AcademicView {

    private static final String CANCEL_HINT = "(escriba 'cancelar' en cualquier momento para volver)";

    private final AcademicController controller;
    private final ConsoleInput input;

    public AcademicView(AcademicController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 9)) {
                    case 1 -> enrollInGrade();
                    case 2 -> showEnrollments();
                    case 3 -> enrollInSubjects();
                    case 4 -> registerGrade();
                    case 5 -> showReportCard();
                    case 6 -> showHistory();
                    case 7 -> showCourseAverages();
                    case 8 -> registerAttendance();
                    case 9 -> showAbsenceReport();
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
        System.out.println("\n=== GESTIÓN ACADÉMICA ===");
        System.out.println("1. Matricular estudiante en un grado");
        System.out.println("2. Ver matrículas de un estudiante");
        System.out.println("3. Inscribir estudiante en materias (por período)");
        System.out.println("4. Registrar o corregir una nota");
        System.out.println("5. Ver boletín de notas y promedios del año");
        System.out.println("6. Ver historial académico completo");
        System.out.println("7. Ver promedio del curso por materia");
        System.out.println("8. Registrar asistencia de una clase");
        System.out.println("9. Reporte de inasistencias de la semana");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    // ---------------------------------------------------------------- enrollment

    private void enrollInGrade() {
        Student student = askStudent();
        int gradeLevel = input.readInt("Grado (0=Preescolar ... 11=Once) " + CANCEL_HINT, 0, 11);
        Enrollment enrollment = controller.enrollInGrade(student, gradeLevel + 1, controller.currentAcademicYear());
        System.out.println("  ✔ " + student.getFullName() + " matriculado en el año " + enrollment.academicYear() + ".");
    }

    private void showEnrollments() {
        Student student = askStudent();
        List<Enrollment> enrollments = controller.getEnrollments(student.getId());
        System.out.println("\nMatrículas de " + student.getFullName() + ":");
        if (enrollments.isEmpty()) {
            System.out.println("  Sin matrículas registradas.");
        }
        enrollments.forEach(e -> System.out.printf("  %d  %-12s %-11s (%s)%n", e.academicYear(), e.gradeName(),
                e.status().getDbValue(), e.enrollmentDate()));
    }

    private void enrollInSubjects() {
        Student student = askStudent();
        Period period = input.readChoice("Período", Period.regular().toArray(new Period[0]), Period::getLabel);
        int year = controller.currentAcademicYear();
        if (input.readYesNo("¿Inscribir en TODAS las materias de su grado?")) {
            int count = controller.enrollInAllSubjects(student, period, year);
            System.out.println("  ✔ Inscrito en " + count + " materias en el " + period.getLabel() + ".");
            return;
        }
        Subject subject = askSubject();
        controller.enrollInSubject(student, subject, period, year);
        System.out.println("  ✔ Inscrito en " + subject.getName() + ".");
    }

    // ---------------------------------------------------------------- grades

    private void registerGrade() {
        Student student = askStudent();
        Subject subject = askSubject();
        Period period = input.readChoice("Período de la nota", Period.gradePeriods().toArray(new Period[0]), Period::getLabel);
        BigDecimal value = new BigDecimal(input.readValidated("Nota (0.0 a 5.0, un decimal)",
                s -> s.matches("[0-4](\\.\\d)?|5(\\.0)?"), "Use una nota entre 0.0 y 5.0 con un solo decimal (ej: 4.2)."));
        String observations = input.readOptional("Observaciones (opcional)");
        controller.registerGrade(student, subject, period, value, observations, controller.currentAcademicYear());
        System.out.println("  ✔ Nota " + value + " registrada en " + subject.getName() + " (" + period.getLabel() + ").");
    }

    private void showReportCard() {
        Student student = askStudent();
        int year = controller.currentAcademicYear();
        List<AcademicRecord> history = controller.getHistory(student.getId());
        List<SubjectGrades> card = controller.buildReportCard(history, year);
        System.out.println("\nBoletín " + year + " - " + student.getFullName());
        if (card.isEmpty()) {
            System.out.println("  Sin notas registradas en " + year + ".");
            return;
        }
        System.out.printf("%-34s %5s %5s %5s %5s %9s%n", "Materia", "I", "II", "III", "IV", "Promedio");
        System.out.println("-".repeat(70));
        card.forEach(row -> System.out.printf("%-34s %5s %5s %5s %5s %9.2f%n", row.subjectName(),
                note(row, Period.FIRST), note(row, Period.SECOND), note(row, Period.THIRD), note(row, Period.FOURTH),
                row.average()));
        System.out.println("-".repeat(70));
        System.out.println("Promedio general por período:");
        controller.generalAverageByPeriod(history, year)
                .forEach((period, average) -> System.out.printf("  %-12s %.2f%n", period.getLabel(), average));
        System.out.printf("Promedio general del año: %.2f%n",
                card.stream().mapToDouble(SubjectGrades::average).average().orElse(0));
    }

    private void showHistory() {
        Student student = askStudent();
        List<AcademicRecord> history = controller.getHistory(student.getId());
        System.out.println("\nHistorial académico de " + student.getFullName());
        if (history.isEmpty()) {
            System.out.println("  Sin notas registradas.");
            return;
        }
        Map<Integer, List<AcademicRecord>> byYear = history.stream()
                .collect(Collectors.groupingBy(AcademicRecord::academicYear, TreeMap::new, Collectors.toList()));
        byYear.forEach((year, records) -> {
            System.out.println("\n  --- " + year + " ---");
            records.forEach(r -> System.out.printf("  %-12s %-34s %s%n", r.period().getLabel(), r.subjectName(), r.value()));
        });
        System.out.println("\nTotal de notas: " + history.size());
    }

    private void showCourseAverages() {
        int gradeLevel = input.readInt("Grado (0=Preescolar ... 11=Once) " + CANCEL_HINT, 0, 11);
        String periodAnswer = input.readOptionalValidated("Período (1=I, 2=II, 3=III, 4=IV, Enter = todos)",
                s -> s.matches("[1-4]"), "Ingrese un número de 1 a 4.");
        Period period = periodAnswer == null ? null : Period.regular().get(Integer.parseInt(periodAnswer) - 1);
        List<CourseAverage> averages = controller.courseAverages(gradeLevel + 1, controller.currentAcademicYear(), period);
        System.out.println("\nPromedio del curso (" + (period == null ? "todos los períodos" : period.getLabel()) + "):");
        averages.forEach(a -> System.out.printf("  %-34s %s%n", a.subjectName(),
                a.average() == null ? "sin notas" : String.format("%.2f", a.average())));
    }

    // ---------------------------------------------------------------- attendance

    private void registerAttendance() {
        Subject subject = askSubject();
        LocalDate date = input.readOptionalDate("Fecha de la clase (AAAA-MM-DD, Enter = hoy)");
        LocalDate day = date == null ? LocalDate.now() : date;
        List<StudentSummary> classList = controller.getClassList(subject, controller.currentAcademicYear());
        if (classList.isEmpty()) {
            throw new BusinessException("No hay estudiantes matriculados en " + subject.getGradeName() + ".");
        }
        System.out.println("\n" + subject.getName() + " - " + day + ". Enter = Presente; 1=Presente 2=Ausente 3=Tardanza 4=Excusa médica");
        Map<Integer, AttendanceStatus> marks = new LinkedHashMap<>();
        for (StudentSummary student : classList) {
            String answer = input.readOptionalValidated("  " + student.fullName(), s -> s.matches("[1-4]"), "Ingrese 1, 2, 3 o 4.");
            marks.put(student.id(), answer == null ? AttendanceStatus.PRESENT : AttendanceStatus.values()[Integer.parseInt(answer) - 1]);
        }
        int saved = controller.registerAttendance(subject, day, marks);
        System.out.println("  ✔ Asistencia registrada para " + saved + " estudiantes.");
    }

    private void showAbsenceReport() {
        LocalDate date = input.readOptionalDate("Fecha dentro de la semana a revisar (AAAA-MM-DD, Enter = hoy)");
        List<AbsenceSummary> report = controller.absenceReport(date == null ? LocalDate.now() : date);
        System.out.println("\nEstudiantes con más de " + AcademicController.ABSENCE_LIMIT_PER_WEEK + " días de inasistencia en la semana:");
        if (report.isEmpty()) {
            System.out.println("  Ningún estudiante supera el límite.");
        }
        report.forEach(a -> System.out.printf("  %-36s %d días ausente%n", a.fullName(), a.absentDays()));
    }

    // ---------------------------------------------------------------- helpers

    private Student askStudent() {
        String document = input.readValidated("Documento del estudiante " + CANCEL_HINT, Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        return controller.findStudent(document)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con el documento " + document + "."));
    }

    private Subject askSubject() {
        int id = input.readInt("ID de la materia (consúltelo en Gestión de Profesores > Catálogo)", 1, Integer.MAX_VALUE);
        return controller.findSubject(id)
                .orElseThrow(() -> new BusinessException("No existe una materia con ID " + id + "."));
    }

    private String note(SubjectGrades row, Period period) {
        BigDecimal value = row.notes().get(period);
        return value == null ? "-" : value.toPlainString();
    }
}
