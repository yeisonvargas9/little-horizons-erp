package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;

import java.util.List;
import java.util.Optional;

public interface EventDao {

    SchoolEvent save(SchoolEvent event);

    Optional<SchoolEvent> findById(int id);

    /** All events ordered by date. */
    List<SchoolEvent> findAll();
}
