package com.hrms.payroll.utils;

import org.mindrot.jbcrypt.BCrypt;

/** Thin wrapper around jBCrypt so no other class needs to know the hashing algorithm in use. */
public final class PasswordHasher {

    private PasswordHasher() {}

    public static String hash(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainTextPassword, String hashed) {
        try {
            return BCrypt.checkpw(plainTextPassword, hashed);
        } catch (IllegalArgumentException e) {
            // Malformed hash in DB — treat as verification failure rather than crashing login.
            return false;
        }
    }
}
