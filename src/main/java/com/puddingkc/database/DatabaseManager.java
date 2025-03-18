package com.puddingkc.database;

import com.puddingkc.TerryPay;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseManager {

    private Connection connection;
    private final TerryPay plugin;

    private final String prefix;
    private final String databaseUrl;

    private final String username;
    private final String password;

    public DatabaseManager(TerryPay plugin, String prefix, String host, int port, String databaseName, String username, String password) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.username = username;
        this.password = password;

        this.databaseUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&serverTimezone=Asia/Shanghai";
    }

    public void connect() {
        try {
            if (plugin.getConfigs().getString("database.type","sqlite").equals("mysql")) {
                connection = DriverManager.getConnection(databaseUrl, username, password);
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() +  "/terryPay.db");
            }
            plugin.getLogger().info("数据库连接成功。");
            createTableIfNotExists();
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库连接失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("数据库关闭成功。");
            } catch (SQLException e) {
                plugin.getLogger().severe("数据库关闭失败: " + e.getMessage());
            }
        }
    }

    private void createTableIfNotExists() {
        String createOrderTableQuery = "CREATE TABLE IF NOT EXISTS " + prefix + "orders (" +
                "order_id VARCHAR(36) PRIMARY KEY" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createOrderTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().severe("新建订单数据表失败: " + e.getMessage());
        }
    }

    public List<String> getMissingOrders(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        String placeholders = String.join(",", Collections.nCopies(orderIds.size(), "?"));
        String selectQuery = "SELECT order_id FROM " + prefix + "orders WHERE order_id IN (" + placeholders + ")";
        List<String> missingOrders = new ArrayList<>(orderIds);

        try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
            for (int i = 0; i < orderIds.size(); i++) {
                statement.setString(i + 1, orderIds.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    missingOrders.remove(resultSet.getString("order_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("检查订单号失败: " + e.getMessage());
        }

        return missingOrders;
    }

    public boolean addOrder(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return false;
        }
        String insertQuery = "INSERT INTO " + prefix + "orders (order_id) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
            statement.setString(1, orderId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("添加订单号失败: " + e.getMessage());
            return false;
        }
    }

}
