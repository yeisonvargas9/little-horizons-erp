package com.littlehorizons.erp.exception;

/** Thrown when a business rule is violated (invalid data, duplicates, etc.). The message is user-friendly. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
