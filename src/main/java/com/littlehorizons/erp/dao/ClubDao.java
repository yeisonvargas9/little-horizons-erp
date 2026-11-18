package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;

import java.util.List;
import java.util.Optional;

public interface ClubDao {

    Club save(Club club);

    Optional<Club> findById(int id);

    Optional<Club> findByName(String name);

    /** All clubs with their number of active members. */
    List<Club> findAll();
}
