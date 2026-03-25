package com.flashsale.dao;

import com.flashsale.entity.OrderDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class OrderDetailDAO {

    public void batchInsertOrderDetails(Connection connection, int orderId, List<OrderDetail> details)
            throws SQLException {
        String sql = "INSERT INTO Order_Details(order_id, product_id, quantity, price) VALUES(?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (OrderDetail detail : details) {
                statement.setInt(1, orderId);
                statement.setInt(2, detail.getProductId());
                statement.setInt(3, detail.getQuantity());
                statement.setBigDecimal(4, detail.getPrice());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
