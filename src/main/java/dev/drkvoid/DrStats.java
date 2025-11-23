package dev.drkvoid;

import dev.drkvoid.listeners.PlayerListener;
import dev.drkvoid.managers.BackupManager;
import dev.drkvoid.managers.LogManager;
import dev.drkvoid.utils.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class DrStats extends JavaPlugin {

    private static DrStats instance;
    private BackupManager backupManager;
    private LogManager logManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;


        this.logManager = new LogManager(this);
        this.backupManager = new BackupManager(this);


        new Metrics(this, 28110);


        this.updateChecker = new UpdateChecker(this);
        this.updateChecker.checkForUpdates();


        getCommand("stats").setExecutor(new StatsCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(this.backupManager, this);

        getLogger().info("DrStats v2.0 has been enabled.");
    }

    @Override
    public void onDisable() {
        if (backupManager != null) {
            backupManager.shutdown();
        }
        getLogger().info("DrStats v2.0 has been disabled.");
    }

    public static DrStats getInstance() {
        return instance;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}