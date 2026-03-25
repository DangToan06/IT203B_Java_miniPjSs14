package com.flashsale.dao;

import com.flashsale.entity.CategoryRevenueReport;
import com.flashsale.entity.TopBuyerReport;
import com.flashsale.utils.DatabaseConnectionManager;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private final DatabaseConnectionManager connectionManager;

    public OrderDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    public int createOrder(Connection connection, int userId, BigDecimal totalAmount) throws SQLException {
        String sql = "INSERT INTO Orders(user_id, total_amount) VALUES(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setBigDecimal(2, totalAmount);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không thể tạo đơn hàng, không nhận được khóa chính.");
        }
    }

    public List<TopBuyerReport> getTopBuyers() throws SQLException {
        String sql = "{CALL SP_GetTopBuyers()}";
        List<TopBuyerReport> result = new ArrayList<>();

        try (Connection connection = connectionManager.getConnection();
             CallableStatement callableStatement = connection.prepareCall(sql);
             ResultSet rs = callableStatement.executeQuery()) {
            while (rs.next()) {
                result.add(new TopBuyerReport(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getInt("total_items")));
            }
        }
        return result;
    }

    public List<CategoryRevenueReport> getRevenueByCategory() throws SQLException {
        String sql = "{CALL SP_GetRevenueByCategory()}";
        List<CategoryRevenueReport> result = new ArrayList<>();

        try (Connection connection = connectionManager.getConnection();
             CallableStatement callableStatement = connection.prepareCall(sql);
             ResultSet rs = callableStatement.executeQuery()) {
            while (rs.next()) {
                result.add(new CategoryRevenueReport(
                        rs.getString("category_name"),
                        rs.getBigDecimal("revenue")));
            }
        }
        return result;
    }
}