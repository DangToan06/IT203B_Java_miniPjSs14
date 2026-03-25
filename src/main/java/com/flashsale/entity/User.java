package com.flashsale.entity;

/**
 * Lớp thực thể (Entity) đại diện cho bảng Users trong database.
 *
 * Tương ứng với cấu trúc bảng:
 * - user_id: khóa chính, tự tăng (AUTO_INCREMENT)
 * - username: tên người dùng (UNIQUE)
 * - email: địa chỉ email (UNIQUE)
 */
public class User {
    private int userId;
    private String username;
    private String email;

    public User() {
    }

    public User(int userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
