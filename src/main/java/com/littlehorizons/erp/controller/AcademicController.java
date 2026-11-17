package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.AbsenceSummary;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.CourseAverage;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Period;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.StudentSummary;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectGrades;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic of the academic module: yearly enrollment, subject enrollment, grades with averages,
 * academic history and daily attendance with the absence report.
 */
public class AcademicController {

    /** "More than 3 absences in a week" => the report lists students with at least 4 absent days. */
    public static final int ABSENCE_LIMIT_PER_WEEK = 3;
    private static final String DEFAULT_SECTION = "A";
    private static final BigDecimal MIN_GRADE = new BigDecimal("0.0");
    private static final BigDecimal MAX_GRADE = new BigDecimal("5.0");

    private final StudentDao studentDao;
    private final SubjectDao subjectDao;
    private final EnrollmentDao enrollmentDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AcademicRecordDao recordDao;
    private final AttendanceDao attendanceDao;
    private final AssignmentDao assignmentDao;

    public AcademicController(StudentDao studentDao, SubjectDao subjectDao, EnrollmentDao enrollmentDao,
                              SubjectEnrollmentDao subjectEnrollmentDao, AcademicRecordDao recordDao,
                              AttendanceDao attendanceDao, AssignmentDao assignmentDao) {
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
        this.enrollmentDao = enrollmentDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.recordDao = recordDao;
        this.attendanceDao = attendanceDao;
        this.assignmentDao = assignmentDao;
    }

    public int currentAcademicYear() {
        return Year.now().getValue();
    }

    public Optional<Student> findStudent(String documentNumber) {
        return studentDao.findByDocument(documentNumber);
    }

    public Optional<Subject> findSubject(int subjectId) {
        return subjectDao.findById(subjectId);
    }

    public List<Subject> subjectsOfGrade(int gradeId) {
        return subjectDao.findByGradeId(gradeId);
    }

    // ---------------------------------------------------------------- enrollment

    /**
     * Rules: the student must be active, can only have one enrollment per year, and may repeat the grade
     * of their last enrollment or move up exactly one grade.
     */
    public Enrollment enrollInGrade(Student student, int gradeId, int academicYear) {
        require(student.getStatus() == StudentStatus.ACTIVE, "Solo se pueden matricular estudiantes activos.");
        require(gradeId >= 1 && gradeId <= 12, "El grado no es válido.");
        require(enrollmentDao.findByStudentAndYear(student.getId(), academicYear).isEmpty(),
                "El estudiante ya está matriculado en " + academicYear + ".");
        enrollmentDao.findByStudent(student.getId()).stream().findFirst().ifPresent(last ->
                require(gradeId == last.gradeId() || gradeId == last.gradeId() + 1,
                        "Desde " + last.gradeName() + " solo puede matricularse en el mismo grado o en el siguiente."));
        return enrollmentDao.save(new Enrollment(0, student.getId(), gradeId, "", academicYear,
                EnrollmentStatus.ACTIVE, LocalDate.now()));
    }

    public List<Enrollment> getEnrollments(int studentId) {
        return enrollmentDao.findByStudent(studentId);
    }

    /** Enrolls the student in every subject of their grade that they are not enrolled in yet. Returns how many. */
    public int enrollInAllSubjects(Student student, Period period, int academicYear) {
        Enrollment enrollment = requireActiveEnrollment(student, academicYear);
        requireRegularPeriod(period);
        List<Integer> pending = subjectDao.findByGradeId(enrollment.gradeId()).stream()
                .filter(Subject::isActive)
                .map(Subject::getId)
                .filter(id -> !subjectEnrollmentDao.isEnrolled(student.getId(), id, academicYear, period))
                .collect(Collectors.toList());
        require(!pending.isEmpty(), "El estudiante ya está inscrito en todas las materias de su grado en ese período.");
        subjectEnrollmentDao.enrollMany(student.getId(), pending, academicYear, period, DEFAULT_SECTION);
        return pending.size();
    }

    public void enrollInSubject(Student student, Subject subject, Period period, int academicYear) {
        Enrollment enrollment = requireActiveEnrollment(student, academicYear);
        requireRegularPeriod(period);
        require(subject.getGradeId() == enrollment.gradeId(),
                "La materia es de " + subject.getGradeName() + " y el estudiante cursa " + enrollment.gradeName() + ".");
        require(!subjectEnrollmentDao.isEnrolled(student.getId(), subject.getId(), academicYear, period),
                "El estudiante ya está inscrito en esa materia en el " + period.getLabel() + ".");
        subjectEnrollmentDao.enrollMany(student.getId(), List.of(subject.getId()), academicYear, period, DEFAULT_SECTION);
    }

    // ---------------------------------------------------------------- grades

