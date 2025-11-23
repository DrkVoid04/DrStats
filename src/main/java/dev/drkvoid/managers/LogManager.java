package dev.drkvoid.managers;

import dev.drkvoid.DrStats;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private final File logFile;

    public LogManager(DrStats plugin) {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        this.logFile = new File(folder, "admin_actions.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logAction(String admin, String action) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String msg = "[" + time + "] Admin: " + admin + " | Action: " + action;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(msg);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}