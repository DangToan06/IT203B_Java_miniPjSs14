package com.flashsale.entity;

/**
 * Lớp thực thể (Entity) đại diện cho bảng Categories trong database.
 *
 * Tương ứng với cấu trúc bảng:
 * - category_id: khóa chính, tự tăng (AUTO_INCREMENT)
 * - category_name: tên danh mục sản phẩm
 *
 * Được dùng để phân loại sản phẩm (Products.category_id → Categories.category_id).
 */
public class Category {
    private int id;
    private String name;

    public Category() {}

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
