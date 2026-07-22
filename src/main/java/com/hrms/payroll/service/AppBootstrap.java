package com.hrms.payroll.service;

import com.hrms.payroll.dao.UserDAO;
import com.hrms.payroll.model.User;
import com.hrms.payroll.utils.PasswordHasher;

/**
 * Runs once at application startup (before the login screen appears) to
 * guarantee a usable admin account exists. Creating it here — rather than
 * as a raw INSERT in schema.sql — means the BCrypt hash is always generated
 * correctly by the same PasswordHasher the login flow verifies against.
 */
public final class AppBootstrap {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "Admin@123";

    private AppBootstrap() {}

    public static void ensureDefaultAdminExists() {
        UserDAO userDAO = AppContext.getInstance().userDAO;
        if (userDAO.findByUsername(DEFAULT_USERNAME).isEmpty()) {
            User admin = new User(
                    DEFAULT_USERNAME,
                    PasswordHasher.hash(DEFAULT_PASSWORD),
                    "System Administrator",
                    User.Role.ADMIN,
                    "admin@hrms.local"
            );
            userDAO.save(admin);
        }
    }
}
