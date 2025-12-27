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
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CategoryTopGui extends AbstractGui {

    private final RefontSocial plugin;
    private final ReputationService service;
    private final TopCategory category;
    private final int page;

    public CategoryTopGui(RefontSocial plugin, ReputationService service, TopCategory category, int page) {
        this.plugin = plugin;
        this.service = service;
        this.category = category;
        this.page = page;
    }

    @Override
    public void open(Player player) {
        String titleTpl = plugin.getConfig().getString("gui.categoryTop.title", "Топ • %category%");
        String title = titleTpl.replace("%category%", categoryRu(category));

        int size = plugin.getConfig().getInt("gui.categoryTop.size", 54);
        if (size < 9) size = 54;
        if (size % 9 != 0) size = 54;

        inventory = Bukkit.createInventory(null, size, title);

        ItemStack filler = ItemUtil.fromGui(plugin, "filler");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta meta = loading.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fЗагрузка...");
            loading.setItemMeta(meta);
        }
        inventory.setItem(22, loading);

        inventory.setItem(inventory.getSize() - 9, ItemUtil.fromGui(plugin, "back"));
        inventory.setItem(inventory.getSize() - 1, ItemUtil.fromGui(plugin, "next"));

        player.openInventory(inventory);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int pageSize = plugin.getConfig().getInt("gui.categoryTop.pageSize", 45);
            if (pageSize < 1) pageSize = 45;

            int offset = (page - 1) * pageSize;
            List<PlayerRep> top = plugin.getStorage().getTop(category, pageSize, offset);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (player.getOpenInventory() == null) return;
                if (player.getOpenInventory().getTopInventory() == null) return;
                if (!player.getOpenInventory().getTopInventory().equals(inventory)) return;

                for (int i = 0; i < 45; i++) inventory.setItem(i, null);

                for (int i = 0; i < top.size() && i < 45; i++) {
                    PlayerRep rep = top.get(i);

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta sm = (SkullMeta) head.getItemMeta();

                    String name = rep.getName();
                    if (name == null) name = service.getName(rep.getUuid());
                    if (name == null) {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(rep.getUuid());
                        name = off != null ? off.getName() : null;
                    }
                    if (name == null) name = rep.getUuid().toString().substring(0, 8);

                    sm.setDisplayName("§f#" + (offset + i + 1) + " §7— §f" + name);

                    List<String> lore = new ArrayList<>();
                    lore.add("§7Рейтинг: §f" + NumberUtil.formatScore(plugin, rep.getScore()));
                    lore.add("§7Лайки: §a" + rep.getLikes() + " §7/ Дизлайки: §c" + rep.getDislikes());
                    lore.add("§7Голосов: §f" + rep.getVotes());
                    lore.add("");
                    lore.add("§eНажми: открыть профиль");
                    sm.setLore(lore);

                    try {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(rep.getUuid());
                        sm.setOwningPlayer(off);
                    } catch (Throwable ignored) {
                    }

                    head.setItemMeta(sm);
                    inventory.setItem(i, head);
                }
            });
        });
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot == inventory.getSize() - 9) {
            player.closeInventory();
            return;
        }

        if (rawSlot == inventory.getSize() - 1) {
            plugin.getGuiService().openCategoryTop(player, category, page + 1);
            return;
        }

        if (rawSlot < 0 || rawSlot >= 45) return;
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        UUID target = meta.getOwningPlayer().getUniqueId();
        String name = meta.getOwningPlayer().getName();
        if (name == null) name = service.getName(target);

        final UUID finalTarget = target;
        final String finalName = (name != null ? name : "Игрок");

        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiService().openProfile(player, finalTarget, finalName));
    }

    private String categoryRu(TopCategory c) {
        if (c == TopCategory.LIKES) return "Лайки";
        if (c == TopCategory.DISLIKES) return "Дизлайки";
        if (c == TopCategory.VOTES) return "Голоса";
        return "Рейтинг";
    }
}