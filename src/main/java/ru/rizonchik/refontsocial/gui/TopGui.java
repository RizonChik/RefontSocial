package ru.rizonchik.refontsocial.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;
import ru.rizonchik.refontsocial.util.YamlUtil;
import xyz.overdyn.dyngui.abstracts.AbstractGuiLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TopGui extends AbstractGuiLayer {

    private final RefontSocial plugin;
    private final ReputationService service;
    private final int page;

    public TopGui(RefontSocial plugin, ReputationService service, int page) {
        this.plugin = plugin;
        this.service = service;
        this.page = page;
    }

    @Override
    public void open(Player player) {
        String title = plugin.getConfig().getString("gui.top.title", "Reputation • Top");
        int size = plugin.getConfig().getInt("gui.top.size", 54);
        if (size < 9) size = 54;
        if (size % 9 != 0) size = 54;

        inventory = Bukkit.createInventory(null, size, title);
        fillFrame();

        int pageSize = plugin.getConfig().getInt("gui.top.pageSize", 45);
        if (pageSize < 1) pageSize = 45;

        int offset = (page - 1) * pageSize;
        List<PlayerRep> top = service.getTop(pageSize, offset);

        for (int i = 0; i < top.size(); i++) {
            PlayerRep rep = top.get(i);
            int slot = i;
            if (slot >= pageSize) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            String name = service.getName(rep.getUuid());
            if (name == null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(rep.getUuid());
                name = off != null ? off.getName() : null;
            }
            if (name == null) name = rep.getUuid().toString().substring(0, 8);

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
            inventory.setItem(slot, head);
        }

        inventory.setItem(inventory.getSize() - 9, ItemUtil.fromGui(plugin, "back"));
        inventory.setItem(inventory.getSize() - 1, ItemUtil.fromGui(plugin, "next"));

        player.openInventory(inventory);
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

            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta == null || meta.getOwningPlayer() == null) return;

            UUID target = meta.getOwningPlayer().getUniqueId();
            String name = meta.getOwningPlayer().getName();
            if (name == null) name = service.getName(target);

            final UUID finalTarget = target;
            final String finalName = (name != null ? name : "Player");

            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuiService().openRate(player, finalTarget, finalName)
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