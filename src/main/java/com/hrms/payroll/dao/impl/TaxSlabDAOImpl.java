package com.hrms.payroll.dao.impl;

import com.hrms.payroll.dao.DataAccessException;
import com.hrms.payroll.dao.TaxSlabDAO;
import com.hrms.payroll.database.DatabaseConnection;
import com.hrms.payroll.model.TaxSlab;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaxSlabDAOImpl implements TaxSlabDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public TaxSlab save(TaxSlab slab) {
        String sql = "INSERT INTO tax_slabs (slab_name, min_amount, max_amount, tax_percent) VALUES (?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, slab.getSlabName());
            ps.setBigDecimal(2, slab.getMinAmount());
            if (slab.getMaxAmount() != null) ps.setBigDecimal(3, slab.getMaxAmount());
            else ps.setNull(3, Types.DECIMAL);
            ps.setBigDecimal(4, slab.getTaxPercent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) slab.setSlabId(keys.getInt(1));
            }
            return slab;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save tax slab: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TaxSlab> findById(Integer id) {
        String sql = "SELECT * FROM tax_slabs WHERE slab_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch tax slab", e);
        }
    }

    @Override
    public List<TaxSlab> findAll() {
        return query("SELECT * FROM tax_slabs ORDER BY min_amount");
    }

    @Override
    public List<TaxSlab> findAllActiveOrdered() {
        return query("SELECT * FROM tax_slabs WHERE is_active = TRUE ORDER BY min_amount");
    }

    @Override
    public void replaceAll(List<TaxSlab> slabs) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement del = conn.createStatement()) {
                del.executeUpdate("DELETE FROM tax_slabs");
            }
            String insertSql = "INSERT INTO tax_slabs (slab_name, min_amount, max_amount, tax_percent) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (TaxSlab slab : slabs) {
                    ps.setString(1, slab.getSlabName());
                    ps.setBigDecimal(2, slab.getMinAmount());
                    if (slab.getMaxAmount() != null) ps.setBigDecimal(3, slab.getMaxAmount());
                    else ps.setNull(3, Types.DECIMAL);
                    ps.setBigDecimal(4, slab.getTaxPercent());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save tax slabs: " + e.getMessage(), e);
        }
    }

    private List<TaxSlab> query(String sql) {
        List<TaxSlab> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list tax slabs", e);
        }
        return list;
    }

    @Override
    public boolean update(TaxSlab slab) {
        String sql = "UPDATE tax_slabs SET slab_name=?, min_amount=?, max_amount=?, tax_percent=?, is_active=? WHERE slab_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slab.getSlabName());
            ps.setBigDecimal(2, slab.getMinAmount());
            if (slab.getMaxAmount() != null) ps.setBigDecimal(3, slab.getMaxAmount());
            else ps.setNull(3, Types.DECIMAL);
            ps.setBigDecimal(4, slab.getTaxPercent());
            ps.setBoolean(5, slab.isActive());
            ps.setInt(6, slab.getSlabId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update tax slab", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "UPDATE tax_slabs SET is_active = FALSE WHERE slab_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to deactivate tax slab", e);
        }
    }

    private TaxSlab mapRow(ResultSet rs) throws SQLException {
        TaxSlab s = new TaxSlab();
        s.setSlabId(rs.getInt("slab_id"));
        s.setSlabName(rs.getString("slab_name"));
        s.setMinAmount(rs.getBigDecimal("min_amount"));
        s.setMaxAmount(rs.getBigDecimal("max_amount")); // may be null
        s.setTaxPercent(rs.getBigDecimal("tax_percent"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }
}
