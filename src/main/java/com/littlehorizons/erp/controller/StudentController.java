package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business logic for students and their guardians. It validates the rules and delegates persistence
 * to the DAOs, which are injected through the constructor (Dependency Inversion / Single Responsibility).
 */
public class StudentController {

    private static final int MIN_AGE = 3;
    private static final int MAX_AGE = 25;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;

    public StudentController(StudentDao studentDao, GuardianDao guardianDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
    }

    /** Rule: every student must be registered together with at least one guardian. */
    public Student registerStudent(Student student, Guardian guardian) {
        validateStudent(student);
        validateGuardian(guardian);
        if (studentDao.findByDocument(student.getDocumentNumber()).isPresent()) {
            throw new BusinessException("Ya existe un estudiante con el documento " + student.getDocumentNumber() + ".");
        }
        return studentDao.registerWithGuardian(student, guardian);
    }

    public List<Student> listStudents() {
        return studentDao.findAll();
    }

    public List<Student> listByStatus(StudentStatus status) {
        return studentDao.findAll().stream()
                .filter(student -> student.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Student> searchByName(String term) {
        return studentDao.searchByName(term);
    }

    public Optional<Student> findByDocument(String documentNumber) {
        return studentDao.findByDocument(documentNumber);
    }

    public Optional<Guardian> findGuardianByDocument(String documentNumber) {
        return guardianDao.findByDocument(documentNumber);
    }

    public List<Guardian> getGuardians(int studentId) {
        return guardianDao.findByStudentId(studentId);
    }

    /** Only the non-null arguments are changed; null means "keep the current value". */
    public void updateContactInfo(Student student, String address, String phone, String email,
                                  String bloodType, String allergies, String medicalConditions) {
        if (address != null) student.setAddress(address);
        if (phone != null) student.setPhone(phone);
        if (email != null) student.setEmail(email);
        if (bloodType != null) student.setBloodType(bloodType);
        if (allergies != null) student.setAllergies(allergies);
        if (medicalConditions != null) student.setMedicalConditions(medicalConditions);
        validateContactData(student);
        if (!studentDao.update(student)) {
            throw new BusinessException("No se pudo actualizar el estudiante.");
        }
    }

    public void changeStatus(Student student, StudentStatus newStatus) {
        if (student.getStatus() == newStatus) {
            throw new BusinessException("El estudiante ya tiene el estado " + newStatus.getLabel() + ".");
        }
        if (!studentDao.updateStatus(student.getId(), newStatus)) {
            throw new BusinessException("No se pudo cambiar el estado.");
        }
        student.setStatus(newStatus);
    }

    /** Number of students per status (used by the summary screen). */
    public Map<StudentStatus, Long> countByStatus() {
        return studentDao.findAll().stream()
                .collect(Collectors.groupingBy(Student::getStatus, Collectors.counting()));
    }

    // ---------------------------------------------------------------- validation

    private void validateStudent(Student student) {
        require(Validator.isValidDocument(student.getDocumentNumber()), "El documento debe tener entre 5 y 20 letras o números.");
        require(Validator.isValidName(student.getFirstName()), "El primer nombre solo puede contener letras.");
        require(student.getMiddleName() == null || Validator.isValidName(student.getMiddleName()), "El segundo nombre solo puede contener letras.");
        require(Validator.isValidName(student.getFirstSurname()), "El primer apellido solo puede contener letras.");
        require(student.getSecondSurname() == null || Validator.isValidName(student.getSecondSurname()), "El segundo apellido solo puede contener letras.");
        LocalDate today = LocalDate.now();
        require(student.getBirthDate() != null && student.getBirthDate().isBefore(today), "La fecha de nacimiento debe ser anterior a hoy.");
        require(student.getAge() >= MIN_AGE && student.getAge() <= MAX_AGE,
                "La edad del estudiante debe estar entre " + MIN_AGE + " y " + MAX_AGE + " años.");
        require(student.getAdmissionDate() != null && !student.getAdmissionDate().isAfter(today)
                && !student.getAdmissionDate().isBefore(student.getBirthDate()), "La fecha de ingreso no es válida.");
        validateContactData(student);
    }

    private void validateContactData(Student student) {
        require(student.getPhone() == null || Validator.isValidPhone(student.getPhone()), "El teléfono debe tener entre 7 y 15 dígitos.");
        require(student.getEmail() == null || Validator.isValidEmail(student.getEmail()), "El correo electrónico no es válido.");
        require(student.getBloodType() == null || Validator.isValidBloodType(student.getBloodType()), "El tipo de sangre debe ser como A+, O-, AB+.");
    }

    private void validateGuardian(Guardian guardian) {
        require(guardian != null, "El estudiante debe tener al menos un acudiente.");
        require(Validator.isValidDocument(guardian.getDocumentNumber()), "El documento del acudiente no es válido.");
        require(Validator.isValidName(guardian.getFirstName()), "El nombre del acudiente solo puede contener letras.");
        require(Validator.isValidName(guardian.getFirstSurname()), "El apellido del acudiente solo puede contener letras.");
        require(guardian.getRelationship() != null && !guardian.getRelationship().isBlank(), "Indique el parentesco del acudiente.");
        require(Validator.isValidPhone(guardian.getPrimaryPhone()), "El teléfono del acudiente debe tener entre 7 y 15 dígitos.");
        require(guardian.getEmail() == null || Validator.isValidEmail(guardian.getEmail()), "El correo del acudiente no es válido.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
