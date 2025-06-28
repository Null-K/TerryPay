package com.puddingkc.commands;

import com.puddingkc.TerryPay;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.puddingkc.TerryPay.isPositiveDouble;
import static com.puddingkc.TerryPay.sendPlayerMessage;
import static com.puddingkc.configs.PluginConfigs.defaultRemark;

public class CreateURLCommand implements CommandExecutor, TabCompleter {

    private final TerryPay plugin;
    public CreateURLCommand(TerryPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!sender.isOp()) { return false; }

        if (args.length >= 2) {

            Player player = Bukkit.getPlayer(args[0]);
            if (player == null || !player.isOnline()) {
                sendPlayerMessage(sender,"&c[TerryPay] &7指定的玩家不在线或不存在。");
                return false;
            }

            if (!isPositiveDouble(args[1])) {
                sendPlayerMessage(sender,"&c[TerryPay] &7金额必须为正数。");
                return false;
            }

            String remark = getRemark(args, player);

            String orderUrl = plugin.getUrlGeneration().generateURL(Math.max(5, Double.parseDouble(args[1])), player, remark);
            if (orderUrl != null) {
                if (args.length == 4 && args[3].equalsIgnoreCase("-d")) {
                    plugin.getUrlGeneration().sendGermDos(player, orderUrl);
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "[TerryPay] " + ChatColor.GRAY + "订单链接已生成: " + ChatColor.WHITE + orderUrl);
            } else {
                sendPlayerMessage(sender,"&c[TerryPay] &7订单链接生成失败。");
            }
            return true;
        }

        sendPlayerMessage(sender,"&c[TerryPay] &7/create-order <玩家名> <金额> [备注]");
        return false;
    }

    private static String getRemark(String[] args, Player player) {
        String remark = defaultRemark;

        if (args.length >= 3 && !Objects.equals(args[2], "@default")) {
            String message = args[2];

            if (message.equalsIgnoreCase("@player")) {
                remark = player.getName();
            } else if (message.equalsIgnoreCase("@uuid")) {
                remark = player.getUniqueId().toString();
            } else {
                remark = message;
            }
        }
        return remark;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!sender.isOp()) { return suggestions; }

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }

        if (args.length == 2) {
            suggestions.add("5.0");
            suggestions.add("10.0");
            suggestions.add("20.0");
        }

        if (args.length == 3) {
            suggestions.add("@uuid");
            suggestions.add("@player");
            suggestions.add("@default");
            suggestions.add("自定义备注");
        }

        if (args.length == 4) {
            suggestions.add("-d");
        }

        return suggestions;
    }
}
