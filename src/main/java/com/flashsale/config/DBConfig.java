package com.flashsale.config;

public class DBConfig {
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    public static final String URL = System.getenv().getOrDefault(
            "FLASHSALE_DB_URL",
            "jdbc:mysql://localhost:3306/flash_sale_db?createDatabaseIfNotExist=true&serverTimezone=UTC");

    public static final String USER = System.getenv().getOrDefault("FLASHSALE_DB_USER", "root");

    public static final String PASSWORD = System.getenv().getOrDefault("FLASHSALE_DB_PASSWORD", "123456");


}
