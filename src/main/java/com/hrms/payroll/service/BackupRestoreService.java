package com.hrms.payroll.service;

import com.hrms.payroll.database.DatabaseConnection;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lightweight backup/restore for the "Backup Database" / "Restore Database"
 * settings screen. Rather than shelling out to mysqldump (which may not be
 * on the PATH on every machine), this walks each table over JDBC and writes
 * plain INSERT statements, which restore() replays inside a single
 * transaction. This keeps backup/restore fully portable and dependency-free.
 */
public class BackupRestoreService {

    // Order matters: children before parents on restore is handled by disabling FK checks.
    private static final List<String> TABLES = List.of(
            "departments", "users", "tax_slabs", "employees", "payroll", "salary_history", "activity_log"
    );

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public File backup(String directory) throws IOException, SQLException {
        String filename = "payroll_backup_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(java.time.LocalDateTime.now()) + ".sql";
        File file = new File(directory, filename);

        try (Connection conn = db.getConnection();
             BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            writer.write("-- Payroll & Tax Calculator database backup\n");
            writer.write("-- Generated: " + java.time.LocalDateTime.now() + "\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            for (String table : TABLES) {
                dumpTable(conn, table, writer);
            }

            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
        }
        return file;
    }

    private void dumpTable(Connection conn, String table, BufferedWriter writer) throws SQLException, IOException {
        writer.write("-- Table: " + table + "\n");
        writer.write("DELETE FROM " + table + ";\n");

        String sql = "SELECT * FROM " + table;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" VALUES (");
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        sb.append("NULL");
                    } else if (value instanceof Number || value instanceof Boolean) {
                        sb.append(value);
                    } else {
                        sb.append("'").append(value.toString().replace("'", "''")).append("'");
                    }
                    if (i < columnCount) sb.append(", ");
                }
                sb.append(");\n");
                writer.write(sb.toString());
            }
        }
        writer.write("\n");
    }

    /** Executes a previously-generated backup .sql file inside one transaction. Rolls back entirely on any error. */
    public void restore(File sqlFile) throws IOException, SQLException {
        List<String> statements = Files.readAllLines(sqlFile.toPath()).stream()
                .filter(line -> !line.isBlank() && !line.trim().startsWith("--"))
                .toList();

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String line : statements) {
                    st.execute(line);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
