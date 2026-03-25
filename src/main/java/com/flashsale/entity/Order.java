package com.flashsale.entity;

import java.math.BigDecimal;

/**
 * Lớp thực thể (Entity) đại diện cho bảng Orders trong database.
 *
 * Tương ứng với cấu trúc bảng:
 * - order_id: khóa chính, tự tăng (AUTO_INCREMENT)
 * - user_id: khóa ngoại liên kết đến bảng Users
 * - total_amount: tổng tiền đơn hàng (DECIMAL)
 *
 * Mỗi đơn hàng có thể chứa nhiều chi tiết (Order_Details).
 */
public class Order {
    private int orderId;
    private int userId;
    private BigDecimal totalAmount;

    public Order() {
    }

    public Order(int orderId, int userId, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
