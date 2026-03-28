package com.sy.bountytasks.economy;

import com.sy.bountytasks.SyBountyTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final SyBountyTask plugin;
    private Economy economy;

    public EconomyManager(SyBountyTask plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().severe("No economy plugin found!");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Economy hooked successfully: " + economy.getName());
        return true;
    }

    public boolean hasEnough(Player player, double amount) {
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public String format(double amount) {
        return economy.format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isEnabled() {
        return economy != null;
    }
}
