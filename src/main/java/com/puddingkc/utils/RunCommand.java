package com.puddingkc.utils;

import com.google.gson.JsonObject;
import com.puddingkc.TerryPay;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.puddingkc.TerryPay.isPositiveDouble;
import static com.puddingkc.utils.JsonUtils.optString;

public class RunCommand {

    private final TerryPay plugin;
    public RunCommand(TerryPay plugin) {
        this.plugin = plugin;
    }

    public void runOrderCommand(String key, JsonObject order) {
        String playerName = optString(order,"custom_order_id",null);
        if (playerName != null) {
            playerName = decodeBase64(playerName);

            Player player = Bukkit.getPlayer(UUID.fromString(playerName));
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
        List<String> commands = plugin.getConfigs().getStringList("command.commands");
        if (!commands.isEmpty()) {
            for (String command : commands) {
                command = command.replace("{player}", player.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{points}", String.valueOf(amount * TerryPay.getRatio()));
                PlaceholderAPI.setPlaceholders(player, command);
                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    private String decodeBase64(String base64Encoded) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(base64Encoded);
        return new String(decodedBytes);
    }
}
