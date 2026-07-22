package com.hrms.payroll.controller;

import com.hrms.payroll.model.Employee;
import com.hrms.payroll.service.EmployeeService;
import com.hrms.payroll.service.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public Employee addEmployee(Employee emp, Integer actingUserId) throws ValidationException {
        return employeeService.addEmployee(emp, actingUserId);
    }

    public boolean updateEmployee(Employee emp, Integer actingUserId, String reason) throws ValidationException {
        return employeeService.updateEmployee(emp, actingUserId, reason);
    }

    public boolean deleteEmployee(int employeeId, Integer actingUserId) {
        return employeeService.deleteEmployee(employeeId, actingUserId);
    }

    public List<Employee> listAll() {
        return employeeService.findAll();
    }

    public List<Employee> search(String keyword) {
        return employeeService.search(keyword);
    }

    public List<Employee> filterByDepartment(int departmentId) {
        return employeeService.filterByDepartment(departmentId);
    }

    public List<Employee> filterBySalaryRange(BigDecimal min, BigDecimal max) {
        return employeeService.filterBySalaryRange(min, max);
    }

    public List<Employee> filterByJoiningDate(LocalDate from, LocalDate to) {
        return employeeService.filterByJoiningDate(from, to);
    }

    public List<Employee> sorted(String column, String direction) {
        return employeeService.sorted(column, direction);
    }
}
