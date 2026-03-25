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

/**
 * Lớp xử lý nghiệp vụ đặt hàng Flash Sale.
 *
 * Chịu trách nhiệm:
 * - Đặt hàng (placeOrder): kiểm tra tồn kho → trừ kho → tạo đơn hàng → tạo chi tiết đơn hàng
 * - Lấy báo cáo top người mua và doanh thu theo danh mục (gọi Stored Procedure qua DAO)
 *
 * Theo SRS: Quá trình đặt hàng phải là một khối nguyên khối (Atomic).
 * Nếu xảy ra lỗi ở bất kỳ bước nào, tồn kho phải được hoàn trả (rollback).
 */
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

    /**
     * Xử lý đặt hàng Flash Sale.
     *
     * Luồng xử lý:
     * 1. Tắt autoCommit để bắt đầu Transaction (theo SRS: setAutoCommit(false))
     * 2. Đặt mức cô lập SERIALIZABLE để chống Lost Update khi nhiều thread cùng mua
     * 3. Với mỗi sản phẩm: khóa dòng (FOR UPDATE) → kiểm tra tồn kho → trừ kho
     * 4. Tạo đơn hàng (Orders) và chi tiết đơn hàng (Order_Details)
     * 5. Nếu thành công → commit(). Nếu lỗi → rollback() hoàn trả tồn kho.
     *
     * @param userId             ID người dùng đặt hàng
     * @param productIdToQuantity Map chứa (mã sản phẩm → số lượng muốn mua)
     * @return true nếu đặt hàng thành công, false nếu thất bại (hết hàng hoặc lỗi)
     */
    public boolean placeOrder(int userId, Map<Integer, Integer> productIdToQuantity) {
        // Kiểm tra đầu vào: nếu không có sản phẩm nào thì không xử lý
        if (productIdToQuantity == null || productIdToQuantity.isEmpty()) {
            return false;
        }

        Connection connection = null;
        int oldIsolation = Connection.TRANSACTION_REPEATABLE_READ; // Giá trị mặc định

        try {
            // === BƯỚC 1: Mở kết nối và bắt đầu Transaction ===
            connection = connectionManager.getConnection();
            oldIsolation = connection.getTransactionIsolation();

            // Tắt auto-commit để kiểm soát Transaction thủ công (theo SRS mục IV)
            connection.setAutoCommit(false);

            // Đặt mức cô lập SERIALIZABLE:
            // Đảm bảo khi 2 thread cùng đọc tồn kho, không xảy ra "Lost Update"
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            // === BƯỚC 2: Kiểm tra tồn kho và trừ kho cho từng sản phẩm ===
            BigDecimal tongTien = BigDecimal.ZERO;
            List<OrderDetail> danhSachChiTiet = new ArrayList<>();

            for (Map.Entry<Integer, Integer> item : productIdToQuantity.entrySet()) {
                int productId = item.getKey();
                int soLuongMua = item.getValue();

                // Kiểm tra số lượng hợp lệ
                if (soLuongMua <= 0) {
                    throw new IllegalArgumentException("Số lượng phải > 0");
                }

                // Khóa dòng sản phẩm bằng SELECT ... FOR UPDATE
                // Tránh thread khác đọc/sửa cùng lúc → chống overselling
                Product sanPham = productDAO.findByIdForUpdate(connection, productId);
                if (sanPham == null) {
                    throw new IllegalStateException("Sản phẩm không tồn tại: " + productId);
                }

                // Kiểm tra tồn kho: nếu không đủ → ném lỗi → rollback
                if (sanPham.getStock() < soLuongMua) {
                    throw new IllegalStateException("Hết hàng cho sản phẩm: " + productId);
                }

                // Trừ tồn kho
                int conLai = sanPham.getStock() - soLuongMua;
                productDAO.updateStock(connection, productId, conLai);

                // Tính tiền và thêm vào danh sách chi tiết đơn hàng
                BigDecimal thanhTien = sanPham.getPrice().multiply(BigDecimal.valueOf(soLuongMua));
                tongTien = tongTien.add(thanhTien);
                danhSachChiTiet.add(new OrderDetail(0, productId, soLuongMua, sanPham.getPrice()));
            }

            // === BƯỚC 3: Tạo đơn hàng và chi tiết đơn hàng ===
            int orderId = orderDAO.createOrder(connection, userId, tongTien);

            // Dùng Batch Processing để insert nhiều chi tiết đơn hàng cùng lúc (theo SRS mục IV)
            orderDetailDAO.batchInsertOrderDetails(connection, orderId, danhSachChiTiet);

            // === BƯỚC 4: Xác nhận giao dịch thành công ===
            connection.commit();
            return true;

        } catch (Exception ex) {
            // Nếu xảy ra BẤT KỲ lỗi nào → rollback toàn bộ (theo SRS: tính Atomic)
            // Tồn kho sẽ được hoàn trả về trạng thái ban đầu
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // Không thể rollback - bỏ qua lỗi phụ
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
                    // Lỗi khi đóng kết nối - bỏ qua
                }
            }
        }
    }

    /**
     * Lấy danh sách top 5 người mua nhiều nhất.
     * Gọi Stored Procedure SP_GetTopBuyers qua CallableStatement (theo SRS mục IV).
     */
    public List<TopBuyerReport> getTopBuyers() throws SQLException {
        return orderDAO.getTopBuyers();
    }

    /**
     * Lấy thống kê doanh thu theo danh mục sản phẩm.
     * Gọi Stored Procedure SP_GetRevenueByCategory qua CallableStatement (theo SRS mục IV).
     */
    public List<CategoryRevenueReport> getRevenueByCategory() throws SQLException {
        return orderDAO.getRevenueByCategory();
    }
}
