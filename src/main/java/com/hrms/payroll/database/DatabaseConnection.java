package com.hrms.payroll.database;

import com.hrms.payroll.dao.DataAccessException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central connection manager for the whole application.
 * Uses HikariCP for production-grade pooled connections and the
 * Singleton pattern so the whole app shares one pool.
 *
 * Configuration is read from src/main/resources/db.properties, falling back
 * to sensible local-dev defaults if the file is absent.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        Properties props = loadProperties();

        String host = props.getProperty("db.host", "localhost");
        String port = props.getProperty("db.port", "3306");
        String name = props.getProperty("db.name", "payroll_hrms");
        String user = props.getProperty("db.user", "root");
        String password = props.getProperty("db.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("PayrollHikariPool");
        config.setConnectionTimeout(10_000);

        this.dataSource = new HikariDataSource(config);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // Fall through to defaults; do not crash startup over a missing config file.
            System.err.println("Warning: could not load db.properties, using defaults. " + e.getMessage());
        }
        return props;
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to obtain a database connection. " +
                    "Check that MySQL is running and db.properties is correct.", e);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
