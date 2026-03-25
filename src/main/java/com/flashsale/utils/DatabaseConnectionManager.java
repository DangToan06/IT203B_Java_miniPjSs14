package com.flashsale.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.flashsale.config.DBConfig;


public class DatabaseConnectionManager {
    // Instance duy nhất (Singleton)
    private static DatabaseConnectionManager instance;

    
    private DatabaseConnectionManager() {
        try {
            Class.forName(DBConfig.DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Không thể load JDBC driver: " + DBConfig.DRIVER, e);
        }
    }

    
    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DBConfig.URL, DBConfig.USER, DBConfig.PASSWORD);
    }
}