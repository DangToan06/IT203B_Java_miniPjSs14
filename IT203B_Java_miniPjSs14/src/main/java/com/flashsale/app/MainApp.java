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

}