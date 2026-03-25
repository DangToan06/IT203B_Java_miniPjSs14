package com.flashsale.app;

import com.flashsale.dao.ProductDAO;
import com.flashsale.entity.CategoryRevenueReport;
import com.flashsale.entity.Product;
import com.flashsale.entity.TopBuyerReport;
import com.flashsale.service.OrderService;
import com.flashsale.utils.DatabaseConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp chính của ứng dụng Flash Sale E-Commerce Engine.
 *
 * Chương trình thực hiện 3 bước:
 * 1. Khởi tạo cơ sở dữ liệu từ file init.sql (dùng Statement - theo SRS)
 * 2. Chạy luồng đặt hàng cơ bản + in báo cáo
 * 3. Kiểm thử áp lực (stress test): 50 threads cùng mua 1 sản phẩm chỉ có 10
 * cái
 */
public class MainAppTest {
    // Hằng số cấu hình cho demo và stress test
    private static final int DEMO_USER_ID = 1; // ID người dùng dùng để demo
    private static final int STRESS_PRODUCT_ID = 1; // ID sản phẩm dùng cho stress test
    private static final int STRESS_INITIAL_STOCK = 10; // Tồn kho ban đầu (chỉ 10 cái)
    private static final int STRESS_THREAD_COUNT = 50; // Số lượng thread (mô phỏng 50 khách hàng)

