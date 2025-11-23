package dev.drkvoid.listeners;

import dev.drkvoid.DrStats;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final DrStats plugin;

    public PlayerListener(DrStats plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBackupManager().createSnapshot(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getBackupManager().createSnapshot(event.getEntity());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && plugin.getUpdateChecker().isUpdateAvailable()) {
            player.sendMessage(ChatColor.GOLD + "[DrStats] " + ChatColor.YELLOW + "A new update is available: " + ChatColor.GREEN + plugin.getUpdateChecker().getLatestVersion());
            player.sendMessage(ChatColor.GOLD + "[DrStats] " + ChatColor.YELLOW + "Download: " + ChatColor.WHITE + plugin.getUpdateChecker().getDownloadUrl());
        }
    }
}