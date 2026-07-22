package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.dao.PayrollDAO;
import com.hrms.payroll.database.DatabaseConnection;
import com.hrms.payroll.model.Payroll;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class PayrollDAOImpl implements PayrollDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String BASE_SELECT =
            "SELECT p.*, e.employee_code, CONCAT(e.first_name,' ',e.last_name) AS employee_name " +
            "FROM payroll p JOIN employees e ON p.employee_id = e.employee_id ";

    @Override
    public Payroll save(Payroll p) {
        String sql = "INSERT INTO payroll " +
                "(employee_id, pay_period, gross_salary, pf_deduction, income_tax, other_deductions, net_salary, generated_by) " +
                "VALUES (?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE gross_salary=VALUES(gross_salary), pf_deduction=VALUES(pf_deduction), " +
                "income_tax=VALUES(income_tax), other_deductions=VALUES(other_deductions), net_salary=VALUES(net_salary), " +
                "generated_on=NOW()";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getEmployeeId());
            ps.setString(2, p.getPayPeriod());
            ps.setBigDecimal(3, p.getGrossSalary());
            ps.setBigDecimal(4, p.getPfDeduction());
            ps.setBigDecimal(5, p.getIncomeTax());
            ps.setBigDecimal(6, p.getOtherDeductions());
            ps.setBigDecimal(7, p.getNetSalary());
            ps.setObject(8, null); // generated_by set by service layer if a logged-in user id is available
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setPayrollId(keys.getInt(1));
            }
            return p;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payroll record: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Payroll> findById(Integer id) {
        String sql = BASE_SELECT + "WHERE p.payroll_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch payroll " + id, e);
        }
    }

    @Override
    public List<Payroll> findAll() {
        return runQuery(BASE_SELECT + "ORDER BY p.generated_on DESC", ps -> {});
    }

    @Override
    public boolean update(Payroll p) {
        String sql = "UPDATE payroll SET gross_salary=?, pf_deduction=?, income_tax=?, other_deductions=?, net_salary=? " +
                "WHERE payroll_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, p.getGrossSalary());
            ps.setBigDecimal(2, p.getPfDeduction());
            ps.setBigDecimal(3, p.getIncomeTax());
            ps.setBigDecimal(4, p.getOtherDeductions());
            ps.setBigDecimal(5, p.getNetSalary());
            ps.setInt(6, p.getPayrollId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update payroll", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM payroll WHERE payroll_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete payroll", e);
        }
    }

    @Override
    public List<Payroll> findByEmployee(int employeeId) {
        return runQuery(BASE_SELECT + "WHERE p.employee_id = ? ORDER BY p.pay_period DESC",
                ps -> ps.setInt(1, employeeId));
    }

    @Override
    public Optional<Payroll> findByEmployeeAndPeriod(int employeeId, String payPeriod) {
        String sql = BASE_SELECT + "WHERE p.employee_id = ? AND p.pay_period = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, payPeriod);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch payroll for period", e);
        }
    }

    @Override
    public List<Payroll> findByPeriod(String payPeriod) {
        return runQuery(BASE_SELECT + "WHERE p.pay_period = ? ORDER BY e.first_name",
                ps -> ps.setString(1, payPeriod));
    }

    @Override
    public BigDecimal totalPayrollForPeriod(String payPeriod) {
        return scalarQuery("SELECT COALESCE(SUM(net_salary),0) FROM payroll WHERE pay_period = ?",
                ps -> ps.setString(1, payPeriod));
    }

    @Override
    public BigDecimal totalTaxCollected() {
        return scalarQuery("SELECT COALESCE(SUM(income_tax),0) FROM payroll", ps -> {});
    }

    @Override
    public BigDecimal averageSalary() {
        return scalarQuery("SELECT COALESCE(AVG(basic_salary),0) FROM employees WHERE is_active = TRUE", ps -> {});
    }

    @Override
    public BigDecimal highestSalary() {
        return scalarQuery("SELECT COALESCE(MAX(basic_salary),0) FROM employees WHERE is_active = TRUE", ps -> {});
    }

    @Override
    public BigDecimal lowestSalary() {
        return scalarQuery("SELECT COALESCE(MIN(basic_salary),0) FROM employees WHERE is_active = TRUE", ps -> {});
    }

    @Override
    public Map<String, BigDecimal> payrollByDepartment() {
        String sql = "SELECT d.department_name, COALESCE(SUM(e.basic_salary + e.bonus + e.allowances),0) AS total " +
                "FROM departments d LEFT JOIN employees e ON d.department_id = e.department_id AND e.is_active = TRUE " +
                "GROUP BY d.department_name ORDER BY d.department_name";
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getBigDecimal(2));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to compute department payroll", e);
        }
        return result;
    }

    @Override
    public Map<String, BigDecimal> monthlyPayrollTrend(int months) {
        String sql = "SELECT pay_period, SUM(net_salary) FROM payroll " +
                "GROUP BY pay_period ORDER BY pay_period DESC LIMIT ?";
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getBigDecimal(2));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to compute payroll trend", e);
        }
        // Reverse so chart reads chronologically left-to-right
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(result.entrySet());
        Collections.reverse(entries);
        Map<String, BigDecimal> chronological = new LinkedHashMap<>();
        entries.forEach(e -> chronological.put(e.getKey(), e.getValue()));
        return chronological;
    }

    // ----------------------------------------------------------------
    private Payroll mapRow(ResultSet rs) throws SQLException {
        Payroll p = new Payroll();
        p.setPayrollId(rs.getInt("payroll_id"));
        p.setEmployeeId(rs.getInt("employee_id"));
        p.setEmployeeCode(rs.getString("employee_code"));
        p.setEmployeeName(rs.getString("employee_name"));
        p.setPayPeriod(rs.getString("pay_period"));
        p.setGrossSalary(rs.getBigDecimal("gross_salary"));
        p.setPfDeduction(rs.getBigDecimal("pf_deduction"));
        p.setIncomeTax(rs.getBigDecimal("income_tax"));
        p.setOtherDeductions(rs.getBigDecimal("other_deductions"));
        p.setNetSalary(rs.getBigDecimal("net_salary"));
        Timestamp gen = rs.getTimestamp("generated_on");
        if (gen != null) p.setGeneratedOn(gen.toLocalDateTime());
        return p;
    }

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Payroll> runQuery(String sql, Binder binder) {
        List<Payroll> results = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Payroll query failed: " + e.getMessage(), e);
        }
        return results;
    }

    private BigDecimal scalarQuery(String sql, Binder binder) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Aggregate query failed: " + e.getMessage(), e);
        }
    }
}