    public static void main(String[] args) {
        try {
            // Bước 1: Khởi tạo database (tạo bảng, stored procedure, dữ liệu mẫu)
            initializeDatabase();

            // Bước 2: Chạy luồng đặt hàng cơ bản và in báo cáo
            runBasicFlow(DEMO_USER_ID);

            // Bước 3: Chạy stress test - kiểm tra chống overselling
            runStressTest(DEMO_USER_ID, STRESS_PRODUCT_ID, STRESS_INITIAL_STOCK, STRESS_THREAD_COUNT);
        } catch (Exception e) {
            System.err.println("Lỗi ứng dụng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Khởi tạo cơ sở dữ liệu bằng cách chạy file init.sql.
     *
     * Theo SRS mục IV: Dùng Statement (không phải PreparedStatement) cho tác vụ
     * khởi tạo hệ thống ban đầu như chạy script tạo bảng CREATE TABLE.
     */
    private static void initializeDatabase() throws SQLException, IOException {
        try (Connection connection = DatabaseConnectionManager.getInstance().getConnection();
             Statement statement = connection.createStatement()) {
            // Đọc và thực thi file SQL từ thư mục resources
            executeSqlScript(statement, "/init.sql");
            System.out.println("=== Đã khởi tạo database từ init.sql ===");
        }
    }

    /**
     * Đọc file SQL từ resources và thực thi từng câu lệnh.
     *
     * Hỗ trợ xử lý DELIMITER cho MySQL Stored Procedure.
     * Ví dụ: DELIMITER $$ ... END$$ sẽ được nhận diện đúng.
     *
     * @param statement    Statement dùng để thực thi SQL
     * @param resourcePath Đường dẫn file SQL trong thư mục resources (vd:
     *                     "/init.sql")
     */
    private static void executeSqlScript(Statement statement, String resourcePath) throws IOException, SQLException {
        // Mở file SQL từ resources
        InputStream in = MainAppTest.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Không tìm thấy file SQL: " + resourcePath);
        }

        String delimiter = ";"; // Ký tự kết thúc câu lệnh SQL mặc định
        StringBuilder command = new StringBuilder(); // Bộ đệm chứa câu lệnh đang đọc

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Bỏ qua dòng trống và dòng comment SQL
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }

                // Xử lý lệnh DELIMITER của MySQL
                // Khi gặp "DELIMITER $$" → đổi ký tự kết thúc thành "$$"
                // Khi gặp "DELIMITER ;" → đổi lại thành ";"
                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring("DELIMITER ".length()).trim();
                    continue;
                }

                // Thêm dòng hiện tại vào bộ đệm
                command.append(line).append('\n');
                String current = command.toString().trim();

                // Nếu bộ đệm kết thúc bằng delimiter → thực thi câu lệnh
                if (current.endsWith(delimiter)) {
                    // Cắt bỏ delimiter ở cuối trước khi thực thi
                    String sql = current.substring(0, current.length() - delimiter.length()).trim();
                    if (!sql.isEmpty()) {
                        statement.execute(sql);
                    }
                    // Xoá bộ đệm, chuẩn bị cho câu lệnh tiếp theo
                    command.setLength(0);
                }
            }
        }
    }

    /**
     * Chạy luồng đặt hàng cơ bản:
     * - Đặt mua sản phẩm 1 (1 cái) và sản phẩm 2 (2 cái)
     * - In ra báo cáo top người mua và doanh thu theo danh mục
     */
    private static void runBasicFlow(int userId) throws SQLException {
        OrderService orderService = new OrderService();

        // Tạo đơn hàng mẫu: sản phẩm 1 → 1 cái, sản phẩm 2 → 2 cái
        Map<Integer, Integer> donHangMau = new HashMap<>();
        donHangMau.put(1, 1); // Mua 1 cái iPhone
        donHangMau.put(2, 2); // Mua 2 cái T-Shirt

        // Thực hiện đặt hàng
        boolean datHangThanhCong = orderService.placeOrder(userId, donHangMau);
        System.out.println("Kết quả đặt hàng mẫu: " + (datHangThanhCong ? "THÀNH CÔNG" : "THẤT BẠI"));

        // In báo cáo top 5 khách mua nhiều nhất (gọi Stored Procedure SP_GetTopBuyers)
        System.out.println("\n=== Top người mua hàng nhiều nhất ===");
        for (TopBuyerReport buyer : orderService.getTopBuyers()) {
            System.out.printf("- userId=%d, username=%s, tổng SP đã mua=%d%n",
                    buyer.getUserId(), buyer.getUsername(), buyer.getTotalItems());
        }

        // In báo cáo doanh thu theo danh mục (gọi Stored Procedure
        // SP_GetRevenueByCategory)
        System.out.println("\n=== Doanh thu theo danh mục ===");
        for (CategoryRevenueReport revenue : orderService.getRevenueByCategory()) {
            System.out.printf("- Danh mục=%s, Doanh thu=%s%n", revenue.getCategoryName(), revenue.getRevenue());
        }
    }

    /**
     * Kiểm thử áp lực (Stress Test) - Giai đoạn 5 trong SRS.
     *
     * Tạo 50 threads (đóng vai 50 khách hàng) cùng lúc mua 1 sản phẩm
     * chỉ có tồn kho là 10. Mục tiêu: chứng minh database không bị âm số lượng
     * nhờ Transaction và DB Locks (SELECT ... FOR UPDATE).
     *
     * Sử dụng CountDownLatch để đồng bộ:
     * - ready: đợi tất cả threads sẵn sàng
     * - start: phát tín hiệu để tất cả threads bắt đầu cùng lúc
     * - done: đợi tất cả threads hoàn thành
     */
    private static void runStressTest(int userId, int productId, int initialStock, int threadCount)
            throws SQLException, InterruptedException {
        ProductDAO productDAO = new ProductDAO();
        OrderService orderService = new OrderService();

        // Reset tồn kho về giá trị ban đầu trước khi test
        productDAO.updateStock(productId, initialStock);

        // Tạo 3 CountDownLatch để đồng bộ các threads
        CountDownLatch ready = new CountDownLatch(threadCount); // Đếm threads đã sẵn sàng
        CountDownLatch start = new CountDownLatch(1); // Tín hiệu bắt đầu chung
        CountDownLatch done = new CountDownLatch(threadCount); // Đếm threads đã hoàn thành
        AtomicInteger soLuongThanhCong = new AtomicInteger(0); // Đếm đơn hàng thành công (thread-safe)

        // Tạo và khởi chạy các threads
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                // Báo hiệu thread này đã sẵn sàng
                ready.countDown();
                try {
                    // Đợi tín hiệu bắt đầu → tất cả threads chạy cùng lúc
                    start.await();

                    // Mỗi thread cố gắng mua 1 cái sản phẩm
                    boolean thanhCong = orderService.placeOrder(userId, Map.of(productId, 1));
                    if (thanhCong) {
                        soLuongThanhCong.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Báo hiệu thread này đã hoàn thành
                    done.countDown();
                }
            });
            t.start();
        }

        // Đợi tất cả threads sẵn sàng, rồi phát tín hiệu bắt đầu
        ready.await();
        start.countDown(); // "Bắn súng xuất phát" → tất cả threads chạy cùng lúc
        done.await(); // Đợi tất cả threads hoàn thành

        // Kiểm tra kết quả
        Product sanPham = productDAO.findById(productId);
        int tonKhoCuoi = (sanPham == null) ? -1 : sanPham.getStock();
        boolean chongOversellingThanhCong = (tonKhoCuoi >= 0) && (soLuongThanhCong.get() <= initialStock);

        // In kết quả stress test
        System.out.println("\n=== Kết quả Stress Test Flash Sale ===");
        System.out.println("- Số khách hàng (threads): " + threadCount);
        System.out.println("- Tồn kho ban đầu: " + initialStock);
        System.out.println("- Số đơn hàng thành công: " + soLuongThanhCong.get());
        System.out.println("- Tồn kho cuối cùng: " + tonKhoCuoi);
        System.out.println("- Chống overselling thành công: " + chongOversellingThanhCong);
    }
}
