CREATE DATABASE IF NOT EXISTS flash_sale_db;
USE flash_sale_db;

DROP PROCEDURE IF EXISTS SP_GetTopBuyers;
DROP PROCEDURE IF EXISTS SP_GetRevenueByCategory;

DROP TABLE IF EXISTS Order_Details;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS Categories;
DROP TABLE IF EXISTS Users;

CREATE TABLE Users (
                       user_id INT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Categories (
                            category_id INT PRIMARY KEY AUTO_INCREMENT,
                            category_name VARCHAR(100) NOT NULL
);

CREATE TABLE Products (
                          product_id INT PRIMARY KEY AUTO_INCREMENT,
                          product_name VARCHAR(150) NOT NULL,
                          price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
                          stock INT NOT NULL CHECK (stock >= 0),
                          category_id INT,
                          CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);
CREATE INDEX idx_product_stock ON Products(stock);

CREATE TABLE Orders (
                        order_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
                        CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE INDEX idx_order_user ON Orders(user_id);

CREATE TABLE Order_Details (
                               order_detail_id INT PRIMARY KEY AUTO_INCREMENT,
                               order_id INT NOT NULL,
                               product_id INT NOT NULL,
                               quantity INT NOT NULL CHECK (quantity > 0),
                               price DECIMAL(10, 2) NOT NULL,
                               CONSTRAINT fk_orderdetail_order FOREIGN KEY (order_id) REFERENCES Orders(order_id),
                               CONSTRAINT fk_orderdetail_product FOREIGN KEY (product_id) REFERENCES Products(product_id)
);

CREATE INDEX idx_orderdetail_order ON Order_Details(order_id);

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


INSERT INTO Users(username, email) VALUES
    ('Alice', 'alice@gmail.com'),
    ('Bob', 'bob@gmail.com'),
    ('Charlie', 'charlie@gmail.com');

INSERT INTO Categories(category_name) VALUES
                                          ('Electronics'),
                                          ('Fashion');

INSERT INTO Products(product_name, price, stock, category_id) VALUES
    ('iPhone', 1000.00, 10, 1),
    ('T-Shirt', 20.00, 50, 2),
    ('Headphone', 120.00, 30, 1);