package com.flashsale.service;

import com.flashsale.dao.OrderDAO;
import com.flashsale.dao.OrderDetailDAO;
import com.flashsale.dao.ProductDAO;
import com.flashsale.entity.CategoryRevenueReport;
import com.flashsale.entity.OrderDetail;
import com.flashsale.entity.Product;
import com.flashsale.entity.TopBuyerReport;
import com.flashsale.utils.DatabaseConnectionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class OrderService {
    private final DatabaseConnectionManager connectionManager;
    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;
    private final OrderDetailDAO orderDetailDAO;

    public OrderService() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
        this.productDAO = new ProductDAO();
        this.orderDAO = new OrderDAO();
        this.orderDetailDAO = new OrderDetailDAO();
    }

    public boolean placeOrder(int userId, Map<Integer, Integer> productIdToQuantity) {
        // Kiểm tra đầu vào: nếu không có sản phẩm nào thì không xử lý
        if (productIdToQuantity == null || productIdToQuantity.isEmpty()) {
            return false;
        }

        Connection connection = null;
        int oldIsolation = Connection.TRANSACTION_REPEATABLE_READ; // Giá trị mặc định

        try {
            connection = connectionManager.getConnection();
            oldIsolation = connection.getTransactionIsolation();

            connection.setAutoCommit(false);

            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            BigDecimal tongTien = BigDecimal.ZERO;
            List<OrderDetail> danhSachChiTiet = new ArrayList<>();

            for (Map.Entry<Integer, Integer> item : productIdToQuantity.entrySet()) {
                int productId = item.getKey();
                int soLuongMua = item.getValue();

                if (soLuongMua <= 0) {
                    throw new IllegalArgumentException("Số lượng phải > 0");
                }

                Product sanPham = productDAO.findByIdForUpdate(connection, productId);
                if (sanPham == null) {
                    throw new IllegalStateException("Sản phẩm không tồn tại: " + productId);
                }

                if (sanPham.getStock() < soLuongMua) {
                    throw new IllegalStateException("Hết hàng cho sản phẩm: " + productId);
                }

                int conLai = sanPham.getStock() - soLuongMua;
                productDAO.updateStock(connection, productId, conLai);

                BigDecimal thanhTien = sanPham.getPrice().multiply(BigDecimal.valueOf(soLuongMua));
                tongTien = tongTien.add(thanhTien);
                danhSachChiTiet.add(new OrderDetail(0, productId, soLuongMua, sanPham.getPrice()));
            }

            int orderId = orderDAO.createOrder(connection, userId, tongTien);

            orderDetailDAO.batchInsertOrderDetails(connection, orderId, danhSachChiTiet);

            connection.commit();
            return true;

        } catch (Exception ex) {

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    System.out.println(ignored);
                }
            }
            return false;

        } finally {
            if (connection != null) {
                try {
                    connection.setTransactionIsolation(oldIsolation);
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ignored) {
                    System.out.println(ignored);
                }
            }
        }
    }

    public List<TopBuyerReport> getTopBuyers() throws SQLException {
        return orderDAO.getTopBuyers();
    }

    public List<CategoryRevenueReport> getRevenueByCategory() throws SQLException {
        return orderDAO.getRevenueByCategory();
    }
}
