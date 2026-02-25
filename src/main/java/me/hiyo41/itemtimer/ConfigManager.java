package me.hiyo41.itemtimer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {
    private final ItemTimer plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    // Feature Toggles
    private boolean displayNameTags;
    private boolean translateItemNames;
    private boolean useCustomName;
    private boolean pulsingEnabled;
    private boolean progressBarEnabled;
    private boolean itemRemovalEnabled;
    private boolean checkUpdate;

    // Settings
    private int defaultTimer;
    private Map<String, Integer> customTimers = new HashMap<>();
    private List<String> enabledWorlds;
    private List<String> disabledWorlds;
    private List<String> excludedItems;
    private List<String> whitelistItems;

    // Pulse/Progress
    private int pulsingStartAt;
    private String pulsingColor;
    private String progressSymbol;
    private int progressSize;
    private String progressFullColor;
    private String progressEmptyColor;

    public ConfigManager(ItemTimer plugin) {
        this.plugin = plugin;
        loadFiles();
    }

    public void loadFiles() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        checkVersions();
        parseConfig();
    }

    private void checkVersions() {
        double currentConfigVer = config.getDouble("config-version", 0.0);
        double currentMessagesVer = messages.getDouble("messages-version", 0.0);

        // Simple update logic: if version is old, we could rename and save new, 
        // but for now we just log a warning or could implement field migration.
        // In a real plugin, we'd copy values from old to new.
        if (currentConfigVer < 1.0) {
            plugin.getLogger().warning("Old config-version detected. Please check for new settings.");
        }
        if (currentMessagesVer < 1.0) {
            plugin.getLogger().warning("Old messages-version detected. Please check for new messages.");
        }
    }

    private void parseConfig() {
        // Features
        checkUpdate = config.getBoolean("check-update", true);
        displayNameTags = config.getBoolean("features.display-name-tags.enable", true);
        translateItemNames = config.getBoolean("features.display-name-tags.translate-item-names", true);
        useCustomName = config.getBoolean("features.display-name-tags.use-custom-name", true);
        pulsingEnabled = config.getBoolean("features.pulsing.enable", true);
        progressBarEnabled = config.getBoolean("features.progress-bar.enable", true);
        itemRemovalEnabled = config.getBoolean("features.item-removal.enable", true);

        // Settings
        defaultTimer = config.getInt("timers.default", 60);
        pulsingStartAt = config.getInt("features.pulsing.start-at", 10);
        pulsingColor = config.getString("features.pulsing.color", "&4");

        progressSymbol = config.getString("features.progress-bar.symbol", "■");
        progressSize = config.getInt("features.progress-bar.size", 10);
        progressFullColor = config.getString("features.progress-bar.full-color", "&a");
        progressEmptyColor = config.getString("features.progress-bar.empty-color", "&7");

        enabledWorlds = config.getStringList("worlds.enabled");
        disabledWorlds = config.getStringList("worlds.disabled");
        excludedItems = config.getStringList("items.excluded");
        whitelistItems = config.getStringList("items.whitelist");

        customTimers.clear();
        if (config.getConfigurationSection("timers.custom") != null) {
            for (String key : config.getConfigurationSection("timers.custom").getKeys(false)) {
                customTimers.put(key.toUpperCase(), config.getInt("timers.custom." + key));
            }
        }
    }

    public String getMessage(String path) {
        String msg = messages.getString(path, "Missing message: " + path);
        return msg;
    }

    public String getDisplayFormat() {
        return messages.getString("display.format", "&c⚠ {time} &f| &e{name} &a{amount}x");
    }

    public boolean useLegacyNames() {
        return messages.getBoolean("display.legacy-name", true);
    }

    // Getters
    public boolean isCheckUpdate() { return checkUpdate; }
    public boolean isDisplayNameTags() { return displayNameTags; }
    public boolean isTranslateItemNames() { return translateItemNames; }
    public boolean isUseCustomName() { return useCustomName; }
    public boolean isPulsingEnabled() { return pulsingEnabled; }
    public boolean isProgressBarEnabled() { return progressBarEnabled; }
    public boolean isItemRemovalEnabled() { return itemRemovalEnabled; }
    public int getDefaultTimer() { return defaultTimer; }
    public int getPulsingStartAt() { return pulsingStartAt; }
    public String getPulsingColor() { return pulsingColor; }
    public String getProgressSymbol() { return progressSymbol; }
    public int getProgressSize() { return progressSize; }
    public String getProgressFullColor() { return progressFullColor; }
    public String getProgressEmptyColor() { return progressEmptyColor; }
    public List<String> getEnabledWorlds() { return enabledWorlds; }
    public List<String> getDisabledWorlds() { return disabledWorlds; }
    public List<String> getExcludedItems() { return excludedItems; }
    public List<String> getWhitelistItems() { return whitelistItems; }
    public Integer getCustomTimer(String material) { return customTimers.get(material.toUpperCase()); }
    
    public String getPrefix() {
        return messages.getString("prefix", "&8[&6ItemTimer&8] &r");
    }
}
