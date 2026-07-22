package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Records and retrieves the "Recent Activity" feed shown on the dashboard.
 * Kept simple (no full CRUD interface needed) since entries are append-only.
 */
public class ActivityLogDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public void log(Integer userId, String action, String entityType, Integer entityId) {
        String sql = "INSERT INTO activity_log (user_id, action, entity_type, entity_id) VALUES (?,?,?,?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId); else ps.setNull(1, java.sql.Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, entityType);
            if (entityId != null) ps.setInt(4, entityId); else ps.setNull(4, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Activity logging is best-effort; never let it block the primary business action.
            System.err.println("Warning: failed to write activity log: " + e.getMessage());
        }
    }

    public List<String> recent(int limit) {
        String sql = "SELECT action, created_at FROM activity_log ORDER BY created_at DESC LIMIT ?";
        List<String> entries = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(rs.getTimestamp("created_at") + "  —  " + rs.getString("action"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load activity log", e);
        }
        return entries;
    }
}
