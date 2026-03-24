package com.flashsale.config;

public class DBConfig {
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    // URL kết nối database (đọc từ biến môi trường, nếu không có thì dùng giá trị mặc định)
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
