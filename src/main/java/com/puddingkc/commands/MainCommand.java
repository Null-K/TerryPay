package com.puddingkc.commands;

import com.puddingkc.TerryPay;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.puddingkc.TerryPay.isPositiveDouble;
import static com.puddingkc.TerryPay.sendPlayerMessage;
import static com.puddingkc.configs.PluginConfigs.aesKey;
import static com.puddingkc.utils.RunCommand.testEncrypt;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final TerryPay plugin;
    public MainCommand(TerryPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!sender.isOp()) { return false; }

        if (args.length >= 1) {
            switch (args[0]) {
                case "reload":
                    plugin.loadConfig();
                    sendPlayerMessage(sender,"&c[TerryPay] &7配置文件已重新加载。");
                    break;
                case "run":
                    if (args.length == 3) {
                        Player player = Bukkit.getPlayer(args[1]);
                        if (player == null || !player.isOnline()) {
                            sendPlayerMessage(sender,"&c[TerryPay] &7指定的玩家不在线或不存在。");
                            return false;
                        }

                        if (!isPositiveDouble(args[2])) {
                            sendPlayerMessage(sender,"&c[TerryPay] &7金额必须为正数。");
                            return false;
                        }

                        plugin.getRunCommand().runCommand(player, Double.parseDouble(args[2]));
                        sendPlayerMessage(sender,"&c[TerryPay] &7已为玩家 &f" + player.getName() + " &7充值 &f" + args[2] + " &7元。");
                    } else {
                        sendPlayerMessage(sender,"&c[TerryPay] &7/terry-pay run <玩家名> <金额>");
                    }
                    break;
                case "test":
                    if (args.length == 3) {
                        String id = args[1];
                        String key = args[2];
                        try {
                            sendPlayerMessage(sender,"&c[TerryPay] &7测试解密结果: " + testEncrypt(id, key));
                        } catch (Exception e) {
                            sendPlayerMessage(sender,"&c[TerryPay] &7解密失败: " + e.getMessage());
                        }
                        return true;
                    }
                    break;
            }
            return true;
        }

        sendPlayerMessage(sender,"&c[TerryPay] &7/terry-pay reload");
        sendPlayerMessage(sender,"&c[TerryPay] &7/terry-pay run <玩家名> <金额>");
        sendPlayerMessage(sender,"&c[TerryPay] &7/terry-pay test <值> <密钥>");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!sender.isOp()) { return suggestions; }

        if (args.length == 1) {
            suggestions.add("reload");
            suggestions.add("run");
            suggestions.add("test");
        }

        if (args.length == 2 && args[0].equals("run")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }

        if (args.length == 3 && args[0].equals("test")) {
            suggestions.add(aesKey);
        }

        if (args.length == 3 && args[0].equals("run")) {
            suggestions.add("3.5");
            suggestions.add("10.0");
            suggestions.add("20.0");
        }

        return suggestions;
    }
}
