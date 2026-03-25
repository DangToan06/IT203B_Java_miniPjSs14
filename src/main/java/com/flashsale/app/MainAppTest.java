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

public class MainAppTest {
    private static final int DEMO_USER_ID = 1; 
    private static final int STRESS_PRODUCT_ID = 1; 
    private static final int STRESS_INITIAL_STOCK = 10; 
    private static final int STRESS_THREAD_COUNT = 50; 

    public static void main(String[] args) {
        try {
            initializeDatabase();
            runBasicFlow(DEMO_USER_ID);
            runStressTest(DEMO_USER_ID, STRESS_PRODUCT_ID, STRESS_INITIAL_STOCK, STRESS_THREAD_COUNT);
        } catch (Exception e) {
            System.err.println("Lỗi ứng dụng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeDatabase() throws SQLException, IOException {
        try (Connection connection = DatabaseConnectionManager.getInstance().getConnection();
             Statement statement = connection.createStatement()) {
            executeSqlScript(statement, "/init.sql");
            System.out.println("=== Đã khởi tạo database từ init.sql ===");
        }
    }

    private static void executeSqlScript(Statement statement, String resourcePath) throws IOException, SQLException {
        // Mở file SQL từ resources
        InputStream in = MainAppTest.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Không tìm thấy file SQL: " + resourcePath);
        }

        String delimiter = ";"; 
        StringBuilder command = new StringBuilder(); 

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }

                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring("DELIMITER ".length()).trim();
                    continue;
                }

                command.append(line).append('\n');
                String current = command.toString().trim();

                if (current.endsWith(delimiter)) {
                    String sql = current.substring(0, current.length() - delimiter.length()).trim();
                    if (!sql.isEmpty()) {
                        statement.execute(sql);
                    }
                    command.setLength(0);
                }
            }
        }
    }

    private static void runBasicFlow(int userId) throws SQLException {
        OrderService orderService = new OrderService();

        Map<Integer, Integer> donHangMau = new HashMap<>();
        donHangMau.put(1, 1);
        donHangMau.put(2, 2);

        boolean datHangThanhCong = orderService.placeOrder(userId, donHangMau);
        System.out.println("Kết quả đặt hàng mẫu: " + (datHangThanhCong ? "THÀNH CÔNG" : "THẤT BẠI"));

        System.out.println("\n=== Top người mua hàng nhiều nhất ===");
        for (TopBuyerReport buyer : orderService.getTopBuyers()) {
            System.out.printf("- userId=%d, username=%s, tổng SP đã mua=%d%n",
                    buyer.getUserId(), buyer.getUsername(), buyer.getTotalItems());
        }

        System.out.println("\n=== Doanh thu theo danh mục ===");
        for (CategoryRevenueReport revenue : orderService.getRevenueByCategory()) {
            System.out.printf("- Danh mục=%s, Doanh thu=%s%n", revenue.getCategoryName(), revenue.getRevenue());
        }
    }

    private static void runStressTest(int userId, int productId, int initialStock, int threadCount)
            throws SQLException, InterruptedException {
        ProductDAO productDAO = new ProductDAO();
        OrderService orderService = new OrderService();

        productDAO.updateStock(productId, initialStock);

        CountDownLatch ready = new CountDownLatch(threadCount); 
        CountDownLatch start = new CountDownLatch(1); 
        CountDownLatch done = new CountDownLatch(threadCount); 
        AtomicInteger soLuongThanhCong = new AtomicInteger(0); 

        
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
        
                ready.countDown();
                try {
                    start.await()
                    boolean thanhCong = orderService.placeOrder(userId, Map.of(productId, 1));
                    if (thanhCong) {
                        soLuongThanhCong.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            t.start();
        }

        ready.await();
        start.countDown(); 
        done.await(); 

        Product sanPham = productDAO.findById(productId);
        int tonKhoCuoi = (sanPham == null) ? -1 : sanPham.getStock();
        boolean chongOversellingThanhCong = (tonKhoCuoi >= 0) && (soLuongThanhCong.get() <= initialStock);

        System.out.println("\n=== Kết quả Stress Test Flash Sale ===");
        System.out.println("- Số khách hàng (threads): " + threadCount);
        System.out.println("- Tồn kho ban đầu: " + initialStock);
        System.out.println("- Số đơn hàng thành công: " + soLuongThanhCong.get());
        System.out.println("- Tồn kho cuối cùng: " + tonKhoCuoi);
        System.out.println("- Chống overselling thành công: " + chongOversellingThanhCong);
    }
}
