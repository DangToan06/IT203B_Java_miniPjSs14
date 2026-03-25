package com.flashsale.entity;

public class TopBuyerReport {
    private final int userId;
    private final String username;
    private final int totalItems;
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
