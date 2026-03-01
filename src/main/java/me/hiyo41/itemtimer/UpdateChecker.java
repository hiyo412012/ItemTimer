package me.hiyo41.itemtimer;

import org.bukkit.Bukkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class UpdateChecker {

    private final ItemTimer plugin;
    private final String githubUrl = "https://api.github.com/repos/hiyo412012/ItemTimer/releases/latest";

    public UpdateChecker(ItemTimer plugin) {
        this.plugin = plugin;
    }

    public void checkUpdate(final Consumer<UpdateResult> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            UpdateResult githubResult = checkGitHub();
            if (githubResult != null && isNewer(githubResult.version)) {
                consumer.accept(githubResult);
            } else {
                consumer.accept(null);
            }
        });
    }

    private UpdateResult checkGitHub() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(githubUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "ItemTimer-Plugin");

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                String json = response.toString();
                String version = json.split("\"tag_name\":\"")[1].split("\"")[0];
                String url = json.split("\"html_url\":\"")[1].split("\"")[0];
                return new UpdateResult(version, url, "GitHub");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isNewer(String remoteVersion) {
        String current = plugin.getDescription().getVersion().replace("-SNAPSHOT", "").replace("v", "");
        String remote = remoteVersion.replace("v", "").replace("-SNAPSHOT", "");
        
        String[] currentParts = current.split("\\.");
        String[] remoteParts = remote.split("\\.");
        
        int length = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            String cPart = i < currentParts.length ? currentParts[i].replaceAll("[^0-9]", "") : "";
            String rPart = i < remoteParts.length ? remoteParts[i].replaceAll("[^0-9]", "") : "";
            
            int c = !cPart.isEmpty() ? Integer.parseInt(cPart) : 0;
            int r = !rPart.isEmpty() ? Integer.parseInt(rPart) : 0;
            
            if (r > c) return true;
            if (c > r) return false;
        }
        return false;
    }

    public static class UpdateResult {
        public final String version;
        public final String url;
        public final String source;

        public UpdateResult(String version, String url, String source) {
            this.version = version;
            this.url = url;
            this.source = source;
        }
    }
}
