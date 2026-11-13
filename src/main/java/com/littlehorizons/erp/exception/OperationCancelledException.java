package com.littlehorizons.erp.exception;

/** Thrown when the user types the cancel keyword at any prompt, to go back to the previous menu. */
public class OperationCancelledException extends RuntimeException {

    public OperationCancelledException() {
        super("Operation cancelled by the user");
    }
}
