package dev.drkvoid.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.drkvoid.DrStats;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final DrStats plugin;
    private final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/drstats/version";
    private final String DOWNLOAD_URL = "https://modrinth.com/plugin/drstats";
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(DrStats plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(MODRINTH_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "DrkVoid/DrStats");

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();

                    if (versions.size() > 0) {
                        this.latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();
                        String currentVersion = plugin.getDescription().getVersion();

                        if (isNewer(currentVersion, latestVersion)) {
                            this.updateAvailable = true;
                            plugin.getLogger().warning("A new version of DrStats is available: " + latestVersion);
                            plugin.getLogger().warning("Download it at: " + DOWNLOAD_URL);
                        } else {
                            this.updateAvailable = false;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }


    private boolean isNewer(String current, String latest) {
        if (current == null || latest == null) return false;

        String currentClean = current.replaceAll("[^0-9.]", "");
        String latestClean = latest.replaceAll("[^0-9.]", "");

        String[] currentParts = currentClean.split("\\.");
        String[] latestParts = latestClean.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int v2 = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (v2 > v1) return true;
            if (v1 > v2) return false;
        }

        return false;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getDownloadUrl() {
        return DOWNLOAD_URL;
    }
}