package com.hrms.payroll.service;

import com.hrms.payroll.dao.*;
import com.hrms.payroll.dao.impl.*;

/**
 * Minimal hand-rolled dependency container. Wires DAO -> Service once at
 * startup and hands out shared singletons. Kept intentionally simple
 * (no Spring) to match the "plain Java / JDBC" brief, but centralizes
 * object construction in one place so nothing else uses `new XyzDAOImpl()`.
 */
public final class AppContext {

    private static AppContext instance;

    public final EmployeeDAO employeeDAO;
    public final UserDAO userDAO;
    public final DepartmentDAO departmentDAO;
    public final PayrollDAO payrollDAO;
    public final TaxSlabDAO taxSlabDAO;
    public final SalaryHistoryDAO salaryHistoryDAO;
    public final ActivityLogDAO activityLogDAO;

    public final AuthService authService;
    public final EmployeeService employeeService;
    public final PayrollService payrollService;
    public final AnalyticsService analyticsService;
    public final TaxCalculationStrategy taxCalculationStrategy;

    private AppContext() {
        employeeDAO = new EmployeeDAOImpl();
        userDAO = new UserDAOImpl();
        departmentDAO = new DepartmentDAOImpl();
        payrollDAO = new PayrollDAOImpl();
        taxSlabDAO = new TaxSlabDAOImpl();
        salaryHistoryDAO = new SalaryHistoryDAOImpl();
        activityLogDAO = new ActivityLogDAO();

        taxCalculationStrategy = new SlabBasedTaxCalculator(taxSlabDAO);
        authService = new AuthService(userDAO, activityLogDAO);
        employeeService = new EmployeeService(employeeDAO, salaryHistoryDAO, activityLogDAO);
        payrollService = new PayrollService(employeeDAO, payrollDAO, taxCalculationStrategy, activityLogDAO);
        analyticsService = new AnalyticsService(employeeDAO, payrollDAO);
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }
}
