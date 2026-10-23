package payroll;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public final class PayrollRepository {
    private final String url;

    public PayrollRepository(Path path) throws SQLException {
        url = "jdbc:sqlite:" + path.toAbsolutePath();
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payroll_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_name TEXT NOT NULL, monthly_salary REAL NOT NULL,
                    bonus REAL NOT NULL, gross_pay REAL NOT NULL, tax_rate REAL NOT NULL,
                    tax_amount REAL NOT NULL, net_pay REAL NOT NULL, total_pay REAL NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }
    }

    Connection connect() throws SQLException {
        var connection = DriverManager.getConnection(url);
        try (var statement = connection.createStatement()) { statement.execute("PRAGMA busy_timeout = 5000"); }
        catch (SQLException exception) { connection.close(); throw exception; }
        return connection;
    }

    public long save(Payroll.Breakdown record) throws SQLException {
        try (var connection = connect(); var statement = connection.prepareStatement("""
            INSERT INTO payroll_records (employee_name, monthly_salary, bonus, gross_pay,
                tax_rate, tax_amount, net_pay, total_pay) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, record.employee_name());
            statement.setDouble(2, record.monthly_salary());
            statement.setDouble(3, record.bonus());
            statement.setDouble(4, record.gross_pay());
            statement.setDouble(5, record.tax_rate());
            statement.setDouble(6, record.tax_amount());
            statement.setDouble(7, record.net_pay());
            statement.setDouble(8, record.total_pay());
            statement.executeUpdate();
            try (var query = connection.createStatement(); var result = query.executeQuery("SELECT last_insert_rowid()")) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    public boolean update(long id, Payroll.Breakdown record) throws SQLException {
        try (var connection = connect(); var statement = connection.prepareStatement("""
            UPDATE payroll_records SET employee_name=?, monthly_salary=?, bonus=?, gross_pay=?,
                tax_rate=?, tax_amount=?, net_pay=?, total_pay=? WHERE id=?
            """)) {
            statement.setString(1, record.employee_name());
            statement.setDouble(2, record.monthly_salary());
            statement.setDouble(3, record.bonus());
            statement.setDouble(4, record.gross_pay());
            statement.setDouble(5, record.tax_rate());
            statement.setDouble(6, record.tax_amount());
            statement.setDouble(7, record.net_pay());
            statement.setDouble(8, record.total_pay());
            statement.setLong(9, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (var connection = connect(); var statement = connection.prepareStatement("DELETE FROM payroll_records WHERE id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Map<String, Object>> findAll() throws SQLException {
        var records = new ArrayList<Map<String, Object>>();
        try (var connection = connect(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT * FROM payroll_records ORDER BY id DESC")) {
            var metadata = result.getMetaData();
            while (result.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) record.put(metadata.getColumnName(i), result.getObject(i));
                records.add(record);
            }
        }
        return records;
    }
}
