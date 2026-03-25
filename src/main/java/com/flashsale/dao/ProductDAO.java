package com.flashsale.dao;

import com.flashsale.entity.Product;
import com.flashsale.utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DAO (Data Access Object) cho bảng Products.
 *
 * Theo SRS mục IV:
 * - Dùng PreparedStatement cho tất cả thao tác CRUD (chống SQL Injection, tối ưu hiệu năng)
 * - Không viết SQL trực tiếp trong class chứa logic nghiệp vụ
 *
 * Cung cấp các phương thức:
 * - CRUD cơ bản: create, findById, findAll, update, delete
 * - findByIdForUpdate: khóa dòng để chống overselling trong flash sale
 * - updateStock: cập nhật riêng tồn kho
 */
public class ProductDAO {
    private final DatabaseConnectionManager connectionManager;

    public ProductDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Thêm sản phẩm mới vào database.
     * Trả về product_id được tự động tạo (AUTO_INCREMENT).
     */
    public int create(Product product) throws SQLException {
        String sql = "INSERT INTO Products(product_name, price, stock, category_id) VALUES(?, ?, ?, ?)";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, product.getProductName());
            statement.setBigDecimal(2, product.getPrice());
            statement.setInt(3, product.getStock());
            statement.setInt(4, product.getCategoryId());
            statement.executeUpdate();

            // Lấy khóa chính được tự động tạo
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không thể tạo sản phẩm, không nhận được khóa chính.");
        }
    }

    /**
     * Tìm sản phẩm theo ID (tự mở và đóng connection).
     * Dùng cho các truy vấn độc lập, không nằm trong transaction.
     */
    public Product findById(int productId) throws SQLException {
        try (Connection connection = connectionManager.getConnection()) {
            return findById(connection, productId);
        }
    }

    /**
     * Tìm sản phẩm theo ID (dùng connection có sẵn).
     * Dùng khi đã có connection trong một transaction.
     */
    public Product findById(Connection connection, int productId) throws SQLException {
        String sql = "SELECT product_id, product_name, price, stock, category_id FROM Products WHERE product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * Tìm sản phẩm theo ID VÀ khóa dòng đó lại (SELECT ... FOR UPDATE).
     *
     * Theo SRS mục IV (Transaction Management - Nâng cao):
     * FOR UPDATE sẽ khóa dòng trong database, ngăn thread khác đọc/sửa
     * cùng dòng cho đến khi transaction hiện tại commit hoặc rollback.
     * → Đây là cách chống "Lost Update" khi nhiều thread cùng mua 1 sản phẩm.
     *
     * @param connection Kết nối đang trong transaction (autoCommit = false)
     * @param productId  ID sản phẩm cần khóa
     * @return Sản phẩm tìm được, hoặc null nếu không tồn tại
     */
    public Product findByIdForUpdate(Connection connection, int productId) throws SQLException {
        String sql = "SELECT product_id, product_name, price, stock, category_id FROM Products WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * Lấy danh sách tất cả sản phẩm, sắp xếp theo product_id.
     */
    public List<Product> findAll() throws SQLException {
        String sql = "SELECT product_id, product_name, price, stock, category_id FROM Products ORDER BY product_id";
        List<Product> products = new ArrayList<>();
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }
        return products;
    }

    /**
     * Cập nhật toàn bộ thông tin sản phẩm.
     * @return true nếu cập nhật thành công (có dòng bị ảnh hưởng)
     */
    public boolean update(Product product) throws SQLException {
        String sql = "UPDATE Products SET product_name = ?, price = ?, stock = ?, category_id = ? WHERE product_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getProductName());
            statement.setBigDecimal(2, product.getPrice());
            statement.setInt(3, product.getStock());
            statement.setInt(4, product.getCategoryId());
            statement.setInt(5, product.getProductId());
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Cập nhật chỉ số lượng tồn kho của sản phẩm (tự mở connection).
     * Dùng cho các thao tác ngoài transaction, ví dụ: reset tồn kho trước stress test.
     */
    public boolean updateStock(int productId, int stock) throws SQLException {
        String sql = "UPDATE Products SET stock = ? WHERE product_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stock);
            statement.setInt(2, productId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Cập nhật tồn kho bằng connection có sẵn (dùng trong transaction).
     * Được gọi bởi OrderService.placeOrder() để trừ kho trong 1 transaction.
     */
    public boolean updateStock(Connection connection, int productId, int stock) throws SQLException {
        String sql = "UPDATE Products SET stock = ? WHERE product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stock);
            statement.setInt(2, productId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Xóa sản phẩm theo ID.
     * @return true nếu xóa thành công
     */
    public boolean delete(int productId) throws SQLException {
        String sql = "DELETE FROM Products WHERE product_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Chuyển đổi 1 dòng ResultSet thành đối tượng Product.
     */
    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getBigDecimal("price"),
                rs.getInt("stock"),
                rs.getInt("category_id"));
    }
}
