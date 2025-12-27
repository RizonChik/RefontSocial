package ru.rizonchik.refontsocial.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.TopCategory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiService implements Listener {

    private final RefontSocial plugin;
    private final ReputationService service;

    private final Map<UUID, AbstractGui> open = new ConcurrentHashMap<>();

    public GuiService(RefontSocial plugin, ReputationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void openTop(Player player, int page) {
        openCategoryTop(player, TopCategory.SCORE, page);
    }

    public void openCategoryTop(Player player, TopCategory category, int page) {
        AbstractGui gui = new CategoryTopGui(plugin, service, category, page);
        open.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    public void openRate(Player player, UUID target, String targetName) {
        AbstractGui gui = new RateGui(plugin, service, target, targetName);
        open.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    public void openProfile(Player player, UUID target, String targetName) {
        AbstractGui gui = new ProfileGui(plugin, service, target, targetName);
        open.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    public void openReasons(Player player, UUID target, String targetName, boolean like) {
        AbstractGui gui = new ReasonsGui(plugin, service, target, targetName, like);
        open.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        AbstractGui gui = open.get(player.getUniqueId());
        if (gui == null) return;

        if (e.getView() == null || e.getView().getTopInventory() == null) return;
        if (!e.getView().getTopInventory().equals(gui.getInventory())) return;

        e.setCancelled(true);
        gui.onClick(player, e.getRawSlot(), e.getCurrentItem());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;

        Player player = (Player) e.getPlayer();
        AbstractGui gui = open.get(player.getUniqueId());
        if (gui == null) return;

        if (e.getInventory() != null && e.getInventory().equals(gui.getInventory())) {
            open.remove(player.getUniqueId());
            gui.onClose(player);
        }
    }

    public void shutdown() {
        for (UUID uuid : open.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.closeInventory();
            }
        }
        open.clear();
    }
}