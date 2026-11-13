package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ProfessorStatus;
import com.littlehorizons.erp.model.ScheduleEntry;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Weekday;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Console screens for the professors module (directory, subject catalog, assignments and schedule). */
public class ProfessorView {

    private static final String CANCEL_HINT = "(escriba 'cancelar' en cualquier momento para volver)";

    private final ProfessorController controller;
    private final ConsoleInput input;

    public ProfessorView(ProfessorController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 8)) {
                    case 1 -> registerProfessor();
                    case 2 -> printProfessors();
                    case 3 -> updateContact();
                    case 4 -> changeStatus();
                    case 5 -> showSubjects();
                    case 6 -> assignSubject();
                    case 7 -> showAssignments();
                    case 8 -> showSchedule();
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
        System.out.println("\n=== GESTIÓN DE PROFESORES ===");
        System.out.println("1. Registrar profesor");
        System.out.println("2. Listar profesores");
        System.out.println("3. Actualizar contacto y especialización");
        System.out.println("4. Cambiar estado del profesor");
        System.out.println("5. Ver catálogo de materias");
        System.out.println("6. Asignar materia a un profesor (con horario)");
        System.out.println("7. Ver materias asignadas a un profesor");
        System.out.println("8. Ver horario semanal de un profesor");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    private void registerProfessor() {
        System.out.println("\n--- Registrar profesor --- " + CANCEL_HINT);
        Professor professor = new Professor();
        DocumentType[] options = {DocumentType.CITIZENSHIP_CARD, DocumentType.FOREIGNER_CARD, DocumentType.PASSPORT};
        professor.setDocumentType(input.readChoice("Tipo de documento", options, DocumentType::getLabel));
        professor.setDocumentNumber(input.readValidated("Número de documento", Validator::isValidDocument, "Use entre 5 y 20 letras o números."));
        professor.setFirstName(input.readValidated("Primer nombre", Validator::isValidName, "Solo letras."));
        professor.setMiddleName(input.readOptionalValidated("Segundo nombre (opcional)", Validator::isValidName, "Solo letras."));
        professor.setFirstSurname(input.readValidated("Primer apellido", Validator::isValidName, "Solo letras."));
        professor.setSecondSurname(input.readOptionalValidated("Segundo apellido (opcional)", Validator::isValidName, "Solo letras."));
        professor.setEmail(input.readValidated("Correo electrónico", Validator::isValidEmail, "Correo no válido."));
        professor.setPhone(input.readOptionalValidated("Teléfono (opcional)", Validator::isValidPhone, "Debe tener entre 7 y 15 dígitos."));
        professor.setSpecialization(input.readRequired("Especialización (Matemáticas, Inglés...)"));
        professor.setHireDate(input.readDate("Fecha de contratación (AAAA-MM-DD)"));
        Professor saved = controller.registerProfessor(professor);
        System.out.println("  ✔ Profesor registrado con ID " + saved.getId() + ": " + saved.getFullName());
    }

    private void printProfessors() {
        List<Professor> professors = controller.listProfessors();
        System.out.printf("%n%-5s %-14s %-34s %-20s %-9s%n", "ID", "Documento", "Nombre completo", "Especialización", "Estado");
        System.out.println("-".repeat(86));
        professors.forEach(p -> System.out.printf("%-5d %-14s %-34s %-20s %-9s%n", p.getId(), p.getDocumentNumber(),
                p.getFullName(), p.getSpecialization(), p.getStatus().getLabel()));
        System.out.println("Total: " + professors.size());
    }

    private void updateContact() {
        Professor professor = askExistingProfessor();
        System.out.println("  Profesor: " + professor.getFullName() + ". Presione Enter para dejar un dato igual.");
        String email = input.readOptionalValidated("Correo [" + professor.getEmail() + "]", Validator::isValidEmail, "Correo no válido.");
        String phone = input.readOptionalValidated("Teléfono [" + dash(professor.getPhone()) + "]", Validator::isValidPhone, "Debe tener entre 7 y 15 dígitos.");
        String specialization = input.readOptional("Especialización [" + professor.getSpecialization() + "]");
        controller.updateContact(professor, email, phone, specialization);
        System.out.println("  ✔ Datos actualizados.");
    }

    private void changeStatus() {
        Professor professor = askExistingProfessor();
        System.out.println("  Estado actual de " + professor.getFullName() + ": " + professor.getStatus().getLabel());
        ProfessorStatus status = input.readChoice("Nuevo estado", ProfessorStatus.values(), ProfessorStatus::getLabel);
        controller.changeStatus(professor, status);
        System.out.println("  ✔ Estado cambiado a " + status.getLabel() + ".");
    }

    private void showSubjects() {
        printSubjects(askGradeFilter());
    }

    private void assignSubject() {
        System.out.println("\n--- Asignar materia --- " + CANCEL_HINT);
        Professor professor = askExistingProfessor();
        List<Subject> subjects = printSubjects(askGradeFilter());
        if (subjects.isEmpty()) {
            return;
        }
        int subjectId = input.readInt("ID de la materia a asignar", 1, Integer.MAX_VALUE);
        Subject subject = controller.findSubject(subjectId)
                .orElseThrow(() -> new BusinessException("No existe una materia con ID " + subjectId + "."));
        String section = input.readValidated("Sección (A, B, C...)", s -> s.matches("[A-Za-z]{1,3}"), "Use 1 a 3 letras.").toUpperCase();

        List<ScheduleSlot> slots = new ArrayList<>();
        System.out.println("  La materia tiene " + subject.getWeeklyHours() + " horas semanales. Ingrese los horarios.");
        do {
            Weekday day = input.readChoice("Día de la semana", Weekday.values(), Weekday::getLabel);
            LocalTime start = input.readTime("Hora de inicio (HH:mm)");
            LocalTime end = input.readTime("Hora de fin (HH:mm)");
            slots.add(new ScheduleSlot(day, start, end));
        } while (input.readYesNo("¿Agregar otro horario?"));

        controller.assignSubject(professor, subject, LocalDate.now().getYear(), section, slots);
        System.out.println("  ✔ " + subject.getName() + " asignada a " + professor.getFullName() + ".");
    }

    private void showAssignments() {
        Professor professor = askExistingProfessor();
        var assignments = controller.getAssignments(professor.getId(), LocalDate.now().getYear());
        System.out.println("\nMaterias de " + professor.getFullName() + " (" + LocalDate.now().getYear() + "):");
        if (assignments.isEmpty()) {
            System.out.println("  Sin materias asignadas.");
        }
        assignments.forEach(a -> System.out.printf("  - %-8s %-34s Sección %s%n", a.getSubjectCode(), a.getSubjectName(), a.getSection()));
        System.out.println("Total de materias: " + assignments.size());
    }

    private void showSchedule() {
        Professor professor = askExistingProfessor();
        List<ScheduleEntry> schedule = controller.getWeeklySchedule(professor.getId(), LocalDate.now().getYear());
        System.out.println("\nHorario semanal de " + professor.getFullName() + ":");
        if (schedule.isEmpty()) {
            System.out.println("  Sin clases asignadas.");
        }
        schedule.forEach(e -> System.out.printf("  %-10s %s - %s  %-34s Sección %s%n", e.day().getLabel(), e.start(), e.end(),
                e.subjectName(), e.section()));
    }

    private Professor askExistingProfessor() {
        String document = input.readValidated("Documento del profesor " + CANCEL_HINT, Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        return controller.findByDocument(document)
                .orElseThrow(() -> new BusinessException("No existe un profesor con el documento " + document + "."));
    }

    /** Grade level 0 (Preescolar) to 11 (Once); the grade id in the database is level + 1. Null means all grades. */
    private Integer askGradeFilter() {
        String value = input.readOptionalValidated("Grado (0=Preescolar ... 11=Once, Enter = todos)",
                s -> s.matches("\\d{1,2}") && Integer.parseInt(s) <= 11, "Ingrese un número de 0 a 11.");
        return value == null ? null : Integer.parseInt(value) + 1;
    }

    private List<Subject> printSubjects(Integer gradeId) {
        List<Subject> subjects = controller.listSubjects(gradeId);
        if (subjects.isEmpty()) {
            System.out.println("  No hay materias para ese grado.");
            return subjects;
        }
        System.out.printf("%n%-5s %-8s %-34s %-12s %s%n", "ID", "Código", "Materia", "Grado", "Horas/sem");
        System.out.println("-".repeat(72));
        subjects.forEach(s -> System.out.printf("%-5d %-8s %-34s %-12s %d%n", s.getId(), s.getCode(), s.getName(),
                s.getGradeName(), s.getWeeklyHours()));
        return subjects;
    }

    private String dash(String value) {
        return value == null ? "-" : value;
    }
}
