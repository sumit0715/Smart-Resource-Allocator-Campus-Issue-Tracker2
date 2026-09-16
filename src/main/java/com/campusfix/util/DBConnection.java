package com.campusfix.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Hands out a JDBC connection to the campusfix MySQL database.
 * Point it at your own server by setting the CAMPUSFIX_DB_* environment variables,
 * or just edit the fallback values below.
 */
public final class DBConnection {

    private static final String URL =
            System.getenv().getOrDefault("CAMPUSFIX_DB_URL",
                    "jdbc:mysql://localhost:3306/campusfix?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static final String DB_USER =
            System.getenv().getOrDefault("CAMPUSFIX_DB_USER", "root");
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("CAMPUSFIX_DB_PASSWORD", "root");

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on the classpath.", e);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }
}
