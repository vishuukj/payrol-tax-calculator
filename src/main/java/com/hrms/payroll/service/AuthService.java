package com.hrms.payroll.service;

import com.hrms.payroll.dao.UserDAO;
import com.hrms.payroll.dao.impl.ActivityLogDAO;
import com.hrms.payroll.model.User;
import com.hrms.payroll.utils.PasswordHasher;
import com.hrms.payroll.utils.ValidationUtils;

/**
 * Handles authentication. Holds the currently logged-in user for the
 * duration of the desktop session (single-user desktop app, so a simple
 * field is appropriate here rather than a token/session store).
 */
public class AuthService {

    private final UserDAO userDAO;
    private final ActivityLogDAO activityLogDAO;
    private User currentUser;

    public AuthService(UserDAO userDAO, ActivityLogDAO activityLogDAO) {
        this.userDAO = userDAO;
        this.activityLogDAO = activityLogDAO;
    }

    public User login(String username, String password) {
        if (!ValidationUtils.isNotBlank(username) || !ValidationUtils.isNotBlank(password)) {
            throw new AuthenticationException("Username and password are required.");
        }

        User user = userDAO.findByUsername(username.trim())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password."));

        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. Contact your administrator.");
        }
        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }

        userDAO.updateLastLogin(user.getUserId());
        activityLogDAO.log(user.getUserId(), user.getUsername() + " logged in", "AUTH", user.getUserId());
        this.currentUser = user;
        return user;
    }

    public void logout() {
        if (currentUser != null) {
            activityLogDAO.log(currentUser.getUserId(), currentUser.getUsername() + " logged out", "AUTH", currentUser.getUserId());
        }
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
