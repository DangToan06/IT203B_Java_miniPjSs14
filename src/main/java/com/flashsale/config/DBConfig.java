package com.flashsale.config;

/**
 * Lớp chứa cấu hình kết nối cơ sở dữ liệu.
 *
 * Các giá trị mặc định kết nối đến MySQL local:
 * - URL: jdbc:mysql://localhost:3306/flash_sale_db
 * - User: root / Password: 123456
 *
 * Có thể ghi đè bằng biến môi trường:
 * - FLASHSALE_DB_URL, FLASHSALE_DB_USER, FLASHSALE_DB_PASSWORD
 *
 * Constructor private → không cho phép tạo instance (chỉ dùng hằng số static).
 */
public class DBConfig {
    // Driver JDBC cho MySQL
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // URL kết nối database (đọc từ biến môi trường, nếu không có thì dùng giá trị
    // mặc định)
    // createDatabaseIfNotExist=true    → Tự động tạo database nếu chưa có
    // serverTimezone=UTC             → Thiết lập múi giờ để tránh lỗi timezone
    public static final String URL = System.getenv().getOrDefault(
            "FLASHSALE_DB_URL",
            "jdbc:mysql://localhost:3306/flash_sale_db?createDatabaseIfNotExist=true&serverTimezone=UTC");

    // Tên đăng nhập database
    public static final String USER = System.getenv().getOrDefault("FLASHSALE_DB_USER", "root");

    // Mật khẩu database
    public static final String PASSWORD = System.getenv().getOrDefault("FLASHSALE_DB_PASSWORD", "123456");

    // Constructor private: không cho phép tạo instance
    private DBConfig() {
    }
}
