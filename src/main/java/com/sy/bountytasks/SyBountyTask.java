package com.sy.bountytasks;

import com.sy.bountytasks.command.CommandHandler;
import com.sy.bountytasks.command.SyBTCommand;
import com.sy.bountytasks.database.DatabaseManager;
import com.sy.bountytasks.economy.EconomyManager;
import com.sy.bountytasks.gui.GUIListener;
import com.sy.bountytasks.gui.GUIManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SyBountyTask extends JavaPlugin {
    private static SyBountyTask instance;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Failed to setup economy! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        guiManager = new GUIManager(this);
        commandHandler = new CommandHandler(this, guiManager);

        getCommand("sybt").setExecutor(new SyBTCommand(this, commandHandler));
        getCommand("sybt").setTabCompleter(new SyBTCommand(this, commandHandler));

        getServer().getPluginManager().registerEvents(new GUIListener(this, guiManager), this);

        int autoDeleteDays = getConfig().getInt("auto-delete-days", 7);
        if (autoDeleteDays > 0) {
            databaseManager.cleanupOldTasks(autoDeleteDays);
        }

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            int days = getConfig().getInt("auto-delete-days", 7);
            if (days > 0) {
                databaseManager.cleanupOldTasks(days);
            }
        }, 20L * 60 * 60 * 24, 20L * 60 * 60 * 24);

        getLogger().info("SyBountyTask has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("SyBountyTask has been disabled!");
    }

    public static SyBountyTask getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
}
