package me.hiyo41.itemtimer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemTracker implements Listener {
    private final ItemTimer plugin;
    private final ConfigManager config;
    private final Map<UUID, Integer> itemTimers = new HashMap<>();
    private final Map<UUID, Integer> maxTimers = new HashMap<>();
    private BukkitTask updateTask;

    public ItemTracker(ItemTimer plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        startTask();
    }

    private void startTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            itemTimers.entrySet().removeIf(entry -> {
                Item item = (Item) Bukkit.getEntity(entry.getKey());
                if (item == null || !item.isValid() || item.isDead()) {
                    maxTimers.remove(entry.getKey());
                    return true;
                }

                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    if (config.isItemRemovalEnabled()) {
                        item.remove();
                    }
                    maxTimers.remove(entry.getKey());
                    return true;
                }

                entry.setValue(remaining);
                if (config.isDisplayNameTags()) {
                    updateItemDisplay(item, remaining, maxTimers.get(entry.getKey()));
                }
                return false;
            });
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (!shouldTrack(item)) return;

        int time = getInitialTime(item);
        itemTimers.put(item.getUniqueId(), time);
        maxTimers.put(item.getUniqueId(), time);
        
        if (config.isDisplayNameTags()) {
            updateItemDisplay(item, time, time);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        Item target = event.getTarget();
        Item source = event.getEntity();

        if (itemTimers.containsKey(target.getUniqueId()) && itemTimers.containsKey(source.getUniqueId())) {
            int timeT = itemTimers.get(target.getUniqueId());
            int timeS = itemTimers.get(source.getUniqueId());
            int maxT = maxTimers.get(target.getUniqueId());
            int maxS = maxTimers.get(source.getUniqueId());
            
            int newTime = Math.max(timeT, timeS);
            int newMax = Math.max(maxT, maxS);
            
            itemTimers.put(target.getUniqueId(), newTime);
            maxTimers.put(target.getUniqueId(), newMax);
            
            itemTimers.remove(source.getUniqueId());
            maxTimers.remove(source.getUniqueId());
            
            if (config.isDisplayNameTags()) {
                Bukkit.getScheduler().runTask(plugin, () -> updateItemDisplay(target, newTime, newMax));
            }
        }
    }

    private boolean shouldTrack(Item item) {
        String world = item.getWorld().getName();
        if (!config.getEnabledWorlds().isEmpty() && !config.getEnabledWorlds().contains(world)) return false;
        if (config.getDisabledWorlds().contains(world)) return false;

        String type = item.getItemStack().getType().name();
        if (config.getExcludedItems().contains(type)) return false;
        if (!config.getWhitelistItems().isEmpty() && !config.getWhitelistItems().contains(type)) return false;

        return true;
    }

    private int getInitialTime(Item item) {
        String type = item.getItemStack().getType().name();
        Integer custom = config.getCustomTimer(type);
        return (custom != null) ? custom : config.getDefaultTimer();
    }

    private void updateItemDisplay(Item item, int seconds, int max) {
        ItemStack stack = item.getItemStack();
        Component nameComponent;
        
        if (config.isUseCustomName() && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            nameComponent = stack.getItemMeta().displayName();
        } else if (config.isTranslateItemNames()) {
            nameComponent = Component.translatable(stack.getType().translationKey());
        } else {
            nameComponent = Component.text(capitalize(stack.getType().name().replace("_", " ")));
        }
        
        String format = config.getDisplayFormat();
        String timeStr = String.valueOf(seconds);
        
        boolean pulsing = false;
        if (config.isPulsingEnabled() && seconds <= config.getPulsingStartAt()) {
            if (seconds % 2 == 0) {
                pulsing = true;
            }
        }

        Component timeComp = Component.text(timeStr);
        if (pulsing) {
            timeComp = timeComp.color(NamedTextColor.DARK_RED);
        } else {
            timeComp = timeComp.color(NamedTextColor.RED);
        }

        Component progressComp = config.isProgressBarEnabled() ? getProgressBarComponent(seconds, max) : Component.empty();
        Component amountComp = Component.text(stack.getAmount()).color(NamedTextColor.GREEN);

        // We use a custom parser to replace placeholders in the format string with Components
        Component finalDisplay = parseFormat(format, timeComp, nameComponent, amountComp, progressComp);

        item.customName(finalDisplay);
        item.setCustomNameVisible(true);
    }

    private Component parseFormat(String format, Component time, Component name, Component amount, Component progress) {
        // Simple manual replacement for Adventure
        String[] parts = format.split("(?=\\{)|(?<=\\})");
        Component result = Component.empty();
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

        for (String part : parts) {
            switch (part) {
                case "{time}":
                    result = result.append(time);
                    break;
                case "{name}":
                    result = result.append(name);
                    break;
                case "{amount}":
                    result = result.append(amount);
                    break;
                case "{progress}":
                    result = result.append(progress);
                    break;
                default:
                    // Colorize the literal parts
                    result = result.append(serializer.deserialize(part.replace("{", "").replace("}", "")));
                    break;
            }
        }
        return result;
    }

    private Component getProgressBarComponent(int current, int max) {
        int size = config.getProgressSize();
        if (max <= 0) max = 1;
        int fullCount = (int) Math.round(((double) current / max) * size);
        fullCount = Math.min(size, Math.max(0, fullCount));
        
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
        Component fullPart = Component.text(repeat(config.getProgressSymbol(), fullCount))
                .color(serializer.deserialize(config.getProgressFullColor()).color());
        Component emptyPart = Component.text(repeat(config.getProgressSymbol(), size - fullCount))
                .color(serializer.deserialize(config.getProgressEmptyColor()).color());
                
        return fullPart.append(emptyPart);
    }

    private String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public void reload() {
    }

    public void cleanup() {
        if (updateTask != null) updateTask.cancel();
        itemTimers.clear();
        maxTimers.clear();
    }

    public int getTrackedCount() {
        return itemTimers.size();
    }
}
