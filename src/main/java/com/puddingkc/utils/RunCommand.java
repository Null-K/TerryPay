package com.puddingkc.utils;

import com.google.gson.JsonObject;
import com.puddingkc.TerryPay;
import com.puddingkc.configs.PluginConfigs;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import java.util.UUID;

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
        String encrypted = optString(order,"custom_order_id",null);

        if (encrypted  != null) {
            encrypted  = encrypted .replaceFirst(startString, "");

            String decrypted;
            try {
                decrypted = decrypt(encrypted,aesKey);
                //playerName = playerName.substring(0,playerName.length()-5);
            } catch (Exception e) {
                addError(key,order);
                plugin.getLogger().warning("解密失败，订单 " + key + " 无法执行，已跳过。");
                return;
            }

            String[] parts = decrypted.split(":");
            if (parts.length != 2) {
                addError(key,order);
                plugin.getLogger().warning("订单 " + key + " 解密格式不正确，已跳过执行: " + decrypted);
                return;
            }

            String playerName = parts[0];
            String serverName = parts[1];

            if (!serverName.equals(PluginConfigs.serverName)) {
                plugin.getLogger().warning("订单 " + key + " 服务器名称不匹配，已跳过执行: " + decrypted);
                return;
            }

            if (playerName.length() < 5) {
                addError(key,order);
                plugin.getLogger().warning("订单 " + key + " 玩家名称不正确，已跳过执行: " + decrypted);
                return;
            }

            playerName = playerName.substring(0,playerName.length()-5);
            Player player = Bukkit.getPlayer(UUID.fromString(playerName));
            if (player == null || !player.isOnline()) { return; }

            String amount = optString(order,"total_amount",null);
            if (!isPositiveDouble(amount)) { return; }

            if (plugin.getDatabaseManager().addOrder(key,player.getUniqueId().toString(),Double.parseDouble(amount))) {
                if (initialization) {
                    plugin.getLogger().warning("当前处于初始化模式，已跳过执行 " + player.getName() + " 订单 (" + key + ")。");
                    return;
                }

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

                if (command.startsWith("message:")) {
                    String message = command.replace("message:", "");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                    return;
                }

                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    public static String testEncrypt(String text, String key) throws Exception {
        if (text.startsWith(startString)) {
            text = text.replaceFirst(startString, "");
            text = decrypt(text, key);

            String[] parts = text.split(":");

            String name = parts[0];
            String server = parts[1];

            if (name.length() >= 5) {
                name = name.substring(0,name.length()-5);
            }

            text = name + " -> " + server;
        }
        return text;
    }

    private void addError(String key, JsonObject order) {
        plugin.getDatabaseManager().addOrder(key,"00000000-0000-0000-0000-000000000000",Double.parseDouble(optString(order,"total_amount","-1.0")));
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
