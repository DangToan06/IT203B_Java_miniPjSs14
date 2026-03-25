package com.flashsale.entity;

import java.math.BigDecimal;

/**
 * Lớp chứa kết quả báo cáo - Doanh thu theo danh mục sản phẩm.
 *
 * Được trả về từ Stored Procedure SP_GetRevenueByCategory.
 * Tính tổng doanh thu = SUM(quantity * price) trong Order_Details cho mỗi danh mục.
 */
public class CategoryRevenueReport {
    private final String categoryName;  // Tên danh mục
    private final BigDecimal revenue;   // Tổng doanh thu

    public CategoryRevenueReport(String categoryName, BigDecimal revenue) {
        this.categoryName = categoryName;
        this.revenue = revenue;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
