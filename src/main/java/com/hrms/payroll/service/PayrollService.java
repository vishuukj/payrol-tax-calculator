package com.hrms.payroll.service;

import com.hrms.payroll.dao.EmployeeDAO;
import com.hrms.payroll.dao.PayrollDAO;
import com.hrms.payroll.dao.impl.ActivityLogDAO;
import com.hrms.payroll.model.Employee;
import com.hrms.payroll.model.Payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core business logic for payroll and salary-slip calculations.
 *
 *   Gross Salary  = Basic + Bonus + Allowances
 *   PF Deduction  = Basic Salary x PF%
 *   Taxable Amount = Gross Salary - PF Deduction   (PF is pre-tax)
 *   Income Tax    = TaxCalculationStrategy.calculateTax(Taxable Amount)
 *   Net Salary    = Gross Salary - PF Deduction - Income Tax - Other Deductions
 */
public class PayrollService {

    private final EmployeeDAO employeeDAO;
    private final PayrollDAO payrollDAO;
    private final TaxCalculationStrategy taxCalculator;
    private final ActivityLogDAO activityLogDAO;

    public PayrollService(EmployeeDAO employeeDAO, PayrollDAO payrollDAO,
                           TaxCalculationStrategy taxCalculator, ActivityLogDAO activityLogDAO) {
        this.employeeDAO = employeeDAO;
        this.payrollDAO = payrollDAO;
        this.taxCalculator = taxCalculator;
        this.activityLogDAO = activityLogDAO;
    }

    /** Simple value object returned to the UI for the on-screen salary breakdown / slip. */
    public record SalaryBreakdown(
            Employee employee,
            BigDecimal grossSalary,
            BigDecimal pfDeduction,
            BigDecimal taxableAmount,
            BigDecimal incomeTax,
            BigDecimal otherDeductions,
            BigDecimal netSalary
    ) {}

    public SalaryBreakdown calculate(Employee emp) {
        BigDecimal gross = emp.getBasicSalary().add(emp.getBonus()).add(emp.getAllowances());

        BigDecimal pf = emp.getBasicSalary()
                .multiply(emp.getPfPercent())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Annualize for slab lookup (slabs are commonly defined on annual income),
        // compute tax annually, then bring back to a monthly figure.
        BigDecimal taxableMonthly = gross.subtract(pf);
        BigDecimal taxableAnnual = taxableMonthly.multiply(BigDecimal.valueOf(12));

        BigDecimal annualTax = taxCalculator.calculateTax(taxableAnnual);
        BigDecimal monthlyTax = annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal net = gross.subtract(pf).subtract(monthlyTax).subtract(emp.getOtherDeductions());

        return new SalaryBreakdown(emp, gross, pf, taxableMonthly, monthlyTax, emp.getOtherDeductions(), net);
    }

    /** Calculates and persists a payroll record for the given employee/period (e.g. "2026-07"). */
    public Payroll generateAndSave(int employeeId, String payPeriod, Integer generatedByUserId) {
        Employee emp = employeeDAO.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Employee not found: " + employeeId));

        SalaryBreakdown breakdown = calculate(emp);

        Payroll payroll = new Payroll();
        payroll.setEmployeeId(emp.getEmployeeId());
        payroll.setPayPeriod(payPeriod);
        payroll.setGrossSalary(breakdown.grossSalary());
        payroll.setPfDeduction(breakdown.pfDeduction());
        payroll.setIncomeTax(breakdown.incomeTax());
        payroll.setOtherDeductions(breakdown.otherDeductions());
        payroll.setNetSalary(breakdown.netSalary());

        Payroll saved = payrollDAO.save(payroll);
        activityLogDAO.log(generatedByUserId, "Generated payroll for " + emp.getFullName() + " (" + payPeriod + ")",
                "PAYROLL", saved.getPayrollId());
        return saved;
    }

    /** Runs payroll for every active employee for the given period ("Run Payroll" bulk action). */
    public List<Payroll> runPayrollForAll(String payPeriod, Integer generatedByUserId) {
        List<Employee> employees = employeeDAO.findAll();
        return employees.stream()
                .map(e -> generateAndSave(e.getEmployeeId(), payPeriod, generatedByUserId))
                .toList();
    }

    public static String currentPayPeriod() {
        return YearMonth.now().toString(); // "2026-07"
    }

    public Optional<Payroll> findExisting(int employeeId, String payPeriod) {
        return payrollDAO.findByEmployeeAndPeriod(employeeId, payPeriod);
    }

    public List<Payroll> historyFor(int employeeId) {
        return payrollDAO.findByEmployee(employeeId);
    }

    public Map<String, BigDecimal> departmentWisePayroll() {
        return payrollDAO.payrollByDepartment();
    }

    public Map<String, BigDecimal> monthlyTrend(int months) {
        return payrollDAO.monthlyPayrollTrend(months);
    }
}
