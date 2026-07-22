package com.hrms.payroll.dao;

import com.hrms.payroll.model.SalaryHistory;

import java.util.List;

public interface SalaryHistoryDAO {
    void record(SalaryHistory history);
    List<SalaryHistory> findByEmployee(int employeeId);
}
