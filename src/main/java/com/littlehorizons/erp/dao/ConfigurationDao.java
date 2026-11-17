package com.littlehorizons.erp.dao;

import java.util.Map;

public interface ConfigurationDao {

    /** Every key/value pair of the school configuration. */
    Map<String, String> findAll();
}
