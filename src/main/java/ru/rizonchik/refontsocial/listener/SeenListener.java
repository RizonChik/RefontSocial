package ru.rizonchik.refontsocial.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.util.SecurityUtil;

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

        String salt = ru.rizonchik.refontsocial.util.SaltStore.getOrCreate(plugin);
        String ipHash = (ip == null) ? null : SecurityUtil.sha256(ip + "|" + salt);

        plugin.getStorage().markSeen(p.getUniqueId(), p.getName(), ipHash);
    }
}