package com.flashsale.app;

import com.flashsale.dao.ProductDAO;
import com.flashsale.dao.UserDAO;
import com.flashsale.entity.CategoryRevenueReport;
import com.flashsale.entity.Product;
import com.flashsale.entity.TopBuyerReport;
import com.flashsale.entity.User;
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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MainApp {

    private static final int STRESS_THREAD_COUNT = 50;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            int choice = readInt(scanner);

            try {
                switch (choice) {
                    case 1:
                        initializeDatabase();
                        break;
                    case 2:
                        showAllProducts();
                        break;
                    case 3:
                        placeFlashSaleOrder(scanner);
                        break;
                    case 4:
                        showReports();
                        break;
                    case 5:
                        runStressTest(scanner);
                        break;
                    case 0:
                        System.out.println("Tạm biệt!");
                        break;

                    default:
                        System.out.println("Lựa chọn không hợp lệ. Vui lòng nhập lại.");
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Có lỗi khi xử lý chức năng: " + ex.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n---  FLASH SALE MANAGEMENT SYSTEM  ---");
        System.out.println("1. Khởi tạo Database (Tạo bảng/Procedures)");
        System.out.println("2. Xem danh sách sản phẩm trong kho");
        System.out.println("3. Đặt hàng Flash Sale (Xử lý Transaction)");
        System.out.println("4. Xem báo cáo thống kê (Top Buyer/Revenue)");
        System.out.println("5. Chạy Stress Test (Mô phỏng 50 Threads)");
        System.out.println("0. Thoát");
        System.out.print(" Chọn chức năng: ");
    }

    private static void initializeDatabase() throws SQLException, IOException {
        try (Connection connection = DatabaseConnectionManager.getInstance().getConnection();
             Statement statement = connection.createStatement()) {
            executeSqlScript(statement, "/init.sql");
            System.out.println("Da khoi tao database tu init.sql thanh cong.");
        }
    }

    private static void executeSqlScript(Statement statement, String resourcePath) throws IOException, SQLException {
        InputStream in = MainApp.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Khong tim thay file SQL: " + resourcePath);
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

    private static void showAllProducts() throws SQLException {
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.findAll();

        System.out.println("\n=== DANH SACH SAN PHAM ===");
        if (products.isEmpty()) {
            System.out.println("Chua co du lieu san pham. Hay chay chuc nang khoi tao database.");
            return;
        }

        System.out.printf("%-10s %-25s %-12s %-10s %-12s%n", "ID", "Ten", "Gia", "Ton kho", "Category");
        for (Product product : products) {
            System.out.printf("%-10d %-25s %-12s %-10d %-12d%n",
                    product.getProductId(),
                    product.getProductName(),
                    product.getPrice(),
                    product.getStock(),
                    product.getCategoryId());
        }
    }

    private static void placeFlashSaleOrder(Scanner scanner) throws SQLException {
        UserDAO userDAO = new UserDAO();
        ProductDAO productDAO = new ProductDAO();
        OrderService orderService = new OrderService();

        List<User> users = userDAO.findAll();
        if (users.isEmpty()) {
            System.out.println("Khong co nguoi dung de dat hang. Hay khoi tao database truoc.");
            return;
        }

        System.out.println("\n=== DANH SACH USER ===");
        for (User user : users) {
            System.out.printf("- %d: %s (%s)%n", user.getUserId(), user.getUsername(), user.getEmail());
        }
        System.out.print("Nhap user_id dat hang: ");
        int userId = readInt(scanner);

        showAllProducts();
        Map<Integer, Integer> productIdToQuantity = new HashMap<>();

        System.out.println("Nhap san pham can mua (nhap 0 de ket thuc):");
        while (true) {
            System.out.print("product_id: ");
            int productId = readInt(scanner);
            if (productId == 0) {
                break;
            }

            Product product = productDAO.findById(productId);
            if (product == null) {
                System.out.println("Khong tim thay san pham voi ID " + productId + ".");
                continue;
            }

            System.out.print("so luong: ");
            int quantity = readInt(scanner);
            if (quantity <= 0) {
                System.out.println("So luong phai > 0.");
                continue;
            }

            productIdToQuantity.merge(productId, quantity, Integer::sum);
        }

        if (productIdToQuantity.isEmpty()) {
            System.out.println("Khong co san pham nao duoc chon.");
            return;
        }

        boolean success = orderService.placeOrder(userId, productIdToQuantity);
        System.out.println(
                success ? "Dat hang thanh cong." : "Dat hang that bai (co the het hang hoac user khong hop le).");
    }

    private static void showReports() throws SQLException {
        OrderService orderService = new OrderService();

        System.out.println("\n=== TOP BUYERS ===");
        for (TopBuyerReport buyer : orderService.getTopBuyers()) {
            System.out.printf("- userId=%d, username=%s, totalItems=%d%n",
                    buyer.getUserId(), buyer.getUsername(), buyer.getTotalItems());
        }

        System.out.println("\n=== REVENUE BY CATEGORY ===");
        for (CategoryRevenueReport revenue : orderService.getRevenueByCategory()) {
            System.out.printf("- category=%s, revenue=%s%n",
                    revenue.getCategoryName(), revenue.getRevenue());
        }
    }

    private static void runStressTest(Scanner scanner) throws SQLException, InterruptedException {
        ProductDAO productDAO = new ProductDAO();
        OrderService orderService = new OrderService();

        System.out.print("Nhap user_id de mo phong dat hang: ");
        int userId = readInt(scanner);
        System.out.print("Nhap product_id de stress test: ");
        int productId = readInt(scanner);
        System.out.print("Nhap ton kho ban dau: ");
        int initialStock = readInt(scanner);

        if (initialStock < 0) {
            System.out.println("Ton kho ban dau phai >= 0.");
            return;
        }

        productDAO.updateStock(productId, initialStock);

        CountDownLatch ready = new CountDownLatch(STRESS_THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(STRESS_THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < STRESS_THREAD_COUNT; i++) {
            Thread thread = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    boolean success = orderService.placeOrder(userId, Map.of(productId, 1));
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }

        ready.await();
        start.countDown();
        done.await();

        Product product = productDAO.findById(productId);
        int finalStock = product == null ? -1 : product.getStock();
        boolean noOverselling = (finalStock >= 0) && (successCount.get() <= initialStock);

        System.out.println("\n=== KET QUA STRESS TEST ===");
        System.out.println("- So luong thread: " + STRESS_THREAD_COUNT);
        System.out.println("- Ton kho ban dau: " + initialStock);
        System.out.println("- So don thanh cong: " + successCount.get());
        System.out.println("- Ton kho cuoi: " + finalStock);
        System.out.println("- Chong overselling thanh cong: " + noOverselling);
    }

    private static int readInt(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.print("Vui long nhap so nguyen hop le: ");
            }
        }
    }

}
