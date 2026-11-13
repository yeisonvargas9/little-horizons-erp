package com.littlehorizons.erp.util;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Small helpers shared by the DAO implementations. */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    public static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    public static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
