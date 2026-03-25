package com.flashsale.dao;

import com.flashsale.entity.OrderDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Lớp DAO cho bảng Order_Details (chi tiết đơn hàng).
 *
 * Theo SRS mục IV: Phải dùng Batch Processing (addBatch/executeBatch)
 * khi cần insert nhiều chi tiết đơn hàng cùng một lúc.
 * → Tối ưu hiệu năng bằng cách gửi nhiều câu SQL qua 1 lần gọi database.
 */
public class OrderDetailDAO {

    /**
     * Insert nhiều chi tiết đơn hàng bằng Batch Processing.
     *
     * Thay vì gọi executeUpdate() N lần (1 lần/dòng), ta dùng:
     * - addBatch(): thêm câu lệnh vào hàng đợi
     * - executeBatch(): thực thi tất cả cùng lúc → nhanh hơn nhiều
     *
     * Nhận connection từ bên ngoài vì chạy trong cùng transaction
     * với tạo đơn hàng và trừ tồn kho.
     *
     * @param connection Kết nối đang trong transaction
     * @param orderId    ID đơn hàng vừa tạo
     * @param details    Danh sách chi tiết đơn hàng cần insert
     */
    public void batchInsertOrderDetails(Connection connection, int orderId, List<OrderDetail> details)
            throws SQLException {
        String sql = "INSERT INTO Order_Details(order_id, product_id, quantity, price) VALUES(?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (OrderDetail detail : details) {
                statement.setInt(1, orderId);
                statement.setInt(2, detail.getProductId());
                statement.setInt(3, detail.getQuantity());
                statement.setBigDecimal(4, detail.getPrice());
                statement.addBatch(); // Thêm vào hàng đợi, chưa thực thi
            }
            statement.executeBatch(); // Thực thi TẤT CẢ câu lệnh cùng một lúc
        }
    }
}
