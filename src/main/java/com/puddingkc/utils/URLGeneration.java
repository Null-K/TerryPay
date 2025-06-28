package com.puddingkc.utils;

import com.germ.germplugin.api.GermPacketAPI;
import com.puddingkc.TerryPay;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.puddingkc.configs.PluginConfigs.*;
import static com.puddingkc.utils.JsonUtils.generateKey;
import static com.puddingkc.utils.JsonUtils.getRandomString;

public class URLGeneration {

    private final TerryPay plugin;
    public URLGeneration(TerryPay plugin) {
        this.plugin = plugin;
    }

    public String generateURL(double amount, Player player, String remark) {
        try {
            if (userID == null || userID.isEmpty()) {
                plugin.getLogger().warning("爱发电用户 ID 未设置，无法生成订单地址。");
                return null;
            }

            String remarkEncoded = URLEncoder.encode(remark, StandardCharsets.UTF_8.toString());
            String playerName = player.getName() + getRandomString();

            return "https://ifdian.net/order/create?user_id=" + userID
                    + "&remark=" + remarkEncoded
                    + "&affiliate_code="
                    + "&custom_price=" + amount
                    + "&custom_order_id=" + encrypt(playerName,aesKey);
        }  catch (Exception e) {
            plugin.getLogger().warning("爱发电订单地址生成失败: " + e.getMessage());
            return null;
        }
    }

    public void sendGermDos(Player player, String url) {
        GermPacketAPI.sendHudDos(player, "openWeb<->" + url);
    }

    private static String encrypt(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = generateKey(key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return startString + bytesToHex(encryptedBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
