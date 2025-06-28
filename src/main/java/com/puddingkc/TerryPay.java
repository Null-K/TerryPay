package com.puddingkc;

import com.puddingkc.commands.CreateURLCommand;
import com.puddingkc.commands.MainCommand;
import com.puddingkc.database.DatabaseManager;
import com.puddingkc.expansion.OrderExpansion;
import com.puddingkc.utils.CheckAPI;
import com.puddingkc.utils.RunCommand;
import com.puddingkc.utils.URLGeneration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

import static com.puddingkc.configs.PluginConfigs.*;

public class TerryPay extends JavaPlugin {

    private URLGeneration urlGeneration;
    private DatabaseManager databaseManager;
    private CheckAPI checkAPI;
    private RunCommand runCommand;

    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        loadConfig();
        urlGeneration = new URLGeneration(this);
        checkAPI = new CheckAPI(this);
        runCommand = new RunCommand(this);

        Objects.requireNonNull(getCommand("create-order")).setExecutor(new CreateURLCommand(this));
        Objects.requireNonNull(getCommand("create-order")).setTabCompleter(new CreateURLCommand(this));

        Objects.requireNonNull(getCommand("terry-pay")).setExecutor(new MainCommand(this));
        Objects.requireNonNull(getCommand("terry-pay")).setTabCompleter(new MainCommand(this));

        new OrderExpansion(this).register();

        getLogger().info("作者: PuddingKC");
        getLogger().info("版本: " + getDescription().getVersion());
        getLogger().info("部分代码取自 SweetAfdian 插件，十分感谢。");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    private void startTask() {
        if (task != null) {
            task.cancel();
        }
        if (detectionEnabled) {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    checkAPI.runTask();
                }
            }.runTaskTimer(this,0,60 * 20);
        }
    }

    public void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();

        detectionEnabled = config.getBoolean("af-dian.detection",true);
        defaultRemark = config.getString("af-dian.default-remark","");
        userID = config.getString("af-dian.user-id","");
        apiToken = config.getString("af-dian.api-token","");

        startString = config.getString("settings.start-string","");
        aesKey = config.getString("settings.aes-key","");

        payCommands = config.getStringList("command.commands");
        ratio = config.getInt("command.ratio",10);

        sqlMode = config.getString("database.type","sqlite");

        if (!userID.isEmpty() && !apiToken.isEmpty()) {
            startTask();
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        loadDatabase();
    }

    private void loadDatabase() {
        String host = getConfig().getString("database.host");
        int port = getConfig().getInt("database.port");

        String prefix = getConfig().getString("database.prefix","terry_pay_");
        String name = getConfig().getString("database.name");

        String username = getConfig().getString("database.username");
        String password = getConfig().getString("database.password");

        databaseManager = new DatabaseManager(this,prefix,host,port,name,username,password);
        databaseManager.connect();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public URLGeneration getUrlGeneration() {
        return urlGeneration;
    }

    public RunCommand getRunCommand() {
        return runCommand;
    }

    public static boolean isPositiveDouble(String string) {
        try {
            double value = Double.parseDouble(string);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void sendPlayerMessage(CommandSender player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
    }

}