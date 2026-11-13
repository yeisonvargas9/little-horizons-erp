package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ProfessorStatus;
import com.littlehorizons.erp.model.ScheduleEntry;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Business logic for professors, the subject catalog and teaching assignments with their schedule. */
public class ProfessorController {

    private static final LocalTime SCHOOL_OPENS = LocalTime.of(6, 0);
    private static final LocalTime SCHOOL_CLOSES = LocalTime.of(18, 0);

    private final ProfessorDao professorDao;
    private final SubjectDao subjectDao;
    private final AssignmentDao assignmentDao;

    public ProfessorController(ProfessorDao professorDao, SubjectDao subjectDao, AssignmentDao assignmentDao) {
        this.professorDao = professorDao;
        this.subjectDao = subjectDao;
        this.assignmentDao = assignmentDao;
    }

    public Professor registerProfessor(Professor professor) {
        require(Validator.isValidDocument(professor.getDocumentNumber()), "El documento debe tener entre 5 y 20 letras o números.");
        require(Validator.isValidName(professor.getFirstName()), "El primer nombre solo puede contener letras.");
        require(professor.getMiddleName() == null || Validator.isValidName(professor.getMiddleName()), "El segundo nombre solo puede contener letras.");
        require(Validator.isValidName(professor.getFirstSurname()), "El primer apellido solo puede contener letras.");
        require(professor.getSecondSurname() == null || Validator.isValidName(professor.getSecondSurname()), "El segundo apellido solo puede contener letras.");
        require(Validator.isValidEmail(professor.getEmail()), "El correo electrónico no es válido.");
        require(professor.getPhone() == null || Validator.isValidPhone(professor.getPhone()), "El teléfono debe tener entre 7 y 15 dígitos.");
        require(professor.getSpecialization() != null && !professor.getSpecialization().isBlank(), "Indique la especialización.");
        require(professor.getHireDate() != null && !professor.getHireDate().isAfter(LocalDate.now()), "La fecha de contratación no puede ser futura.");
        require(professorDao.findByDocument(professor.getDocumentNumber()).isEmpty(), "Ya existe un profesor con ese documento.");
        require(!professorDao.existsByEmail(professor.getEmail()), "Ya existe un profesor con ese correo.");
        return professorDao.save(professor);
    }

    public List<Professor> listProfessors() {
        return professorDao.findAll();
    }

    public Optional<Professor> findByDocument(String documentNumber) {
        return professorDao.findByDocument(documentNumber);
    }

    /** Null arguments keep the current value. */
    public void updateContact(Professor professor, String email, String phone, String specialization) {
        require(email == null || Validator.isValidEmail(email), "El correo electrónico no es válido.");
        require(phone == null || Validator.isValidPhone(phone), "El teléfono debe tener entre 7 y 15 dígitos.");
        if (email != null && !email.equalsIgnoreCase(professor.getEmail())) {
            require(!professorDao.existsByEmail(email), "Ya existe un profesor con ese correo.");
            professor.setEmail(email);
        }
        if (phone != null) professor.setPhone(phone);
        if (specialization != null) professor.setSpecialization(specialization);
        require(professorDao.update(professor), "No se pudo actualizar el profesor.");
    }

    public void changeStatus(Professor professor, ProfessorStatus newStatus) {
        require(professor.getStatus() != newStatus, "El profesor ya tiene el estado " + newStatus.getLabel() + ".");
        require(professorDao.updateStatus(professor.getId(), newStatus), "No se pudo cambiar el estado.");
        professor.setStatus(newStatus);
    }

    public List<Subject> listSubjects(Integer gradeId) {
        return gradeId == null ? subjectDao.findAll() : subjectDao.findByGradeId(gradeId);
    }

    public Optional<Subject> findSubject(int subjectId) {
        return subjectDao.findById(subjectId);
    }

    /**
     * Assigns a subject to a professor with its weekly schedule. Rules: the professor must be active,
     * slots must be inside school hours, must not overlap each other, must not exceed the subject's weekly
     * hours, and must not collide with the professor's other classes or with the grade/section's classes.
     */
    public Assignment assignSubject(Professor professor, Subject subject, int academicYear, String section,
                                    List<ScheduleSlot> slots) {
        require(professor.getStatus() == ProfessorStatus.ACTIVE, "Solo se pueden asignar materias a profesores activos.");
        require(subject.isActive(), "La materia no está activa.");
        require(!slots.isEmpty(), "Debe indicar al menos un horario de clase.");
        require(!assignmentDao.exists(professor.getId(), subject.getId(), academicYear, section),
                "Ese profesor ya dicta esa materia en la sección " + section + ".");

        for (ScheduleSlot slot : slots) {
            require(slot.end().isAfter(slot.start()), "La hora de fin debe ser posterior a la de inicio.");
            require(!slot.start().isBefore(SCHOOL_OPENS) && !slot.end().isAfter(SCHOOL_CLOSES),
                    "El horario debe estar entre las 06:00 y las 18:00.");
        }
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                require(!slots.get(i).overlaps(slots.get(j)), "Los horarios ingresados se cruzan entre sí.");
            }
        }
        double totalHours = slots.stream().mapToDouble(ScheduleSlot::hours).sum();
        require(totalHours <= subject.getWeeklyHours(),
                "La materia tiene " + subject.getWeeklyHours() + " horas semanales y se ingresaron " + totalHours + ".");

        for (ScheduleSlot slot : slots) {
            String when = slot.day().getLabel() + " " + slot.start() + "-" + slot.end();
            require(!assignmentDao.hasProfessorConflict(professor.getId(), academicYear, slot),
                    "El profesor ya tiene otra clase que se cruza el " + when + ".");
            require(!assignmentDao.hasGroupConflict(subject.getGradeId(), academicYear, section, slot),
                    "El grupo " + subject.getGradeName() + " sección " + section + " ya tiene clase el " + when + ".");
        }

        Assignment assignment = new Assignment();
        assignment.setProfessorId(professor.getId());
        assignment.setSubjectId(subject.getId());
        assignment.setSubjectCode(subject.getCode());
        assignment.setSubjectName(subject.getName());
        assignment.setGradeName(subject.getGradeName());
        assignment.setAcademicYear(academicYear);
        assignment.setSection(section);
        assignment.setSlots(slots);
        return assignmentDao.save(assignment);
    }

    public List<Assignment> getAssignments(int professorId, int academicYear) {
        return assignmentDao.findByProfessor(professorId, academicYear);
    }

    /** Flattens every assignment into timetable rows, sorted by day and start time. */
    public List<ScheduleEntry> getWeeklySchedule(int professorId, int academicYear) {
        return getAssignments(professorId, academicYear).stream()
                .flatMap(a -> a.getSlots().stream()
                        .map(s -> new ScheduleEntry(s.day(), s.start(), s.end(), a.getSubjectName(), a.getSection())))
                .sorted(Comparator.comparing(ScheduleEntry::day).thenComparing(ScheduleEntry::start))
                .collect(Collectors.toList());
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
