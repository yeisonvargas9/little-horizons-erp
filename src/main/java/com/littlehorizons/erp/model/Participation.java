package com.littlehorizons.erp.model;

/** A student's participation in an event, with the position obtained when there is a competition. */
public record Participation(int id, int eventId, int studentId, String studentName, ParticipationRole role,
                            String position) {
}
