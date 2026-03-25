package com.flashsale.entity;

import java.math.BigDecimal;

/**
 * Lớp thực thể (Entity) đại diện cho bảng Order_Details trong database.
 *
 * Tương ứng với cấu trúc bảng:
 * - order_detail_id: khóa chính, tự tăng (AUTO_INCREMENT)
 * - order_id: khóa ngoại liên kết đến bảng Orders
 * - product_id: khóa ngoại liên kết đến bảng Products
 * - quantity: số lượng mua (phải > 0 theo CHECK constraint)
 * - price: giá tại thời điểm mua
 *
 * Một đơn hàng (Order) có thể có NHIỀU chi tiết (Order_Details),
 * mỗi chi tiết tương ứng với 1 sản phẩm được mua trong đơn hàng đó.
 */
public class OrderDetail {
    private int orderId;
    private int productId;
    private int quantity;
    private BigDecimal price;

    public OrderDetail() {
    }

    public OrderDetail(int orderId, int productId, int quantity, BigDecimal price) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
