package com.hrms.payroll.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Centralized currency formatting so every screen/report renders numbers identically. */
public final class CurrencyFormatter {

    // Change Locale here to re-brand the whole app's currency (e.g. Locale.US for $).
    private static final Locale CURRENCY_LOCALE = new Locale("en", "IN");
    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(CURRENCY_LOCALE);

    private CurrencyFormatter() {}

    public static String format(BigDecimal amount) {
        if (amount == null) return FORMAT.format(0);
        return FORMAT.format(amount);
    }

    public static String formatPlain(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
