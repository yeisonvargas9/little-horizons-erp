package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helper that removes the repetitive JDBC plumbing (open connection, PreparedStatement, close, error log)
 * from the DAOs. Always uses PreparedStatement and try-with-resources; failures are logged and wrapped.
 */
public class SqlExecutor {

    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionBody<T> {
        T run(Connection connection) throws SQLException;
    }

    public static final Binder NO_PARAMS = statement -> { };

    private final DatabaseConnection database;

    public SqlExecutor(DatabaseConnection database) {
        this.database = database;
    }

    public <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection connection = database.getConnection()) {
            return query(connection, sql, binder, mapper);
        } catch (SQLException e) {
            throw fail(sql, e);
        }
    }

    public int update(String sql, Binder binder) {
        try (Connection connection = database.getConnection()) {
            return update(connection, sql, binder);
        } catch (SQLException e) {
            throw fail(sql, e);
        }
    }

    /** Runs an INSERT and returns the generated key. */
    public int insert(String sql, Binder binder) {
        try (Connection connection = database.getConnection()) {
            return insert(connection, sql, binder);
        } catch (SQLException e) {
            throw fail(sql, e);
        }
    }

    /** Runs several statements in ONE transaction: commit if the body ends well, rollback if it throws. */
    public <T> T inTransaction(TransactionBody<T> body) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = body.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("transaction", e);
        }
    }

    // ---- connection-level versions, used inside a transaction body

    public static <T> List<T> query(Connection connection, String sql, Binder binder, RowMapper<T> mapper) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        }
    }

    public static int update(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        }
    }

    public static int insert(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    private DataAccessException fail(String sql, SQLException cause) {
        AppLogger.error("SQL failed: " + sql, cause);
        return new DataAccessException("Error de base de datos. Revise error_log.txt para más detalles.", cause);
    }
}
