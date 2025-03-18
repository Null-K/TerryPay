package com.puddingkc.utils;

import com.puddingkc.TerryPay;
import org.bukkit.entity.Player;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class URLGeneration {

    private final TerryPay plugin;
    public URLGeneration(TerryPay plugin) {
        this.plugin = plugin;
    }

    public String generateURL(double amount, Player player, String remark) {
        try {
            if (TerryPay.getUserId() == null || TerryPay.getUserId().isEmpty()) {
                plugin.getLogger().warning("爱发电用户 ID 未设置，无法生成订单地址。");
                return null;
            }

            String remarkEncoded = URLEncoder.encode(remark, StandardCharsets.UTF_8.toString());
            return "https://ifdian.net/order/create?user_id=" + TerryPay.getUserId()
                    + "&remark=" + remarkEncoded
                    + "&affiliate_code="
                    + "&custom_price=" + amount
                    + "&custom_order_id=" + stringToBase64(player.getUniqueId().toString());
        }  catch (Exception e) {
            plugin.getLogger().warning("爱发电订单地址生成失败: " + e.getMessage());
            return null;
        }
    }

    private static String stringToBase64(String string) {
        return Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

}
