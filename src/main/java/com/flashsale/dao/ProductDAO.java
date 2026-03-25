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

public class ProductDAO {
    private final DatabaseConnectionManager connectionManager;

    public ProductDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    public int create(Product product) throws SQLException {
        String sql = "INSERT INTO Products(product_name, price, stock, category_id) VALUES(?, ?, ?, ?)";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, product.getProductName());
            statement.setBigDecimal(2, product.getPrice());
            statement.setInt(3, product.getStock());
            statement.setInt(4, product.getCategoryId());
            statement.executeUpdate();


            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không thể tạo sản phẩm, không nhận được khóa chính.");
        }
    }

    public Product findById(int productId) throws SQLException {
        try (Connection connection = connectionManager.getConnection()) {
            return findById(connection, productId);
        }
    }

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

    public boolean updateStock(int productId, int stock) throws SQLException {
        String sql = "UPDATE Products SET stock = ? WHERE product_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stock);
            statement.setInt(2, productId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateStock(Connection connection, int productId, int stock) throws SQLException {
        String sql = "UPDATE Products SET stock = ? WHERE product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stock);
            statement.setInt(2, productId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int productId) throws SQLException {
        String sql = "DELETE FROM Products WHERE product_id = ?";
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            return statement.executeUpdate() > 0;
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getBigDecimal("price"),
                rs.getInt("stock"),
                rs.getInt("category_id"));
    }
}
