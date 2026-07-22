package com.hrms.payroll.service;

/** Thrown for invalid login credentials or inactive accounts. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
