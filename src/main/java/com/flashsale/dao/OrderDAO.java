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

/**
 * Lớp DAO cho bảng Orders.
 *
 * Cung cấp:
 * - createOrder: tạo đơn hàng mới (dùng PreparedStatement trong transaction)
 * - getTopBuyers: gọi Stored Procedure SP_GetTopBuyers (dùng CallableStatement)
 * - getRevenueByCategory: gọi Stored Procedure SP_GetRevenueByCategory (dùng CallableStatement)
 *
 * Theo SRS mục IV:
 * - Phần báo cáo PHẢI dùng Stored Procedure, không tính toán bằng Java
 * - Dùng CallableStatement để gọi Stored Procedure
 */
public class OrderDAO {
    private final DatabaseConnectionManager connectionManager;

    public OrderDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Tạo đơn hàng mới trong database.
     *
     * Nhận connection từ bên ngoài vì hàm này chạy trong transaction
     * (cùng transaction với trừ kho và tạo chi tiết đơn hàng).
     *
     * @param connection  Kết nối đang trong transaction
     * @param userId      ID người dùng đặt hàng
     * @param totalAmount Tổng tiền đơn hàng
     * @return order_id được tự động tạo
     */
    public int createOrder(Connection connection, int userId, BigDecimal totalAmount) throws SQLException {
        String sql = "INSERT INTO Orders(user_id, total_amount) VALUES(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setBigDecimal(2, totalAmount);
            statement.executeUpdate();

            // Lấy order_id vừa được AUTO_INCREMENT tạo ra
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không thể tạo đơn hàng, không nhận được khóa chính.");
        }
    }

    /**
     * Lấy top 5 người mua nhiều hàng nhất.
     *
     * Theo SRS mục IV: Dùng CallableStatement để gọi Stored Procedure SP_GetTopBuyers.
     * Procedure này chạy ở phía database, không tính toán bằng Java.
     *
     * @return Danh sách TopBuyerReport (userId, username, tổng sản phẩm đã mua)
     */
    public List<TopBuyerReport> getTopBuyers() throws SQLException {
        // Cú pháp gọi Stored Procedure: {CALL tên_procedure()}
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

    /**
     * Thống kê doanh thu theo từng danh mục sản phẩm.
     *
     * Theo SRS mục IV: Dùng CallableStatement để gọi Stored Procedure SP_GetRevenueByCategory.
     * Procedure tính tổng (quantity * price) trong Order_Details cho từng danh mục.
     *
     * @return Danh sách CategoryRevenueReport (tên danh mục, doanh thu)
     */
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
