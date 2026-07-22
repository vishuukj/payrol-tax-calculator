package com.hrms.payroll.controller;

import com.hrms.payroll.model.User;
import com.hrms.payroll.service.AuthService;
import com.hrms.payroll.service.AuthenticationException;

/**
 * Mediates between the LoginFrame (view) and AuthService (model/service).
 * Keeps Swing event-handling code out of the business layer and vice versa.
 */
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    /** Attempts login; returns the authenticated user or throws AuthenticationException with a UI-friendly message. */
    public User attemptLogin(String username, String password) throws AuthenticationException {
        return authService.login(username, password);
    }

    public void logout() {
        authService.logout();
    }
}
