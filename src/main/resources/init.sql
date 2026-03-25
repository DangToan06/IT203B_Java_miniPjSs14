-- =====================================================
-- Script khởi tạo cơ sở dữ liệu Flash Sale
-- Tạo các bảng: Users, Categories, Products, Orders, Order_Details
-- Tạo Stored Procedures cho báo cáo
-- Chèn dữ liệu mẫu
-- =====================================================

-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS flash_sale_db;
USE flash_sale_db;

-- Xóa Stored Procedures cũ (nếu có) trước khi tạo lại
DROP PROCEDURE IF EXISTS SP_GetTopBuyers;
DROP PROCEDURE IF EXISTS SP_GetRevenueByCategory;

-- Xóa bảng theo thứ tự: bảng con trước, bảng cha sau (tránh lỗi Foreign Key)
DROP TABLE IF EXISTS Order_Details;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS Categories;
DROP TABLE IF EXISTS Users;

-- =====================================================
-- BẢNG USERS: Lưu thông tin người dùng
-- - username và email phải là duy nhất (UNIQUE)
-- - created_at tự động ghi thời gian tạo
-- =====================================================
CREATE TABLE Users (
                       user_id INT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- BẢNG CATEGORIES: Lưu danh mục sản phẩm
-- - Ví dụ: Electronics, Fashion, ...
-- =====================================================
CREATE TABLE Categories (
                            category_id INT PRIMARY KEY AUTO_INCREMENT,
                            category_name VARCHAR(100) NOT NULL
);

-- =====================================================
-- BẢNG PRODUCTS: Lưu thông tin sản phẩm
-- - price > 0: giá phải dương (CHECK constraint)
-- - stock >= 0: tồn kho không được âm (CHECK constraint)
--   → Đây là ràng buộc quan trọng để chống overselling
-- - category_id: khóa ngoại liên kết đến Categories
-- =====================================================
CREATE TABLE Products (
                          product_id INT PRIMARY KEY AUTO_INCREMENT,
                          product_name VARCHAR(150) NOT NULL,
                          price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
                          stock INT NOT NULL CHECK (stock >= 0),
                          category_id INT,
                          CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);
-- Index trên cột stock để tối ưu truy vấn kiểm tra tồn kho
CREATE INDEX idx_product_stock ON Products(stock);

-- =====================================================
-- BẢNG ORDERS: Lưu đơn hàng
-- - user_id: khóa ngoại liên kết đến Users
-- - order_date: tự động ghi thời gian đặt hàng
-- - total_amount: tổng tiền đơn hàng
-- =====================================================
CREATE TABLE Orders (
                        order_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
                        CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES Users(user_id)
);
-- Index trên user_id để tối ưu truy vấn tìm đơn hàng theo người dùng
CREATE INDEX idx_order_user ON Orders(user_id);

-- =====================================================
-- BẢNG ORDER_DETAILS: Lưu chi tiết đơn hàng
-- - Mỗi dòng = 1 sản phẩm trong đơn hàng
-- - quantity > 0: số lượng mua phải dương
-- - price: giá tại thời điểm mua (có thể khác giá hiện tại)
-- - order_id, product_id: khóa ngoại
-- =====================================================
CREATE TABLE Order_Details (
                               order_detail_id INT PRIMARY KEY AUTO_INCREMENT,
                               order_id INT NOT NULL,
                               product_id INT NOT NULL,
                               quantity INT NOT NULL CHECK (quantity > 0),
                               price DECIMAL(10, 2) NOT NULL,
                               CONSTRAINT fk_orderdetail_order FOREIGN KEY (order_id) REFERENCES Orders(order_id),
                               CONSTRAINT fk_orderdetail_product FOREIGN KEY (product_id) REFERENCES Products(product_id)
);
-- Index trên order_id để tối ưu join với bảng Orders
CREATE INDEX idx_orderdetail_order ON Order_Details(order_id);

-- =====================================================
-- STORED PROCEDURE: SP_GetTopBuyers
-- Lấy top 5 người mua nhiều hàng nhất (theo tổng quantity)
-- Theo SRS: Phải dùng Stored Procedure, không tính bằng Java
-- Được gọi từ Java bằng CallableStatement
-- =====================================================
DELIMITER $$
CREATE PROCEDURE SP_GetTopBuyers()
BEGIN
SELECT u.user_id, u.username, COALESCE(SUM(od.quantity), 0) AS total_items
FROM Users u
         LEFT JOIN Orders o ON u.user_id = o.user_id
         LEFT JOIN Order_Details od ON o.order_id = od.order_id
GROUP BY u.user_id, u.username
ORDER BY total_items DESC
    LIMIT 5;
END$$
DELIMITER ;

-- =====================================================
-- STORED PROCEDURE: SP_GetRevenueByCategory
-- Thống kê tổng doanh thu theo từng danh mục sản phẩm
-- Doanh thu = SUM(số lượng × giá) trong Order_Details
-- Theo SRS: Phải dùng Stored Procedure, không tính bằng Java
-- Được gọi từ Java bằng CallableStatement
-- =====================================================
DELIMITER $$
CREATE PROCEDURE SP_GetRevenueByCategory()
BEGIN
SELECT c.category_name, COALESCE(SUM(od.quantity * od.price), 0) AS revenue
FROM Categories c
         LEFT JOIN Products p ON c.category_id = p.category_id
         LEFT JOIN Order_Details od ON p.product_id = od.product_id
GROUP BY c.category_name
ORDER BY revenue DESC;
END$$
DELIMITER ;


-- 3 người dùng mẫu
INSERT INTO Users(username, email) VALUES
                                       ('Alice', 'alice@gmail.com'),
                                       ('Bob', 'bob@gmail.com'),
                                       ('Charlie', 'charlie@gmail.com');

-- 2 danh mục sản phẩm
INSERT INTO Categories(category_name) VALUES
                                          ('Electronics'),
                                          ('Fashion');

-- 3 sản phẩm mẫu (iPhone có stock=10 để dùng cho stress test)
INSERT INTO Products(product_name, price, stock, category_id) VALUES
                                                                  ('iPhone', 1000.00, 10, 1),
                                                                  ('T-Shirt', 20.00, 50, 2),
                                                                  ('Headphone', 120.00, 30, 1);