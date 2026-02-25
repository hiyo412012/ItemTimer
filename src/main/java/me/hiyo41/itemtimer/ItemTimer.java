package me.hiyo41.itemtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemTimer extends JavaPlugin implements CommandExecutor, Listener {

    private ConfigManager configManager;
    private ItemTracker itemTracker;
    private long startTime;
    private UpdateChecker.UpdateResult latestUpdate = null;

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        this.configManager = new ConfigManager(this);
        this.itemTracker = new ItemTracker(this, configManager);

        getServer().getPluginManager().registerEvents(itemTracker, this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("itemtimer").setExecutor(this);

        if (configManager.isCheckUpdate()) {
            checkUpdate(null);
        }

        getLogger().info("ItemTimer v" + getDescription().getVersion() + " by Hiyo41 đã được kích hoạt!");
    }

    private void checkUpdate(CommandSender sender) {
        new UpdateChecker(this).checkUpdate(result -> {
            this.latestUpdate = result;
            if (result != null) {
                String msg = color(configManager.getPrefix() + configManager.getMessage("commands.update-found")
                        .replace("{version}", result.version)
                        .replace("{url}", result.url));
                
                if (sender != null) {
                    sender.sendMessage(msg);
                } else {
                    getLogger().warning(msg);
                }
            } else if (sender != null) {
                sender.sendMessage(color(configManager.getPrefix() + configManager.getMessage("commands.update-latest")));
            }
        });
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (latestUpdate != null && player.hasPermission("itemtimer.admin")) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                String msg = color(configManager.getMessage("commands.update-join-notify")
                        .replace("{version}", latestUpdate.version)
                        .replace("{url}", latestUpdate.url));
                player.sendMessage(color(configManager.getPrefix() + msg));
            }, 40L); // Delay 2 seconds to make sure chat is stable
        }
    }

    @Override
    public void onDisable() {
        if (itemTracker != null) {
            itemTracker.cleanup();
        }
        getLogger().info("ItemTimer đã bị vô hiệu hóa.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("itemtimer.admin")) {
                    sendMessage(sender, "commands.no-permission");
                    return true;
                }
                configManager.loadFiles();
                itemTracker.reload();
                sendMessage(sender, "commands.reload");
                break;

            case "status":
                sendStatus(sender);
                break;

            case "check":
                if (!sender.hasPermission("itemtimer.admin")) {
                    sendMessage(sender, "commands.no-permission");
                    return true;
                }
                checkUpdate(sender);
                break;

            case "help":
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendMessage(CommandSender sender, String path) {
        String msg = configManager.getMessage(path);
        String prefix = configManager.getPrefix();
        sender.sendMessage(color(prefix + msg));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color(configManager.getMessage("commands.help.header")));
        sender.sendMessage(color(configManager.getMessage("commands.help.reload")));
        sender.sendMessage(color(configManager.getMessage("commands.help.status")));
        sender.sendMessage(color(color("&f/it check &7- Kiểm tra cập nhật thủ công")));
        sender.sendMessage(color(configManager.getMessage("commands.help.footer")));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(color(configManager.getMessage("commands.status.header")));
        sender.sendMessage(color(configManager.getMessage("commands.status.version")).replace("{version}", getDescription().getVersion()));
        sender.sendMessage(color(configManager.getMessage("commands.status.author")));
        sender.sendMessage(color(configManager.getMessage("commands.status.tracked-items")).replace("{amount}", String.valueOf(itemTracker.getTrackedCount())));
        sender.sendMessage(color(configManager.getMessage("commands.status.uptime")).replace("{uptime}", getUptime()));
        sender.sendMessage(color(configManager.getMessage("commands.status.footer")));
    }

    private String getUptime() {
        long diff = System.currentTimeMillis() - startTime;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
