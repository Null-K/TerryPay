package com.puddingkc.expansion;

import com.puddingkc.TerryPay;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.puddingkc.TerryPay.isPositiveDouble;
import static com.puddingkc.configs.PluginConfigs.defaultRemark;

public class OrderExpansion extends PlaceholderExpansion {

    private final TerryPay plugin;
    public OrderExpansion(TerryPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "terrypay";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PuddingKC";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || !player.isOnline()) { return null; }

        String[] args = params.split("_");

        if (args.length >= 1) {
            if (!isPositiveDouble(args[0])) { return null; }
            double amount = Math.max(5, Double.parseDouble(args[0]));

            String remark = defaultRemark;
            if (args.length >= 2) {
                remark = args[1];
            }

            return plugin.getUrlGeneration().generateURL(amount, player, remark);
        }

        return null;
    }
}
