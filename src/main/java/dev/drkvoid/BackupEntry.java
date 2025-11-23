package dev.drkvoid;

import org.bukkit.Statistic;
import java.util.UUID;

public class BackupEntry {

    private final UUID playerUuid;
    private final Statistic stat;
    private final int oldValue;

    public BackupEntry(UUID playerUuid, Statistic stat, int oldValue) {
        this.playerUuid = playerUuid;
        this.stat = stat;
        this.oldValue = oldValue;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Statistic getStat() {
        return stat;
    }

    public int getOldValue() {
        return oldValue;
    }
}