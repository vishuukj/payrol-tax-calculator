package com.hrms.payroll.dao;

import com.hrms.payroll.model.Payroll;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PayrollDAO extends CrudDAO<Payroll, Integer> {

    List<Payroll> findByEmployee(int employeeId);

    Optional<Payroll> findByEmployeeAndPeriod(int employeeId, String payPeriod);

    List<Payroll> findByPeriod(String payPeriod);

    /** Sum of net salary across all employees for a given pay period ("this month's payroll"). */
    BigDecimal totalPayrollForPeriod(String payPeriod);

    BigDecimal totalTaxCollected();

    BigDecimal averageSalary();

    BigDecimal highestSalary();

    BigDecimal lowestSalary();

    /** department_name -> total gross payroll, for the department-wise bar chart. */
    Map<String, BigDecimal> payrollByDepartment();

    /** pay_period (YYYY-MM) -> total net payroll, for the monthly trend line chart. */
    Map<String, BigDecimal> monthlyPayrollTrend(int months);
}
