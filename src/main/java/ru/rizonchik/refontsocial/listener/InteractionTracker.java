package ru.rizonchik.refontsocial.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionTracker implements Listener {

    private final JavaPlugin plugin;

    private final Map<UUID, Map<UUID, Long>> nearSince = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> lastInteraction = new ConcurrentHashMap<>();

    private int taskId = -1;

    public InteractionTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        double radius = plugin.getConfig().getDouble("antiAbuse.requireInteraction.radiusBlocks", 7.0);
        int requiredSeconds = plugin.getConfig().getInt("antiAbuse.requireInteraction.requiredSecondsNear", 8);
        long period = plugin.getConfig().getLong("antiAbuse.requireInteraction.taskPeriodTicks", 40L);
        if (period < 20L) period = 20L;

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tick(radius, requiredSeconds), period, period);
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        nearSince.clear();
        lastInteraction.clear();
    }

    private void tick(double radius, int requiredSeconds) {
        long now = System.currentTimeMillis();
        long requiredMs = Math.max(1, requiredSeconds) * 1000L;

        for (Player p : Bukkit.getOnlinePlayers()) {
            Map<UUID, Long> map = nearSince.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>());

            List<Player> nearby = new ArrayList<>();
            for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
                if (e instanceof Player) nearby.add((Player) e);
            }

            Set<UUID> alive = new HashSet<>();
            for (Player other : nearby) {
                if (other == null || !other.isOnline()) continue;
                if (other.getUniqueId().equals(p.getUniqueId())) continue;

                UUID a = p.getUniqueId();
                UUID b = other.getUniqueId();
                alive.add(b);

                Long since = map.get(b);
                if (since == null) {
                    map.put(b, now);
                    continue;
                }

                if (now - since >= requiredMs) {
                    lastInteraction
                            .computeIfAbsent(a, k -> new ConcurrentHashMap<>())
                            .put(b, now);
                }
            }

            map.keySet().removeIf(uuid -> !alive.contains(uuid));
        }
    }

    public boolean hasRecentInteraction(UUID voter, UUID target, long validMs) {
        Map<UUID, Long> map = lastInteraction.get(voter);
        if (map == null) return false;
        Long t = map.get(target);
        if (t == null) return false;
        return System.currentTimeMillis() - t <= validMs;
    }
}