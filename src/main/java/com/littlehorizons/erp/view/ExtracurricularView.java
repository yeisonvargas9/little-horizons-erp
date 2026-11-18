package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.Membership;
import com.littlehorizons.erp.model.Participation;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;

/** Console screens for clubs, memberships, events and participants. */
public class ExtracurricularView {

    private static final String CANCEL_HINT = "(escriba 'cancelar' en cualquier momento para volver)";

    private final ExtracurricularController controller;
    private final ConsoleInput input;

    public ExtracurricularView(ExtracurricularController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 9)) {
                    case 1 -> showClubs();
                    case 2 -> createClub();
                    case 3 -> joinClub();
                    case 4 -> leaveClub();
                    case 5 -> showMembers();
                    case 6 -> showEvents();
                    case 7 -> createEvent();
                    case 8 -> registerParticipant();
                    case 9 -> showParticipants();
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
        System.out.println("\n=== ACTIVIDADES EXTRACURRICULARES ===");
        System.out.println("1. Ver clubes (con cupos)");
        System.out.println("2. Crear un club");
        System.out.println("3. Inscribir estudiante en un club");
        System.out.println("4. Retirar estudiante de un club");
        System.out.println("5. Ver miembros de un club");
        System.out.println("6. Ver calendario de eventos");
        System.out.println("7. Crear un evento");
        System.out.println("8. Registrar participante en un evento");
        System.out.println("9. Ver participantes y posiciones de un evento");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    // ---------------------------------------------------------------- clubs

    private void showClubs() {
        List<Club> clubs = controller.listClubs();
        System.out.printf("%n%-4s %-32s %-22s %8s %-24s%n", "ID", "Club", "Encargado", "Cupos", "Horario");
        System.out.println("-".repeat(94));
        clubs.forEach(c -> System.out.printf("%-4d %-32s %-22s %3d/%-4d %-24s%n", c.id(), c.name(),
                c.supervisorName() == null ? "-" : c.supervisorName(), c.activeMembers(), c.capacity(),
                c.meetingSchedule() == null ? "-" : c.meetingSchedule()));
        System.out.println("Total de clubes: " + clubs.size() + "   (cupos = miembros activos / máximo)");
    }

    private void createClub() {
        System.out.println("\n--- Crear club --- " + CANCEL_HINT);
        String name = input.readRequired("Nombre del club");
        String description = input.readOptional("Descripción (opcional)");
        String supervisor = input.readOptionalValidated("Documento del profesor encargado (opcional)",
                Validator::isValidDocument, "Use entre 5 y 20 letras o números.");
        int capacity = input.readInt("Cupo máximo de estudiantes (1 a 100)", 1, 100);
        String schedule = input.readOptional("Horario de reunión (ej: Martes 3:00-5:00 PM)");
        Club club = controller.createClub(name, description, supervisor, capacity, schedule);
        System.out.println("  ✔ Club creado con ID " + club.id() + ".");
    }

    private void joinClub() {
        Student student = askStudent();
        Club club = askClub();
        controller.joinClub(student, club);
        System.out.println("  ✔ " + student.getFullName() + " inscrito en " + club.name() + ".");
    }

    private void leaveClub() {
        Student student = askStudent();
        Club club = askClub();
        controller.leaveClub(student, club, input.readRequired("Motivo del retiro"));
        System.out.println("  ✔ Retiro registrado.");
    }

    private void showMembers() {
        Club club = askClub();
        List<Membership> members = controller.listMembers(club, !input.readYesNo("¿Incluir también los retirados?"));
        System.out.println("\nMiembros de " + club.name() + ":");
        if (members.isEmpty()) {
            System.out.println("  Sin miembros.");
        }
        members.forEach(m -> System.out.printf("  %-34s ingresó %s  %s%s%n", m.studentName(), m.joinDate(),
                m.status().getLabel(), m.leaveDate() == null ? "" : " (" + m.leaveDate() + ": " + m.leaveReason() + ")"));
    }

    // ---------------------------------------------------------------- events

    private void showEvents() {
        List<SchoolEvent> events = controller.listEvents();
        System.out.printf("%n%-4s %-34s %-11s %-12s %-12s %s%n", "ID", "Evento", "Fecha", "Tipo", "Competencia", "Lugar");
        System.out.println("-".repeat(96));
        events.forEach(e -> System.out.printf("%-4d %-34s %-11s %-12s %-12s %s%n", e.id(), e.name(), e.date(),
                e.type().getLabel(), e.hasCompetition() ? "Sí" : "No", e.place() == null ? "-" : e.place()));
        System.out.println("Total de eventos: " + events.size());
    }

    private void createEvent() {
        System.out.println("\n--- Crear evento --- " + CANCEL_HINT);
        String name = input.readRequired("Nombre del evento");
        String description = input.readOptional("Descripción (opcional)");
        EventType type = input.readChoice("Tipo de evento", EventType.values(), EventType::getLabel);
        LocalDate date = input.readDate("Fecha (AAAA-MM-DD)");
        String place = input.readOptional("Lugar (opcional)");
        boolean competition = input.readYesNo("¿Tiene competencia con posiciones?");
        String organizer = input.readOptionalValidated("Documento del profesor organizador (opcional)",
                Validator::isValidDocument, "Use entre 5 y 20 letras o números.");
        SchoolEvent event = controller.createEvent(name, description, type, date, place, competition, organizer);
        System.out.println("  ✔ Evento creado con ID " + event.id() + ".");
    }

    private void registerParticipant() {
        SchoolEvent event = askEvent();
        Student student = askStudent();
        ParticipationRole role = event.hasCompetition()
                ? input.readChoice("Rol", new ParticipationRole[]{ParticipationRole.COMPETITOR, ParticipationRole.ORGANIZER,
                ParticipationRole.ATTENDEE}, ParticipationRole::getLabel)
                : input.readChoice("Rol", new ParticipationRole[]{ParticipationRole.PARTICIPANT, ParticipationRole.ORGANIZER,
                ParticipationRole.ATTENDEE}, ParticipationRole::getLabel);
        String position = null;
        if (event.hasCompetition() && role == ParticipationRole.COMPETITOR) {
            String[] options = {"1er Lugar", "2do Lugar", "3er Lugar", "Participante"};
            position = input.readChoice("Posición obtenida", options, s -> s);
        }
        controller.registerParticipant(event, student, role, position);
        System.out.println("  ✔ " + student.getFullName() + " registrado en " + event.name()
                + (position == null ? "." : " (" + position + ")."));
    }

    private void showParticipants() {
        SchoolEvent event = askEvent();
        List<Participation> participants = controller.listParticipants(event);
        System.out.println("\nParticipantes de " + event.name() + " (" + event.date() + "):");
        if (participants.isEmpty()) {
            System.out.println("  Sin participantes registrados.");
        }
        participants.forEach(p -> System.out.printf("  %-34s %-13s %s%n", p.studentName(), p.role().getLabel(),
                p.position() == null ? "" : p.position()));
        System.out.println("Total: " + participants.size());
    }

    // ---------------------------------------------------------------- helpers

    private Student askStudent() {
        String document = input.readValidated("Documento del estudiante " + CANCEL_HINT, Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        return controller.findStudent(document)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con el documento " + document + "."));
    }

    private Club askClub() {
        int id = input.readInt("ID del club (véalo en la opción 1)", 1, Integer.MAX_VALUE);
        return controller.findClub(id).orElseThrow(() -> new BusinessException("No existe un club con ID " + id + "."));
    }

    private SchoolEvent askEvent() {
        int id = input.readInt("ID del evento (véalo en la opción 6)", 1, Integer.MAX_VALUE);
        return controller.findEvent(id).orElseThrow(() -> new BusinessException("No existe un evento con ID " + id + "."));
    }
}
