package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Participation;

import java.util.List;

public interface ParticipationDao {

    Participation save(Participation participation);

    boolean exists(int eventId, int studentId);

    boolean isPositionTaken(int eventId, String position);

    List<Participation> findByEvent(int eventId);
}
