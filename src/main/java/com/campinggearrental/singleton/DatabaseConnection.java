package com.campinggearrental.singleton;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    private static class InstanceHolder {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    }

    public static DatabaseConnection getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        String url = setting("camping.db.url", "CAMPING_DB_URL",
                "jdbc:mysql://localhost:3306/camping_gear_rental?serverTimezone=UTC");
        String username = setting("camping.db.username", "CAMPING_DB_USERNAME", "root");
        String password = setting("camping.db.password", "CAMPING_DB_PASSWORD", "");
        return DriverManager.getConnection(url, username, password);
    }

    private String setting(String property, String environmentVariable, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}