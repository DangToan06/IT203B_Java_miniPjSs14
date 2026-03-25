package com.flashsale.utils;

import com.flashsale.config.DBConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Lớp quản lý kết nối cơ sở dữ liệu - áp dụng Singleton Pattern.
 *
 * Theo SRS mục IV (Bonus): Xây dựng class DatabaseConnectionManager
 * sử dụng Singleton Pattern để quản lý kết nối.
 *
 * Singleton Pattern đảm bảo:
 * - Chỉ có DUY NHẤT 1 instance của lớp này trong toàn bộ ứng dụng
 * - Tất cả DAO đều dùng chung instance này để lấy connection
 * - JDBC Driver chỉ được load 1 lần duy nhất
 *
 * Lưu ý: Mỗi lần gọi getConnection() sẽ tạo connection MỚI.
 * Connection được đóng bởi người gọi (dùng try-with-resources).
 */
public class DatabaseConnectionManager {
    // Instance duy nhất (Singleton)
    private static DatabaseConnectionManager instance;

    /**
     * Constructor private: chỉ được gọi từ bên trong lớp.
     * Load JDBC Driver 1 lần khi tạo instance.
     */
    private DatabaseConnectionManager() {
        try {
            Class.forName(DBConfig.DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Không thể load JDBC driver: " + DBConfig.DRIVER, e);
        }
    }

    /**
     * Lấy instance duy nhất (Singleton).
     * Dùng synchronized để đảm bảo thread-safe khi nhiều thread cùng gọi.
     */
    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    /**
     * Tạo và trả về 1 connection mới đến database.
     * Người gọi phải tự đóng connection sau khi sử dụng (dùng try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DBConfig.URL, DBConfig.USER, DBConfig.PASSWORD);
    }
}
