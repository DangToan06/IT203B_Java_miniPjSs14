package com.flashsale.entity;

import java.math.BigDecimal;

public class Product {
    private int productId;
    private String productName;
    private BigDecimal price;
    private int stock;
    private int categoryId;

    public Product() {
    }

    public Product(int productId, String productName, BigDecimal price, int stock, int categoryId) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}