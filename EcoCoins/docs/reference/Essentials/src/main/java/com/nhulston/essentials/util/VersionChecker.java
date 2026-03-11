package com.nhulston.essentials.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Checks CurseForge for new plugin versions using the CFWidget API.
 */
public class VersionChecker {
    private static final String CFWIDGET_API_URL = "https://api.cfwidget.com/hytale/mods/essentials-core";
    private static final int TIMEOUT_MS = 10000;

    private final String currentVersion;
    private volatile String latestVersion = null;
    private volatile boolean updateAvailable = false;

    public VersionChecker(@Nonnull String currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * Asynchronously checks for updates.
     * Call this on startup, results available via getters.
     */
    public void checkForUpdatesAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                String latest = fetchLatestVersion();
                if (latest != null) {
                    latestVersion = latest;
                    updateAvailable = isNewerVersion(latest, currentVersion);
                    
                    if (updateAvailable) {
                        Log.info("New version available: " + latest + " (current: " + currentVersion + ")");
                    } else {
                        Log.info("Plugin is up to date (v" + currentVersion + ")");
                    }
                }
            } catch (Exception e) {
                Log.warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches the latest version from CurseForge via CFWidget API.
     */
    @Nullable
    private String fetchLatestVersion() {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(CFWIDGET_API_URL);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Essentials-Plugin-VersionChecker/1.0");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Log.warning("CFWidget API returned status " + responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return parseVersionFromJson(response.toString());
        } catch (Exception e) {
            Log.warning("Error fetching version: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parses the version from CFWidget JSON response.
     * Looks at the "download" object which contains the latest file.
     */
    @Nullable
    private String parseVersionFromJson(@Nonnull String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            
            // The "download" field contains the latest release
            if (root.has("download")) {
                JsonObject download = root.getAsJsonObject("download");
                String name = download.get("name").getAsString();
                // Name format: "Essentials-1.2.1.jar"
                return extractVersion(name);
            }
            
            // Fallback: check files array for the first entry
            if (root.has("files")) {
                JsonArray files = root.getAsJsonArray("files");
                if (!files.isEmpty()) {
                    JsonObject firstFile = files.get(0).getAsJsonObject();
                    String name = firstFile.get("name").getAsString();
                    return extractVersion(name);
                }
            }
            
            Log.warning("Could not find version in CFWidget response");
            return null;
        } catch (Exception e) {
            Log.warning("Error parsing CFWidget JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts version from filename like "Essentials-1.2.1.jar"
     */
    @Nullable
    private String extractVersion(@Nonnull String filename) {
        // Remove "Essentials-" prefix and ".jar" suffix
        if (filename.startsWith("Essentials-") && filename.endsWith(".jar")) {
            return filename.substring(11, filename.length() - 4);
        }
        return null;
    }

    /**
     * Compares two semantic versions.
     * Returns true if version1 is newer than version2.
     */
    private boolean isNewerVersion(@Nonnull String version1, @Nonnull String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 > v2) return true;
            if (v1 < v2) return false;
        }
        return false;
    }

    private int parseVersionPart(@Nonnull String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns true if an update is available.
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Returns the latest version found, or null if not checked yet.
     */
    @Nullable
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Returns the current plugin version.
     */
    @Nonnull
    public String getCurrentVersion() {
        return currentVersion;
    }
}
