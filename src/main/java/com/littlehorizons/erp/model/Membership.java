package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A student's membership in a club (table "membresias_club"). */
public record Membership(int id, int studentId, String studentName, int clubId, LocalDate joinDate,
                         MembershipStatus status, LocalDate leaveDate, String leaveReason) {
}
