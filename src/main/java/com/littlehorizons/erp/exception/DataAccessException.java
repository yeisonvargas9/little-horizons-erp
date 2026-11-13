package com.littlehorizons.erp.exception;

/** Unchecked wrapper for any database failure (the original SQLException is kept as the cause). */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
