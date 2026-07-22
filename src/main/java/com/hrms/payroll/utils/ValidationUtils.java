package com.hrms.payroll.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Stateless utility class of static validation helpers used by both the
 * UI forms and the service layer (defense in depth).
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{7,15}$");

    private ValidationUtils() {
        // Utility class: prevent instantiation.
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNonNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static boolean isValidPercent(BigDecimal value) {
        return value != null
                && value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(new BigDecimal("100")) <= 0;
    }

    public static boolean isNotFutureDate(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    public static boolean isValidPassword(String password) {
        // At least 6 chars; kept simple for demo purposes but easy to extend.
        return isNotBlank(password) && password.length() >= 6;
    }
}
