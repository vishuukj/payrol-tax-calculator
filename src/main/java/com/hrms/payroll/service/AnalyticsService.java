package com.hrms.payroll.service;

import com.hrms.payroll.dao.EmployeeDAO;
import com.hrms.payroll.dao.PayrollDAO;
import com.hrms.payroll.model.Employee;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates KPI figures for the dashboard cards and the analytics charts.
 */
public class AnalyticsService {

    private final EmployeeDAO employeeDAO;
    private final PayrollDAO payrollDAO;

    public AnalyticsService(EmployeeDAO employeeDAO, PayrollDAO payrollDAO) {
        this.employeeDAO = employeeDAO;
        this.payrollDAO = payrollDAO;
    }

    /** Immutable snapshot of the six headline dashboard KPI cards. */
    public record DashboardSnapshot(
            int totalEmployees,
            BigDecimal monthlyPayroll,
            BigDecimal totalTaxCollected,
            BigDecimal averageSalary,
            BigDecimal highestSalary,
            BigDecimal lowestSalary
    ) {}

    public DashboardSnapshot getDashboardSnapshot() {
        int total = employeeDAO.countActiveEmployees();
        BigDecimal monthly = payrollDAO.totalPayrollForPeriod(PayrollService.currentPayPeriod());
        BigDecimal tax = payrollDAO.totalTaxCollected();
        BigDecimal avg = payrollDAO.averageSalary();
        BigDecimal max = payrollDAO.highestSalary();
        BigDecimal min = payrollDAO.lowestSalary();
        return new DashboardSnapshot(total, monthly, tax, avg, max, min);
    }

    public Map<String, BigDecimal> departmentWisePayroll() {
        return payrollDAO.payrollByDepartment();
    }

    public Map<String, Integer> employeeDistributionByDepartment() {
        List<Employee> employees = employeeDAO.findAll();
        Map<String, Integer> distribution = new HashMap<>();
        for (Employee e : employees) {
            distribution.merge(e.getDepartmentName(), 1, Integer::sum);
        }
        return distribution;
    }

    /** Buckets employees into salary bands for the salary-distribution pie chart. */
    public Map<String, Integer> salaryDistribution() {
        List<Employee> employees = employeeDAO.findAll();
        Map<String, Integer> buckets = new java.util.LinkedHashMap<>();
        buckets.put("Below 50k", 0);
        buckets.put("50k - 75k", 0);
        buckets.put("75k - 100k", 0);
        buckets.put("Above 100k", 0);

        for (Employee e : employees) {
            BigDecimal salary = e.getBasicSalary();
            String bucket;
            if (salary.compareTo(new BigDecimal("50000")) < 0) bucket = "Below 50k";
            else if (salary.compareTo(new BigDecimal("75000")) < 0) bucket = "50k - 75k";
            else if (salary.compareTo(new BigDecimal("100000")) < 0) bucket = "75k - 100k";
            else bucket = "Above 100k";
            buckets.merge(bucket, 1, Integer::sum);
        }
        return buckets;
    }

    public Map<String, BigDecimal> monthlyPayrollTrend(int months) {
        return payrollDAO.monthlyPayrollTrend(months);
    }
}
