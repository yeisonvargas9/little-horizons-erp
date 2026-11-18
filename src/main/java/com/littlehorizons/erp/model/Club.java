package com.littlehorizons.erp.model;

/** An extracurricular club (table "clubes") with its current number of active members. */
public record Club(int id, String name, String description, Integer supervisorId, String supervisorName,
                   int capacity, String meetingSchedule, boolean active, int activeMembers) {

    public int freeSpots() {
        return capacity - activeMembers;
    }
}
