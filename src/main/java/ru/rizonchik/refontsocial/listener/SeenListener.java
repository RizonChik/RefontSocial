package ru.rizonchik.refontsocial.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.util.SecurityUtil;
import ru.rizonchik.refontsocial.util.SaltStore;

public final class SeenListener implements Listener {

    private final RefontSocial plugin;

    public SeenListener(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String ip = null;
        try {
            if (p.getAddress() != null && p.getAddress().getAddress() != null) {
                ip = p.getAddress().getAddress().getHostAddress();
            }
        } catch (Throwable ignored) {
        }

        final String ipFinal = ip;
        final String name = p.getName();
        final java.util.UUID uuid = p.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String salt = SaltStore.getOrCreate(plugin);
            String ipHash = (ipFinal == null) ? null : SecurityUtil.sha256(ipFinal + "|" + salt);
            plugin.getStorage().markSeen(uuid, name, ipHash);
        });
    }
}
