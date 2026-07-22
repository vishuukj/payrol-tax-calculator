package com.hrms.payroll.service;

import com.hrms.payroll.dao.EmployeeDAO;
import com.hrms.payroll.dao.SalaryHistoryDAO;
import com.hrms.payroll.dao.impl.ActivityLogDAO;
import com.hrms.payroll.model.Employee;
import com.hrms.payroll.model.SalaryHistory;
import com.hrms.payroll.utils.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business layer sitting between the UI and EmployeeDAO. Every write path
 * validates input first (fail fast, keep bad data out of the database) and
 * writes an audit trail / activity log entry.
 */
public class EmployeeService {

    private final EmployeeDAO employeeDAO;
    private final SalaryHistoryDAO salaryHistoryDAO;
    private final ActivityLogDAO activityLogDAO;

    public EmployeeService(EmployeeDAO employeeDAO, SalaryHistoryDAO salaryHistoryDAO, ActivityLogDAO activityLogDAO) {
        this.employeeDAO = employeeDAO;
        this.salaryHistoryDAO = salaryHistoryDAO;
        this.activityLogDAO = activityLogDAO;
    }

    public Employee addEmployee(Employee emp, Integer actingUserId) {
        validate(emp);
        emp.setEmployeeCode(employeeDAO.generateNextEmployeeCode());
        Employee saved = employeeDAO.save(emp);
        activityLogDAO.log(actingUserId, "Added new employee " + saved.getFullName() + " (" + saved.getEmployeeCode() + ")",
                "EMPLOYEE", saved.getEmployeeId());
        return saved;
    }

    public boolean updateEmployee(Employee emp, Integer actingUserId, String changeReason) {
        validate(emp);
        Employee existing = employeeDAO.findById(emp.getEmployeeId())
                .orElseThrow(() -> new ValidationException("Employee not found."));

        boolean salaryChanged = existing.getBasicSalary().compareTo(emp.getBasicSalary()) != 0
                || existing.getBonus().compareTo(emp.getBonus()) != 0;

        boolean updated = employeeDAO.update(emp);

        if (updated && salaryChanged) {
            SalaryHistory history = new SalaryHistory();
            history.setEmployeeId(emp.getEmployeeId());
            history.setOldBasicSalary(existing.getBasicSalary());
            history.setNewBasicSalary(emp.getBasicSalary());
            history.setOldBonus(existing.getBonus());
            history.setNewBonus(emp.getBonus());
            history.setChangedBy(actingUserId);
            history.setChangeReason(changeReason == null || changeReason.isBlank() ? "Salary revision" : changeReason);
            salaryHistoryDAO.record(history);
        }

        activityLogDAO.log(actingUserId, "Updated employee " + emp.getFullName() + " (" + emp.getEmployeeCode() + ")",
                "EMPLOYEE", emp.getEmployeeId());
        return updated;
    }

    public boolean deleteEmployee(int employeeId, Integer actingUserId) {
        Employee emp = employeeDAO.findById(employeeId).orElse(null);
        boolean deleted = employeeDAO.deleteById(employeeId);
        if (deleted && emp != null) {
            activityLogDAO.log(actingUserId, "Removed employee " + emp.getFullName() + " (" + emp.getEmployeeCode() + ")",
                    "EMPLOYEE", employeeId);
        }
        return deleted;
    }

    public Optional<Employee> findById(int id) {
        return employeeDAO.findById(id);
    }

    public List<Employee> findAll() {
        return employeeDAO.findAll();
    }

    public List<Employee> search(String keyword) {
        return ValidationUtils.isNotBlank(keyword) ? employeeDAO.search(keyword.trim()) : employeeDAO.findAll();
    }

    public List<Employee> filterByDepartment(int departmentId) {
        return employeeDAO.findByDepartment(departmentId);
    }

    public List<Employee> filterBySalaryRange(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new ValidationException("Minimum salary cannot exceed maximum salary.");
        }
        return employeeDAO.findBySalaryRange(min, max);
    }

    public List<Employee> filterByJoiningDate(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ValidationException("'From' date cannot be after 'To' date.");
        }
        return employeeDAO.findByJoiningDateRange(from, to);
    }

    public List<Employee> sorted(String column, String direction) {
        return employeeDAO.findAllSorted(column, direction);
    }

    public int countActive() {
        return employeeDAO.countActiveEmployees();
    }

    // ------------------------------------------------------------------
    private void validate(Employee emp) {
        if (!ValidationUtils.isNotBlank(emp.getFirstName())) throw new ValidationException("First name is required.");
        if (!ValidationUtils.isNotBlank(emp.getLastName())) throw new ValidationException("Last name is required.");
        if (emp.getGender() == null) throw new ValidationException("Gender is required.");
        if (!ValidationUtils.isValidEmail(emp.getEmail())) throw new ValidationException("A valid email address is required.");
        if (!ValidationUtils.isValidPhone(emp.getPhone())) throw new ValidationException("A valid phone number is required.");
        if (emp.getDepartmentId() <= 0) throw new ValidationException("Please select a department.");
        if (!ValidationUtils.isNotBlank(emp.getDesignation())) throw new ValidationException("Designation is required.");
        if (!ValidationUtils.isNotFutureDate(emp.getJoiningDate())) throw new ValidationException("Joining date cannot be in the future.");
        if (!ValidationUtils.isPositive(emp.getBasicSalary())) throw new ValidationException("Basic salary must be greater than zero.");
        if (!ValidationUtils.isNonNegative(emp.getBonus())) throw new ValidationException("Bonus cannot be negative.");
        if (!ValidationUtils.isNonNegative(emp.getAllowances())) throw new ValidationException("Allowances cannot be negative.");
        if (!ValidationUtils.isValidPercent(emp.getPfPercent())) throw new ValidationException("PF percent must be between 0 and 100.");
        if (!ValidationUtils.isNonNegative(emp.getOtherDeductions())) throw new ValidationException("Other deductions cannot be negative.");
    }
}
