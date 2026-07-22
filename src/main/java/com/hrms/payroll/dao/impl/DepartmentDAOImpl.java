package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.dao.DepartmentDAO;
import com.hrms.payroll.database.DatabaseConnection;
import com.hrms.payroll.model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartmentDAOImpl implements DepartmentDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Department save(Department d) {
        String sql = "INSERT INTO departments (department_name, department_head) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getDepartmentName());
            ps.setString(2, d.getDepartmentHead());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setDepartmentId(keys.getInt(1));
            }
            return d;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save department: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Department> findById(Integer id) {
        String sql = "SELECT * FROM departments WHERE department_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch department " + id, e);
        }
    }

    @Override
    public List<Department> findAll() {
        String sql = "SELECT * FROM departments ORDER BY department_name";
        List<Department> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list departments", e);
        }
        return list;
    }

    @Override
    public boolean update(Department d) {
        String sql = "UPDATE departments SET department_name = ?, department_head = ? WHERE department_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getDepartmentName());
            ps.setString(2, d.getDepartmentHead());
            ps.setInt(3, d.getDepartmentId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update department", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM departments WHERE department_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to delete department. It may still have employees assigned to it.", e);
        }
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
                rs.getInt("department_id"),
                rs.getString("department_name"),
                rs.getString("department_head")
        );
    }
}
