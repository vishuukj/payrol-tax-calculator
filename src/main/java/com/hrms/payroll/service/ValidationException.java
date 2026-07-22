package com.hrms.payroll.service;

/** Thrown when input fails a business validation rule before hitting the database. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
