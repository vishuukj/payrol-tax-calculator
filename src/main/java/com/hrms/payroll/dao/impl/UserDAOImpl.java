package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.dao.UserDAO;
import com.hrms.payroll.database.DatabaseConnection;
import com.hrms.payroll.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, email) VALUES (?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setUserId(keys.getInt(1));
            }
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        return findByColumn("user_id", id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findByColumn("username", username);
    }

    private Optional<User> findByColumn(String column, Object value) {
        String sql = "SELECT * FROM users WHERE " + column + " = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch user", e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list users", e);
        }
        return users;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET full_name=?, role=?, email=?, is_active=? WHERE user_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getRole().name());
            ps.setString(3, user.getEmail());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getUserId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "UPDATE users SET is_active = FALSE WHERE user_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to deactivate user", e);
        }
    }

    @Override
    public void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update last login", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setEmail(rs.getString("email"));
        u.setActive(rs.getBoolean("is_active"));
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) u.setLastLogin(lastLogin.toLocalDateTime());
        return u;
    }
}
