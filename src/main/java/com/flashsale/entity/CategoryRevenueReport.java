package com.flashsale.entity;

import java.math.BigDecimal;

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