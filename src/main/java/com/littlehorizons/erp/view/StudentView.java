package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Gender;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Console screens for the students module. Only does input/output; all rules live in the controller. */
public class StudentView {

    private static final String CANCEL_HINT = "(escriba 'cancelar' en cualquier momento para volver)";

    private final StudentController controller;
    private final ConsoleInput input;

    public StudentView(StudentController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 7)) {
                    case 1 -> registerStudent();
                    case 2 -> printTable(controller.listStudents());
                    case 3 -> searchStudents();
                    case 4 -> updateContactInfo();
                    case 5 -> changeStatus();
                    case 6 -> showGuardians();
                    case 7 -> showSummary();
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
        System.out.println("\n=== GESTIÓN DE ESTUDIANTES ===");
        System.out.println("1. Registrar estudiante (con su acudiente)");
        System.out.println("2. Listar estudiantes");
        System.out.println("3. Buscar por nombre o documento");
        System.out.println("4. Actualizar datos de contacto y salud");
        System.out.println("5. Cambiar estado del estudiante");
        System.out.println("6. Ver acudientes de un estudiante");
        System.out.println("7. Resumen por estado");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    // ---------------------------------------------------------------- option 1

    private void registerStudent() {
        System.out.println("\n--- Registrar estudiante --- " + CANCEL_HINT);
        Student student = new Student();
        student.setDocumentType(input.readChoice("Tipo de documento", DocumentType.values(), DocumentType::getLabel));
        student.setDocumentNumber(input.readValidated("Número de documento", Validator::isValidDocument,
                "Use entre 5 y 20 letras o números."));
        student.setFirstName(input.readValidated("Primer nombre", Validator::isValidName, "Solo letras."));
        student.setMiddleName(input.readOptionalValidated("Segundo nombre (opcional)", Validator::isValidName, "Solo letras."));
        student.setFirstSurname(input.readValidated("Primer apellido", Validator::isValidName, "Solo letras."));
        student.setSecondSurname(input.readOptionalValidated("Segundo apellido (opcional)", Validator::isValidName, "Solo letras."));
        student.setBirthDate(input.readDate("Fecha de nacimiento (AAAA-MM-DD)"));
        student.setGender(input.readChoice("Género", Gender.values(), Gender::getLabel));
        student.setAddress(input.readOptional("Dirección (opcional)"));
        student.setPhone(input.readOptionalValidated("Teléfono (opcional)", Validator::isValidPhone, "Debe tener entre 7 y 15 dígitos."));
        student.setEmail(input.readOptionalValidated("Correo electrónico (opcional)", Validator::isValidEmail, "Correo no válido."));
        student.setBloodType(input.readOptionalValidated("Tipo de sangre (opcional, ej: O+)", Validator::isValidBloodType, "Use el formato A+, O-, AB+."));
        student.setAllergies(input.readOptional("Alergias (opcional)"));
        student.setMedicalConditions(input.readOptional("Condiciones médicas (opcional)"));
        LocalDate admission = input.readOptionalDate("Fecha de ingreso (Enter = hoy)");
        student.setAdmissionDate(admission == null ? LocalDate.now() : admission);

        System.out.println("\n--- Datos del acudiente ---");
        Guardian guardian = askGuardian(student);
        Student saved = controller.registerStudent(student, guardian);
        System.out.println("  ✔ Estudiante registrado con ID " + saved.getId() + ": " + saved.getFullName());
    }

    private Guardian askGuardian(Student student) {
        String document = input.readValidated("Número de documento del acudiente", Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        Optional<Guardian> existing = controller.findGuardianByDocument(document);
        if (existing.isPresent()) {
            System.out.println("  ℹ Ya existe el acudiente " + existing.get().getFullName() + ". Se vinculará al estudiante.");
            return existing.get();
        }
        Guardian guardian = new Guardian();
        guardian.setDocumentNumber(document);
        DocumentType[] options = {DocumentType.CITIZENSHIP_CARD, DocumentType.FOREIGNER_CARD, DocumentType.PASSPORT};
        guardian.setDocumentType(input.readChoice("Tipo de documento del acudiente", options, DocumentType::getLabel));
        guardian.setFirstName(input.readValidated("Primer nombre", Validator::isValidName, "Solo letras."));
        guardian.setMiddleName(input.readOptionalValidated("Segundo nombre (opcional)", Validator::isValidName, "Solo letras."));
        guardian.setFirstSurname(input.readValidated("Primer apellido", Validator::isValidName, "Solo letras."));
        guardian.setSecondSurname(input.readOptionalValidated("Segundo apellido (opcional)", Validator::isValidName, "Solo letras."));
        guardian.setRelationship(input.readRequired("Parentesco (Padre, Madre, Tutor legal...)"));
        guardian.setPrimaryPhone(input.readValidated("Teléfono principal", Validator::isValidPhone, "Debe tener entre 7 y 15 dígitos."));
        guardian.setSecondaryPhone(input.readOptionalValidated("Teléfono secundario (opcional)", Validator::isValidPhone, "Debe tener entre 7 y 15 dígitos."));
        guardian.setEmail(input.readOptionalValidated("Correo electrónico (opcional)", Validator::isValidEmail, "Correo no válido."));
        String address = input.readOptional("Dirección (Enter = la del estudiante)");
        guardian.setAddress(address == null ? student.getAddress() : address);
        guardian.setOccupation(input.readOptional("Ocupación (opcional)"));
        guardian.setFinanciallyResponsible(input.readYesNo("¿Es el responsable de los pagos?"));
        return guardian;
    }

    // ---------------------------------------------------------------- options 3 to 7

    private void searchStudents() {
        String term = input.readRequired("Nombre o documento a buscar " + CANCEL_HINT);
        List<Student> results = term.matches("\\d+")
                ? controller.findByDocument(term).map(List::of).orElse(List.of())
                : controller.searchByName(term);
        printTable(results);
    }

    private void updateContactInfo() {
        Student student = askExistingStudent();
        System.out.println("  Estudiante: " + student.getFullName() + ". Presione Enter para dejar un dato igual.");
        String address = input.readOptional("Dirección [" + nullToDash(student.getAddress()) + "]");
        String phone = input.readOptionalValidated("Teléfono [" + nullToDash(student.getPhone()) + "]", Validator::isValidPhone, "Debe tener entre 7 y 15 dígitos.");
        String email = input.readOptionalValidated("Correo [" + nullToDash(student.getEmail()) + "]", Validator::isValidEmail, "Correo no válido.");
        String blood = input.readOptionalValidated("Tipo de sangre [" + nullToDash(student.getBloodType()) + "]", Validator::isValidBloodType, "Use el formato A+, O-, AB+.");
        String allergies = input.readOptional("Alergias [" + nullToDash(student.getAllergies()) + "]");
        String conditions = input.readOptional("Condiciones médicas [" + nullToDash(student.getMedicalConditions()) + "]");
        controller.updateContactInfo(student, address, phone, email, blood, allergies, conditions);
        System.out.println("  ✔ Datos actualizados.");
    }

    private void changeStatus() {
        Student student = askExistingStudent();
        System.out.println("  Estado actual de " + student.getFullName() + ": " + student.getStatus().getLabel());
        StudentStatus newStatus = input.readChoice("Nuevo estado", StudentStatus.values(), StudentStatus::getLabel);
        controller.changeStatus(student, newStatus);
        System.out.println("  ✔ Estado cambiado a " + newStatus.getLabel() + ".");
    }

    private void showGuardians() {
        Student student = askExistingStudent();
        List<Guardian> guardians = controller.getGuardians(student.getId());
        System.out.println("\nAcudientes de " + student.getFullName() + ":");
        guardians.forEach(g -> System.out.printf("  - %s (%s) | %s %s | Tel: %s | Resp. de pagos: %s%n",
                g.getFullName(), g.getRelationship(), g.getDocumentType().getDbValue(), g.getDocumentNumber(),
                g.getPrimaryPhone(), g.isFinanciallyResponsible() ? "Sí" : "No"));
    }

    private void showSummary() {
        Map<StudentStatus, Long> counts = controller.countByStatus();
        System.out.println("\nResumen de estudiantes por estado:");
        for (StudentStatus status : StudentStatus.values()) {
            System.out.printf("  %-12s %d%n", status.getLabel(), counts.getOrDefault(status, 0L));
        }
        System.out.println("  Total        " + counts.values().stream().mapToLong(Long::longValue).sum());
    }

    // ---------------------------------------------------------------- helpers

    private Student askExistingStudent() {
        String document = input.readValidated("Documento del estudiante " + CANCEL_HINT, Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        return controller.findByDocument(document)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con el documento " + document + "."));
    }

    private void printTable(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("\n  No se encontraron estudiantes.");
            return;
        }
        System.out.printf("%n%-5s %-16s %-34s %-5s %-11s%n", "ID", "Documento", "Nombre completo", "Edad", "Estado");
        System.out.println("-".repeat(75));
        students.forEach(s -> System.out.printf("%-5d %-16s %-34s %-5d %-11s%n", s.getId(),
                s.getDocumentType().getDbValue() + " " + s.getDocumentNumber(), s.getFullName(), s.getAge(),
                s.getStatus().getLabel()));
        System.out.println("Total: " + students.size());
    }

    private String nullToDash(String value) {
        return value == null ? "-" : value;
    }
}
