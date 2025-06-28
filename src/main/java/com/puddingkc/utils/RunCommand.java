package com.puddingkc.utils;

import com.google.gson.JsonObject;
import com.puddingkc.TerryPay;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

import static com.puddingkc.TerryPay.isPositiveDouble;
import static com.puddingkc.configs.PluginConfigs.*;
import static com.puddingkc.utils.JsonUtils.generateKey;
import static com.puddingkc.utils.JsonUtils.optString;

public class RunCommand {

    private final TerryPay plugin;
    public RunCommand(TerryPay plugin) {
        this.plugin = plugin;
    }

    public void runOrderCommand(String key, JsonObject order) {
        String playerName = optString(order,"custom_order_id",null);
        if (playerName != null) {
            playerName = playerName.replaceFirst(startString, "");

            try {
                playerName = decrypt(playerName,aesKey);
                playerName = playerName.substring(0,playerName.length()-5);
            } catch (Exception e) {
                plugin.getLogger().warning("解密失败，订单 " + key + " (" + playerName + ") 无法执行。");
                return;
            }

            Player player = Bukkit.getPlayer(playerName);
            if (player == null || !player.isOnline()) { return; }

            String amount = optString(order,"total_amount",null);
            if (!isPositiveDouble(amount)) { return; }

            if (plugin.getDatabaseManager().addOrder(key)) {
                runCommand(player, Double.parseDouble(amount));
                plugin.getLogger().info("玩家 " + player.getName() + " 订单 (" + key + ") 执行完成，金额 " + amount + " 元。");
            }
        }
    }

    public void runCommand(Player player, double amount) {
        List<String> commands = payCommands;
        if (!commands.isEmpty()) {
            for (String command : commands) {
                command = command.replace("{player}", player.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{points}", String.valueOf((int) Math.ceil(amount * ratio)));
                PlaceholderAPI.setPlaceholders(player, command);
                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    public static String testEncrypt(String text, String key) throws Exception {
        if (text.startsWith(startString)) {
            text = text.replaceFirst(startString, "");
            text = decrypt(text, key);
            text = text.substring(0,text.length()-5);
        }
        return text;
    }

    private static String decrypt(String encryptedText, String key) throws Exception {
        SecretKeySpec secretKey = generateKey(key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(hexToBytes(encryptedText));
        return new String(decryptedBytes);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
