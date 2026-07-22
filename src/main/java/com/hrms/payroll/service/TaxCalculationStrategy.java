package com.hrms.payroll.service;

import java.math.BigDecimal;

/**
 * Strategy interface for computing income tax on a given taxable amount.
 * Demonstrates polymorphism/abstraction: the PayrollService depends only on
 * this contract, not on a concrete calculation algorithm, so alternative
 * strategies (e.g. flat-rate, regional) can be swapped in without touching
 * payroll logic.
 */
public interface TaxCalculationStrategy {
    BigDecimal calculateTax(BigDecimal taxableAmount);
}
