package com.flashsale.dao;

import com.flashsale.entity.User;
import com.flashsale.utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DAO cho bảng Users (người dùng).
 *
 * Cung cấp các thao tác CRUD: thêm, tìm, sửa, xóa người dùng.
 * Tất cả đều dùng PreparedStatement để chống SQL Injection (theo SRS mục IV).
 */
public class UserDAO {
    private final DatabaseConnectionManager connectionManager;

    public UserDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Thêm người dùng mới.
     * @return user_id được tự động tạo
     */
    public int create(User user) throws SQLException {
        String sql = "INSERT INTO Users(username, email) VALUES(?, ?)";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.executeUpdate();

            // Lấy user_id vừa được AUTO_INCREMENT tạo ra
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không thể tạo người dùng, không nhận được khóa chính.");
        }
    }

    /**
     * Tìm người dùng theo ID.
     * @return đối tượng User, hoặc null nếu không tìm thấy
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, email FROM Users WHERE user_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * Lấy danh sách tất cả người dùng, sắp xếp theo user_id.
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT user_id, username, email FROM Users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    /**
     * Cập nhật thông tin người dùng.
     * @return true nếu cập nhật thành công
     */
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE Users SET username = ?, email = ? WHERE user_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setInt(3, user.getUserId());
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Xóa người dùng theo ID.
     * @return true nếu xóa thành công
     */
    public boolean delete(int userId) throws SQLException {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Chuyển đổi 1 dòng ResultSet thành đối tượng User.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("email"));
    }
}
