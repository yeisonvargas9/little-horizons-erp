package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.MembershipDao;
import com.littlehorizons.erp.dao.ParticipationDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.Membership;
import com.littlehorizons.erp.model.Participation;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ProfessorStatus;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business logic of the extracurricular module: clubs, memberships, events and participants. */
public class ExtracurricularController {

    /** Positions of a competition, in order. */
    public static final List<String> PODIUM = List.of("1er Lugar", "2do Lugar", "3er Lugar");
    public static final String PARTICIPANT_POSITION = "Participante";

    private final ClubDao clubDao;
    private final MembershipDao membershipDao;
    private final EventDao eventDao;
    private final ParticipationDao participationDao;
    private final StudentDao studentDao;
    private final ProfessorDao professorDao;

    public ExtracurricularController(ClubDao clubDao, MembershipDao membershipDao, EventDao eventDao,
                                     ParticipationDao participationDao, StudentDao studentDao, ProfessorDao professorDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.eventDao = eventDao;
        this.participationDao = participationDao;
        this.studentDao = studentDao;
        this.professorDao = professorDao;
    }

    public Optional<Student> findStudent(String documentNumber) {
        return studentDao.findByDocument(documentNumber);
    }

    // ---------------------------------------------------------------- clubs

    public List<Club> listClubs() {
        return clubDao.findAll();
    }

    public Optional<Club> findClub(int clubId) {
        return clubDao.findById(clubId);
    }

    public Club createClub(String name, String description, String supervisorDocument, int capacity, String schedule) {
        require(name != null && !name.isBlank(), "El nombre del club es obligatorio.");
        require(capacity >= 1 && capacity <= 100, "El cupo máximo debe estar entre 1 y 100.");
        require(clubDao.findByName(name).isEmpty(), "Ya existe un club con ese nombre.");
        Integer supervisorId = null;
        String supervisorName = null;
        if (supervisorDocument != null) {
            Professor professor = professorDao.findByDocument(supervisorDocument)
                    .orElseThrow(() -> new BusinessException("No existe un profesor con ese documento."));
            require(professor.getStatus() == ProfessorStatus.ACTIVE, "El profesor encargado no está activo.");
            supervisorId = professor.getId();
            supervisorName = professor.getFullName();
        }
        return clubDao.save(new Club(0, name, description, supervisorId, supervisorName, capacity, schedule, true, 0));
    }

    /** Rules: active club and student, not already a member, and the maximum capacity cannot be exceeded. */
    public void joinClub(Student student, Club club) {
        require(student.getStatus() == StudentStatus.ACTIVE, "Solo los estudiantes activos pueden inscribirse en un club.");
        require(club.active(), "El club no está activo.");
        require(membershipDao.findActive(student.getId(), club.id()).isEmpty(),
                student.getFirstName() + " ya es miembro activo de " + club.name() + ".");
        require(membershipDao.enrollIfRoom(student.getId(), club.id(), LocalDate.now()),
                "El club " + club.name() + " está lleno (cupo máximo: " + club.capacity() + ").");
    }

    public void leaveClub(Student student, Club club, String reason) {
        require(reason != null && !reason.isBlank(), "Indique el motivo del retiro.");
        Membership membership = membershipDao.findActive(student.getId(), club.id())
                .orElseThrow(() -> new BusinessException(student.getFirstName() + " no es miembro activo de " + club.name() + "."));
        require(membershipDao.withdraw(membership.id(), LocalDate.now(), reason), "No se pudo registrar el retiro.");
    }

    public List<Membership> listMembers(Club club, boolean activeOnly) {
        return membershipDao.findByClub(club.id(), activeOnly);
    }

    // ---------------------------------------------------------------- events

    public List<SchoolEvent> listEvents() {
        return eventDao.findAll();
    }

    public Optional<SchoolEvent> findEvent(int eventId) {
        return eventDao.findById(eventId);
    }

    public SchoolEvent createEvent(String name, String description, EventType type, LocalDate date, String place,
                                   boolean hasCompetition, String organizerDocument) {
        require(name != null && !name.isBlank(), "El nombre del evento es obligatorio.");
        require(date != null && date.getYear() >= 2000, "La fecha del evento no es válida.");
        Integer organizerId = null;
        String organizerName = null;
        if (organizerDocument != null) {
            Professor professor = professorDao.findByDocument(organizerDocument)
                    .orElseThrow(() -> new BusinessException("No existe un profesor con ese documento."));
            organizerId = professor.getId();
            organizerName = professor.getFullName();
        }
        return eventDao.save(new SchoolEvent(0, name, description, type, date, place, hasCompetition, organizerId,
                organizerName, true));
    }

    /**
     * Rules: the event must have already taken place, the student can register only once, and a position
     * (1st, 2nd, 3rd or Participant) only exists in events with a competition; each podium place is given once.
     */
    public Participation registerParticipant(SchoolEvent event, Student student, ParticipationRole role, String position) {
        require(!event.date().isAfter(LocalDate.now()), "El evento es el " + event.date() + ": solo se registran participantes de eventos ya realizados.");
        require(student.getStatus() == StudentStatus.ACTIVE, "El estudiante no está activo.");
        require(!participationDao.exists(event.id(), student.getId()), "El estudiante ya está registrado en este evento.");
        String finalPosition = null;
        if (event.hasCompetition() && role == ParticipationRole.COMPETITOR) {
            finalPosition = position == null ? PARTICIPANT_POSITION : position;
            if (PODIUM.contains(finalPosition)) {
                require(!participationDao.isPositionTaken(event.id(), finalPosition),
                        "El " + finalPosition + " ya fue asignado en este evento.");
            }
        } else {
            require(position == null, "Solo los competidores de eventos con competencia tienen posición.");
        }
        return participationDao.save(new Participation(0, event.id(), student.getId(), student.getFullName(), role, finalPosition));
    }

    public List<Participation> listParticipants(SchoolEvent event) {
        return participationDao.findByEvent(event.id());
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