    /** Registers a grade, or corrects it if the student already has one for that subject and period. */
    public void registerGrade(Student student, Subject subject, Period period, BigDecimal value,
                              String observations, int academicYear) {
        require(value != null && value.scale() <= 1 && value.compareTo(MIN_GRADE) >= 0 && value.compareTo(MAX_GRADE) <= 0,
                "La nota debe estar entre 0.0 y 5.0 con un solo decimal.");
        requireActiveEnrollment(student, academicYear);
        Period toCheck = period == Period.FINAL ? null : period;
        require(subjectEnrollmentDao.isEnrolled(student.getId(), subject.getId(), academicYear, toCheck),
                "El estudiante no está inscrito en esa materia" + (toCheck == null ? "." : " en el " + period.getLabel() + "."));
        int professorId = assignmentDao.findProfessorId(subject.getId(), academicYear)
                .orElseThrow(() -> new BusinessException("La materia no tiene un profesor asignado en " + academicYear + "."));
        recordDao.saveOrUpdate(new AcademicRecord(0, student.getId(), subject.getId(), subject.getName(),
                professorId, academicYear, period, value, observations, LocalDate.now()));
    }

    /** Complete history of grades of a student since they entered the school. */
    public List<AcademicRecord> getHistory(int studentId) {
        return recordDao.findByStudent(studentId);
    }

    /** Report card of a year: one row per subject with its notes by period and its average (Streams). */
    public List<SubjectGrades> buildReportCard(List<AcademicRecord> history, int academicYear) {
        Map<String, List<AcademicRecord>> bySubject = history.stream()
                .filter(r -> r.academicYear() == academicYear && r.period() != Period.FINAL)
                .collect(Collectors.groupingBy(AcademicRecord::subjectName, TreeMap::new, Collectors.toList()));
        return bySubject.entrySet().stream().map(entry -> {
            Map<Period, BigDecimal> notes = new EnumMap<>(Period.class);
            entry.getValue().forEach(r -> notes.put(r.period(), r.value()));
            double average = entry.getValue().stream().mapToDouble(r -> r.value().doubleValue()).average().orElse(0);
            return new SubjectGrades(entry.getKey(), notes, average);
        }).collect(Collectors.toList());
    }

    /** General average of the student in each period of the year (average of all their subjects). */
    public Map<Period, Double> generalAverageByPeriod(List<AcademicRecord> history, int academicYear) {
        return history.stream()
                .filter(r -> r.academicYear() == academicYear && r.period() != Period.FINAL)
                .collect(Collectors.groupingBy(AcademicRecord::period, () -> new EnumMap<>(Period.class),
                        Collectors.averagingDouble(r -> r.value().doubleValue())));
    }

    /** Average of the whole course in each subject of the grade (null period = all periods). */
    public List<CourseAverage> courseAverages(int gradeId, int academicYear, Period period) {
        return subjectDao.findByGradeId(gradeId).stream()
                .map(subject -> new CourseAverage(subject.getName(),
                        recordDao.averageBySubject(subject.getId(), academicYear, period).orElse(null), 0))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- attendance

    public List<StudentSummary> getClassList(Subject subject, int academicYear) {
        return enrollmentDao.findStudentsByGrade(subject.getGradeId(), academicYear);
    }

    /** Saves the attendance of a whole class for one day. A second save for the same day corrects the first. */
    public int registerAttendance(Subject subject, LocalDate date, Map<Integer, AttendanceStatus> marks) {
        require(!date.isAfter(LocalDate.now()), "No se puede registrar asistencia de una fecha futura.");
        require(date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY,
                "El colegio no tiene clases los fines de semana.");
        require(!marks.isEmpty(), "No hay estudiantes para registrar asistencia.");
        Integer professorId = assignmentDao.findProfessorId(subject.getId(), date.getYear()).orElse(null);
        List<AttendanceRecord> records = marks.entrySet().stream()
                .map(e -> new AttendanceRecord(e.getKey(), subject.getId(), date, e.getValue(), null, professorId))
                .collect(Collectors.toList());
        attendanceDao.saveAll(records);
        return records.size();
    }

    /** Students with more than {@link #ABSENCE_LIMIT_PER_WEEK} absent days in the week that contains the date. */
    public List<AbsenceSummary> absenceReport(LocalDate anyDayOfWeek) {
        LocalDate monday = anyDayOfWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);
        return attendanceDao.findAbsenceSummary(monday, friday, ABSENCE_LIMIT_PER_WEEK + 1);
    }

    // ---------------------------------------------------------------- helpers

    private Enrollment requireActiveEnrollment(Student student, int academicYear) {
        return enrollmentDao.findByStudentAndYear(student.getId(), academicYear)
                .filter(e -> e.status() == EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene una matrícula activa en " + academicYear + "."));
    }

    private void requireRegularPeriod(Period period) {
        require(Period.regular().contains(period), "La inscripción solo aplica a los períodos I, II, III y IV.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
