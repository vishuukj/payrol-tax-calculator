package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.dao.EmployeeDAO;
import com.hrms.payroll.database.DatabaseConnection;
import com.hrms.payroll.model.Employee;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JDBC implementation of EmployeeDAO.
 * Every query uses PreparedStatement to prevent SQL injection.
 */
public class EmployeeDAOImpl implements EmployeeDAO {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "employee_code", "first_name", "last_name", "basic_salary", "joining_date", "designation"
    );

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String BASE_SELECT =
            "SELECT e.*, d.department_name FROM employees e " +
            "JOIN departments d ON e.department_id = d.department_id ";

    @Override
    public Employee save(Employee emp) {
        String sql = "INSERT INTO employees " +
                "(employee_code, first_name, last_name, gender, email, phone, department_id, designation, " +
                " joining_date, basic_salary, bonus, allowances, pf_percent, other_deductions) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindEmployee(ps, emp);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    emp.setEmployeeId(keys.getInt(1));
                }
            }
            return emp;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> findById(Integer id) {
        String sql = BASE_SELECT + "WHERE e.employee_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch employee " + id, e);
        }
    }

    @Override
    public List<Employee> findAll() {
        String sql = BASE_SELECT + "WHERE e.is_active = TRUE ORDER BY e.employee_id DESC";
        return runQuery(sql, ps -> {});
    }

    @Override
    public boolean update(Employee emp) {
        String sql = "UPDATE employees SET first_name=?, last_name=?, gender=?, email=?, phone=?, " +
                "department_id=?, designation=?, joining_date=?, basic_salary=?, bonus=?, allowances=?, " +
                "pf_percent=?, other_deductions=? WHERE employee_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getGender().name());
            ps.setString(4, emp.getEmail());
            ps.setString(5, emp.getPhone());
            ps.setInt(6, emp.getDepartmentId());
            ps.setString(7, emp.getDesignation());
            ps.setDate(8, Date.valueOf(emp.getJoiningDate()));
            ps.setBigDecimal(9, emp.getBasicSalary());
            ps.setBigDecimal(10, emp.getBonus());
            ps.setBigDecimal(11, emp.getAllowances());
            ps.setBigDecimal(12, emp.getPfPercent());
            ps.setBigDecimal(13, emp.getOtherDeductions());
            ps.setInt(14, emp.getEmployeeId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update employee " + emp.getEmployeeId(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        // Soft delete keeps payroll history intact (referential integrity + auditability).
        String sql = "UPDATE employees SET is_active = FALSE WHERE employee_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete employee " + id, e);
        }
    }

    @Override
    public List<Employee> search(String keyword) {
        String sql = BASE_SELECT + "WHERE e.is_active = TRUE AND (" +
                "e.employee_code LIKE ? OR e.first_name LIKE ? OR e.last_name LIKE ? " +
                "OR d.department_name LIKE ? OR e.designation LIKE ? OR e.email LIKE ?) " +
                "ORDER BY e.employee_id DESC";
        String like = "%" + keyword + "%";
        return runQuery(sql, ps -> {
            for (int i = 1; i <= 6; i++) ps.setString(i, like);
        });
    }

    @Override
    public List<Employee> findByDepartment(int departmentId) {
        String sql = BASE_SELECT + "WHERE e.is_active = TRUE AND e.department_id = ? ORDER BY e.first_name";
        return runQuery(sql, ps -> ps.setInt(1, departmentId));
    }

    @Override
    public List<Employee> findBySalaryRange(BigDecimal min, BigDecimal max) {
        String sql = BASE_SELECT + "WHERE e.is_active = TRUE AND e.basic_salary BETWEEN ? AND ? " +
                "ORDER BY e.basic_salary DESC";
        return runQuery(sql, ps -> {
            ps.setBigDecimal(1, min);
            ps.setBigDecimal(2, max);
        });
    }

    @Override
    public List<Employee> findByJoiningDateRange(LocalDate from, LocalDate to) {
        String sql = BASE_SELECT + "WHERE e.is_active = TRUE AND e.joining_date BETWEEN ? AND ? " +
                "ORDER BY e.joining_date DESC";
        return runQuery(sql, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    @Override
    public List<Employee> findAllSorted(String sortColumn, String direction) {
        String col = ALLOWED_SORT_COLUMNS.contains(sortColumn) ? sortColumn : "employee_code";
        String dir = "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        // Column/direction are whitelisted above, never taken raw from user input, so string
        // concatenation here is safe (JDBC cannot parameterize identifiers).
        String sql = BASE_SELECT + "WHERE e.is_active = TRUE ORDER BY e." + col + " " + dir;
        return runQuery(sql, ps -> {});
    }

    @Override
    public int countActiveEmployees() {
        String sql = "SELECT COUNT(*) FROM employees WHERE is_active = TRUE";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count employees", e);
        }
    }

    @Override
    public String generateNextEmployeeCode() {
        String sql = "SELECT employee_code FROM employees ORDER BY employee_id DESC LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int next = 1;
            if (rs.next()) {
                String last = rs.getString(1); // e.g. EMP-0005
                String digits = last.replaceAll("\\D", "");
                next = Integer.parseInt(digits) + 1;
            }
            return String.format("EMP-%04d", next);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to generate employee code", e);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void bindEmployee(PreparedStatement ps, Employee emp) throws SQLException {
        ps.setString(1, emp.getEmployeeCode());
        ps.setString(2, emp.getFirstName());
        ps.setString(3, emp.getLastName());
        ps.setString(4, emp.getGender().name());
        ps.setString(5, emp.getEmail());
        ps.setString(6, emp.getPhone());
        ps.setInt(7, emp.getDepartmentId());
        ps.setString(8, emp.getDesignation());
        ps.setDate(9, Date.valueOf(emp.getJoiningDate()));
        ps.setBigDecimal(10, emp.getBasicSalary());
        ps.setBigDecimal(11, emp.getBonus());
        ps.setBigDecimal(12, emp.getAllowances());
        ps.setBigDecimal(13, emp.getPfPercent());
        ps.setBigDecimal(14, emp.getOtherDeductions());
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getInt("employee_id"));
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setFirstName(rs.getString("first_name"));
        e.setLastName(rs.getString("last_name"));
        e.setGender(Employee.Gender.valueOf(rs.getString("gender")));
        e.setEmail(rs.getString("email"));
        e.setPhone(rs.getString("phone"));
        e.setDepartmentId(rs.getInt("department_id"));
        e.setDepartmentName(rs.getString("department_name"));
        e.setDesignation(rs.getString("designation"));
        e.setJoiningDate(rs.getDate("joining_date").toLocalDate());
        e.setBasicSalary(rs.getBigDecimal("basic_salary"));
        e.setBonus(rs.getBigDecimal("bonus"));
        e.setAllowances(rs.getBigDecimal("allowances"));
        e.setPfPercent(rs.getBigDecimal("pf_percent"));
        e.setOtherDeductions(rs.getBigDecimal("other_deductions"));
        e.setActive(rs.getBoolean("is_active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) e.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) e.setUpdatedAt(updated.toLocalDateTime());
        return e;
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Employee> runQuery(String sql, Binder binder) {
        List<Employee> results = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + e.getMessage(), e);
        }
        return results;
    }
}
