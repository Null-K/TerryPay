package com.puddingkc.utils;

import com.google.gson.*;
import com.puddingkc.TerryPay;

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

        JsonObject result = request(path, TerryPay.getUserId(), TerryPay.getApiToken(), params);
        Map<String, JsonObject> ordersMap = new HashMap<>();

        if (optInt(result, "ec", 0) == 200) {
            JsonObject data = optObject(result, "data");
            JsonArray list = optArray(data, "list");

            for (JsonElement element : list) {
                JsonObject order = element.getAsJsonObject();
                String outTradeNo = optString(order, "out_trade_no", null);
                if (outTradeNo == null) continue;
                ordersMap.put(outTradeNo, order);
            }
        }

        List<String> outTradeNos = new ArrayList<>(ordersMap.keySet());
        outTradeNos.removeIf(outTradeNo -> {
            JsonObject order = ordersMap.get(outTradeNo);
            String customID = optString(order, "custom_order_id", null);
            return customID == null;
        });

        List<String> missingOutTradeNos = plugin.getDatabaseManager().getMissingOrders(outTradeNos);

        for (String key : missingOutTradeNos) {
            JsonObject order = ordersMap.get(key);
            if (order == null || optInt(order, "status", -1) != 2) { continue; }
            plugin.getLogger().info("有新的爱发电订单: " + key);
            plugin.getRunCommand().runOrderCommand(key,order);
        }
    }

    private JsonObject request(String path, String userId, String token, JsonObject params) {
        try {
            String url = "https://afdian.com" + path;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setDoOutput(true);
            conn.setDoInput(true);
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
                return element.getAsJsonObject();
            }
        } catch (IOException | JsonSyntaxException e) {
            return null;
        }
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
