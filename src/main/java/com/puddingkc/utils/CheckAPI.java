package com.puddingkc.utils;

import com.google.gson.*;
import com.puddingkc.TerryPay;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

import static com.puddingkc.configs.PluginConfigs.*;
import static com.puddingkc.utils.JsonUtils.*;

public class CheckAPI {

    private final TerryPay plugin;
    public CheckAPI(TerryPay plugin) {
        this.plugin = plugin;
    }

    public void runTask() {
        String path = "/api/open/query-order";
        JsonObject params = new JsonObject();
        params.addProperty("page", 1);

        request(path, userID, apiToken, params,
                (result) -> {
                    if (result == null || optInt(result, "ec", 0) != 200) {
                        plugin.getLogger().warning("请求订单列表失败或无效返回");
                        return;
                    }

                    Map<String, JsonObject> ordersMap = new HashMap<>();
                    JsonArray list = optArray(optObject(result, "data"), "list");

                    for (JsonElement element : list) {
                        JsonObject order = element.getAsJsonObject();
                        String outTradeNo = optString(order, "out_trade_no", null);
                        if (outTradeNo == null) continue;
                        ordersMap.put(outTradeNo, order);
                    }

                    List<String> outTradeNos = getStrings(ordersMap);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        List<String> missingOutTradeNos = plugin.getDatabaseManager().getMissingOrders(outTradeNos);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (String key : missingOutTradeNos) {
                                JsonObject order = ordersMap.get(key);
                                if (order == null || optInt(order, "status", -1) != 2) { continue; }
                                plugin.getLogger().info("有新的爱发电订单: " + key);
                                plugin.getRunCommand().runOrderCommand(key,order);
                            }
                        });
                    });
                });
    }

    private static @NotNull List<String> getStrings(Map<String, JsonObject> ordersMap) {
        List<String> outTradeNos = new ArrayList<>(ordersMap.keySet());
        outTradeNos.removeIf(outTradeNo -> {
            JsonObject order = ordersMap.get(outTradeNo);
            String customID = optString(order, "custom_order_id", null);
            return customID == null;
        });

        if (startString != null && !startString.isEmpty()) {
            outTradeNos.removeIf(outTradeNo -> {
                String id = optString(ordersMap.get(outTradeNo),"custom_order_id",null);
                return !id.startsWith(startString);
            });
        }
        return outTradeNos;
    }

    private void request(String path, String userId, String token, JsonObject params, Consumer<JsonObject> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection conn = null;
            try {
                String url = "https://afdian.com" + path;
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);

                String body = buildParams(userId, token, params);
                try (PrintWriter writer = new PrintWriter(conn.getOutputStream())) {
                    writer.write(body);
                    writer.flush();
                }
                try (InputStream input = conn.getInputStream();
                     InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[1024];
                    int len;
                    while ((len = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, len);
                    }
                    JsonElement element = JsonParser.parseString(sb.toString());
                    JsonObject result = element.getAsJsonObject();

                    //JsonElement element = new JsonParser().parse(sb.toString());
                    //return element.getAsJsonObject();

                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
                }
            } catch (IOException | JsonSyntaxException e) {
                plugin.getLogger().warning("请求爱发电 API 失败: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private String buildParams(String userId, String token, JsonObject params) {
        JsonObject object = new JsonObject();
        object.addProperty("user_id", userId);
        object.addProperty("params", params.toString());
        long timestamp = System.currentTimeMillis() / 1000L;
        String sign = sign(userId, token, params.toString(), timestamp);
        object.addProperty("ts", timestamp);
        object.addProperty("sign", sign);
        return object.toString();
    }

    private String sign(String userId, String token, String params, long timestamp) {
        String string = token + "params" + params + "ts" + timestamp + "user_id" + userId;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

}
