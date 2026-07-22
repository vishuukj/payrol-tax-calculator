package com.hrms.payroll.dao;

import com.hrms.payroll.model.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EmployeeDAO extends CrudDAO<Employee, Integer> {

    /** Free-text search across code, name, department, designation. */
    List<Employee> search(String keyword);

    List<Employee> findByDepartment(int departmentId);

    List<Employee> findBySalaryRange(BigDecimal min, BigDecimal max);

    List<Employee> findByJoiningDateRange(LocalDate from, LocalDate to);

    /** Sorted results, e.g. sortColumn="basic_salary", direction="DESC". */
    List<Employee> findAllSorted(String sortColumn, String direction);

    int countActiveEmployees();

    String generateNextEmployeeCode();
}
