package com.flashsale.dao;

import com.flashsale.entity.Category;
import com.flashsale.utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DAO cho bảng Categories (danh mục sản phẩm).
 *
 * Cung cấp các thao tác CRUD: thêm, tìm, sửa, xóa danh mục.
 * Dùng PreparedStatement cho tất cả thao tác để chống SQL Injection (theo SRS mục IV).
 */
public class CategoryDAO {
    private final DatabaseConnectionManager connectionManager;

    public CategoryDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Thêm danh mục mới.
     * @return category_id được tự động tạo
     */
    public int create(Category category) throws SQLException {
        String sql = "INSERT INTO Categories(category_name) VALUES(?)";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            statement.executeUpdate();

            // Lấy category_id vừa được AUTO_INCREMENT tạo ra
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không thể tạo danh mục, không nhận được khóa chính.");
        }
    }

    /**
     * Tìm danh mục theo ID.
     * @return đối tượng Category, hoặc null nếu không tìm thấy
     */
    public Category findById(int categoryId) throws SQLException {
        String sql = "SELECT category_id, category_name FROM Categories WHERE category_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * Lấy danh sách tất cả danh mục, sắp xếp theo category_id.
     */
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT category_id, category_name FROM Categories ORDER BY category_id";
        List<Category> categories = new ArrayList<>();
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
        }
        return categories;
    }

    /**
     * Cập nhật tên danh mục.
     * @return true nếu cập nhật thành công
     */
    public boolean update(Category category) throws SQLException {
        String sql = "UPDATE Categories SET category_name = ? WHERE category_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setInt(2, category.getId());
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Xóa danh mục theo ID.
     * @return true nếu xóa thành công
     */
    public boolean delete(int categoryId) throws SQLException {
        String sql = "DELETE FROM Categories WHERE category_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Chuyển đổi 1 dòng ResultSet thành đối tượng Category.
     */
    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("category_id"), rs.getString("category_name"));
    }
}
