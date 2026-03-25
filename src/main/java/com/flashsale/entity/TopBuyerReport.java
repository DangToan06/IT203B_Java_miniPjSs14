package com.flashsale.entity;

/**
 * Lớp chứa kết quả báo cáo - Top người mua hàng nhiều nhất.
 *
 * Được trả về từ Stored Procedure SP_GetTopBuyers.
 * Dùng để hiển thị top 5 khách hàng mua nhiều nhất trong đợt Flash Sale.
 */
public class TopBuyerReport {
    private final int userId;       // ID người dùng
    private final String username;   // Tên người dùng
    private final int totalItems;    // Tổng số sản phẩm đã mua

    public TopBuyerReport(int userId, String username, int totalItems) {
        this.userId = userId;
        this.username = username;
        this.totalItems = totalItems;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getTotalItems() {
        return totalItems;
    }
}
