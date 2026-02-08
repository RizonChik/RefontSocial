package ru.rizonchik.refontsocial.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TopGui extends AbstractGui {

    private final RefontSocial plugin;
    private final ReputationService service;
    private final int page;
    private final Map<Integer, UUID> slotTargets = new HashMap<>();
    private final Map<Integer, String> slotNames = new HashMap<>();

    public TopGui(RefontSocial plugin, ReputationService service, int page) {
        this.plugin = plugin;
        this.service = service;
        this.page = page;
    }

    @Override
    public void open(Player player) {
        String title = plugin.getConfig().getString("gui.top.title", "Репутация • Топ");
        int size = plugin.getConfig().getInt("gui.top.size", 54);
        if (size < 9) size = 54;
        if (size % 9 != 0) size = 54;

        inventory = Bukkit.createInventory(null, size, title);
        fillFrame();

        inventory.setItem(inventory.getSize() - 9, ItemUtil.fromGui(plugin, "back"));
        inventory.setItem(inventory.getSize() - 1, ItemUtil.fromGui(plugin, "next"));

        player.openInventory(inventory);

        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta loadingMeta = loading.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.setDisplayName("§fЗагрузка...");
            loading.setItemMeta(loadingMeta);
        }
        inventory.setItem(22, loading);

        final Player viewer = player;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int pageSize = plugin.getConfig().getInt("gui.top.pageSize", 45);
            if (pageSize < 1) pageSize = 45;
            final int pageSizeFinal = pageSize;

            int offset = (page - 1) * pageSizeFinal;
            List<PlayerRep> top = service.getTopCached(ru.rizonchik.refontsocial.storage.TopCategory.SCORE, pageSizeFinal, offset);
            List<String> names = new ArrayList<>(top.size());

            for (PlayerRep rep : top) {
                String name = rep.getName();
                if (name == null || name.trim().isEmpty()) {
                    name = service.getNameCached(rep.getUuid());
                }
                if (name == null || name.trim().isEmpty()) {
                    name = rep.getUuid().toString().substring(0, 8);
                }
                names.add(name);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!viewer.isOnline()) return;
                if (viewer.getOpenInventory() == null) return;
                if (viewer.getOpenInventory().getTopInventory() == null) return;
                if (!viewer.getOpenInventory().getTopInventory().equals(inventory)) return;

                for (int i = 0; i < pageSizeFinal; i++) inventory.setItem(i, null);
                slotTargets.clear();
                slotNames.clear();

                for (int i = 0; i < top.size() && i < pageSizeFinal; i++) {
                    PlayerRep rep = top.get(i);
                    String name = names.size() > i ? names.get(i) : rep.getUuid().toString().substring(0, 8);

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();

                    meta.setDisplayName("§f#" + (offset + i + 1) + " §7— §f" + name);

                    List<String> lore = new ArrayList<>();
                    lore.add("§7Рейтинг: §f" + NumberUtil.formatScore(plugin, rep.getScore()));
                    lore.add("§7Лайки: §a" + rep.getLikes() + " §7/ Дизлайки: §c" + rep.getDislikes());
                    lore.add("§7Голосов: §f" + rep.getVotes());
                    lore.add("");
                    lore.add("§eНажми, чтобы оценить");

                    meta.setLore(lore);

                    try {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(rep.getUuid());
                        meta.setOwningPlayer(off);
                    } catch (Throwable ignored) {
                    }

                    head.setItemMeta(meta);
                    inventory.setItem(i, head);
                    slotTargets.put(i, rep.getUuid());
                    slotNames.put(i, name);
                }
            });
        });
    }

    private void fillFrame() {
        ItemStack filler = ItemUtil.fromGui(plugin, "filler");
        for (int i = 45; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot < 0) return;

        int size = inventory.getSize();
        int pageSize = plugin.getConfig().getInt("gui.top.pageSize", 45);
        if (pageSize < 1) pageSize = 45;

        if (rawSlot < pageSize) {
            ItemStack item = inventory.getItem(rawSlot);
            if (item == null || item.getType() != Material.PLAYER_HEAD) return;
            if (!slotTargets.containsKey(rawSlot)) return;

            UUID target = slotTargets.get(rawSlot);
            String name = slotNames.get(rawSlot);

            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuiService().openRate(player, target, name)
            );
            return;
        }

        if (rawSlot == size - 9) {
            int newPage = page - 1;
            if (newPage < 1) newPage = 1;
            plugin.getGuiService().openTop(player, newPage);
            return;
        }

        if (rawSlot == size - 1) {
            plugin.getGuiService().openTop(player, page + 1);
        }
    }
}
