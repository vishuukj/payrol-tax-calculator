package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.dao.SalaryHistoryDAO;
import com.hrms.payroll.database.DatabaseConnection;
import com.hrms.payroll.model.SalaryHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalaryHistoryDAOImpl implements SalaryHistoryDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public void record(SalaryHistory h) {
        String sql = "INSERT INTO salary_history " +
                "(employee_id, old_basic_salary, new_basic_salary, old_bonus, new_bonus, changed_by, change_reason) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, h.getEmployeeId());
            ps.setBigDecimal(2, h.getOldBasicSalary());
            ps.setBigDecimal(3, h.getNewBasicSalary());
            ps.setBigDecimal(4, h.getOldBonus());
            ps.setBigDecimal(5, h.getNewBonus());
            if (h.getChangedBy() != null) ps.setInt(6, h.getChangedBy()); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, h.getChangeReason());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record salary history", e);
        }
    }

    @Override
    public List<SalaryHistory> findByEmployee(int employeeId) {
        String sql = "SELECT * FROM salary_history WHERE employee_id = ? ORDER BY changed_at DESC";
        List<SalaryHistory> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SalaryHistory h = new SalaryHistory();
                    h.setHistoryId(rs.getInt("history_id"));
                    h.setEmployeeId(rs.getInt("employee_id"));
                    h.setOldBasicSalary(rs.getBigDecimal("old_basic_salary"));
                    h.setNewBasicSalary(rs.getBigDecimal("new_basic_salary"));
                    h.setOldBonus(rs.getBigDecimal("old_bonus"));
                    h.setNewBonus(rs.getBigDecimal("new_bonus"));
                    h.setChangeReason(rs.getString("change_reason"));
                    Timestamp ts = rs.getTimestamp("changed_at");
                    if (ts != null) h.setChangedAt(ts.toLocalDateTime());
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch salary history", e);
        }
        return list;
    }
}
