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
        // Kiểm tra đầu vào
        if (productIdToQuantity == null || productIdToQuantity.isEmpty()) {
            return false;
        }

        Connection connection = null;
        int oldIsolation = Connection.TRANSACTION_REPEATABLE_READ;

        try {
            connection = connectionManager.getConnection();
            oldIsolation = connection.getTransactionIsolation();

            // Tắt auto-commit để kiểm soát Transaction thủ công (theo SRS mục IV)
            connection.setAutoCommit(false);

            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            // Kiểm tra tồn kho và trừ kho cho từng sản phẩm
            BigDecimal tongTien = BigDecimal.ZERO;
            List<OrderDetail> danhSachChiTiet = new ArrayList<>();

            for (Map.Entry<Integer, Integer> item : productIdToQuantity.entrySet()) {
                int productId = item.getKey();
                int soLuongMua = item.getValue();

                if (soLuongMua <= 0) {
                    throw new IllegalArgumentException("Số lượng phải > 0");
                }

                // Khóa dòng sản phẩm bằng SELECT ... FOR UPDATE
                // Tránh thread khác đọc/sửa cùng lúc
                Product sanPham = productDAO.findByIdForUpdate(connection, productId);
                if (sanPham == null) {
                    throw new IllegalStateException("Sản phẩm không tồn tại: " + productId);
                }

                // Kiểm tra tồn kho
                if (sanPham.getStock() < soLuongMua) {
                    throw new IllegalStateException("Hết hàng cho sản phẩm: " + productId);
                }


                int conLai = sanPham.getStock() - soLuongMua;
                productDAO.updateStock(connection, productId, conLai);

                // Tính tiền và thêm vào danh sách chi tiết đơn hàng
                BigDecimal thanhTien = sanPham.getPrice().multiply(BigDecimal.valueOf(soLuongMua));
                tongTien = tongTien.add(thanhTien);
                danhSachChiTiet.add(new OrderDetail(0, productId, soLuongMua, sanPham.getPrice()));
            }

            // Tạo đơn hàng và chi tiết đơn hàng
            int orderId = orderDAO.createOrder(connection, userId, tongTien);

            // Batch Processing để insert nhiều chi tiết đơn hàng cùng lúc (theo SRS mục IV)
            orderDetailDAO.batchInsertOrderDetails(connection, orderId, danhSachChiTiet);

            connection.commit();
            return true;

        } catch (Exception ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }
            return false;

        } finally {
            // Dọn dẹp: khôi phục trạng thái kết nối và đóng
            if (connection != null) {
                try {
                    connection.setTransactionIsolation(oldIsolation);
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Gọi Stored Procedure SP_GetTopBuyers qua CallableStatement (theo SRS mục IV).
     */
    public List<TopBuyerReport> getTopBuyers() throws SQLException {
        return orderDAO.getTopBuyers();
    }

    /**
     * Gọi Stored Procedure SP_GetRevenueByCategory qua CallableStatement (theo SRS mục IV).
     */
    public List<CategoryRevenueReport> getRevenueByCategory() throws SQLException {
        return orderDAO.getRevenueByCategory();
    }
}