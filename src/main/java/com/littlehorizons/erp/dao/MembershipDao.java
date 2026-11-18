package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Membership;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembershipDao {

    /**
     * Joins the student to the club ONLY IF there is still room (the capacity check and the insert are one SQL
     * statement, so two people cannot take the last spot at the same time). Returns false when the club is full.
     */
    boolean enrollIfRoom(int studentId, int clubId, LocalDate joinDate);

    Optional<Membership> findActive(int studentId, int clubId);

    boolean withdraw(int membershipId, LocalDate leaveDate, String reason);

    List<Membership> findByClub(int clubId, boolean activeOnly);
}
