package com.hrms.payroll.dao;

/**
 * Custom unchecked exception wrapping low-level SQLExceptions so the
 * service/UI layers never need to import java.sql directly.
 * Demonstrates the "wrap and rethrow" exception-handling pattern.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
