package com.flashsale.utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {
    
    private static class Helper {
        private static final DatabaseConnectionManager INSTANCE = new DatabaseConnectionManager();
    }

    private DatabaseConnectionManager() {
    }

    // Method trả về instance duy nhất
    public static DatabaseConnectionManager getInstance() {
        return Helper.INSTANCE;
    }


    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(DBConfig.URL, DBConfig.USER, DBConfig.PASSWORD);
        } catch (Exception e) {
            // Log gọn, không lộ password
            System.err.println("Cannot connect to database: " + DBConfig.URL);
            // Ném exception lên trên để xử lý đúng cách
            throw e;
        }
    }
}
