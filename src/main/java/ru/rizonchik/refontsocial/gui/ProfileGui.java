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
import ru.rizonchik.refontsocial.storage.model.VoteLogEntry;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;
import xyz.overdyn.dyngui.abstracts.AbstractGuiLayer;

import java.text.SimpleDateFormat;
import java.util.*;

public final class ProfileGui extends AbstractGuiLayer {

    private final RefontSocial plugin;
    private final ReputationService service;
    private final UUID target;
    private final String targetName;

    public ProfileGui(RefontSocial plugin, ReputationService service, UUID target, String targetName) {
        this.plugin = plugin;
        this.service = service;
        this.target = target;
        this.targetName = (targetName != null ? targetName : "Игрок");
    }

    @Override
    public void open(Player viewer) {
        String title = plugin.getConfig().getString("gui.profile.title", "Профиль");
        int size = plugin.getConfig().getInt("gui.profile.size", 54);
        if (size < 9) size = 54;
        if (size % 9 != 0) size = 54;

        inventory = Bukkit.createInventory(null, size, title);

        ItemStack filler = ItemUtil.fromGui(plugin, "filler");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta lm = loading.getItemMeta();
        if (lm != null) {
            lm.setDisplayName("§fЗагрузка профиля...");
            loading.setItemMeta(lm);
        }
        inventory.setItem(22, loading);

        inventory.setItem(inventory.getSize() - 9, ItemUtil.fromGui(plugin, "back"));

        viewer.openInventory(inventory);

        final Player viewerFinal = viewer;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerRep rep = service.getOrCreate(target, targetName);
            int rank = plugin.getStorage().getRank(target);
            String rankStr = (rank > 0)
                    ? String.valueOf(rank)
                    : plugin.getConfig().getString("placeholders.notFound", "не найден");

            int tagLimit = plugin.getConfig().getInt("profile.topTags.limit", 3);
            if (tagLimit < 1) tagLimit = 3;

            Map<String, Integer> topTags = plugin.getStorage().getTopTags(target, tagLimit);

            boolean historyEnabled = plugin.getConfig().getBoolean("profile.history.enabled", true);
            int limit = plugin.getConfig().getInt("profile.history.limit", 10);
            if (limit < 1) limit = 10;

            boolean includeVoter = service.shouldShowVoterName(viewerFinal);
            List<VoteLogEntry> history = historyEnabled
                    ? plugin.getStorage().getRecentVotes(target, limit, includeVoter)
                    : Collections.emptyList();

            final int limitFinal = limit;
            final boolean includeVoterFinal = includeVoter;
            final boolean historyEnabledFinal = historyEnabled;
            final PlayerRep repFinal = rep;
            final String rankStrFinal = rankStr;
            final Map<String, Integer> topTagsFinal = topTags;
            final List<VoteLogEntry> historyFinal = history;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!viewerFinal.isOnline()) return;
                if (viewerFinal.getOpenInventory() == null) return;
                if (viewerFinal.getOpenInventory().getTopInventory() == null) return;
                if (!viewerFinal.getOpenInventory().getTopInventory().equals(inventory)) return;

                for (int i = 0; i < 45; i++) inventory.setItem(i, null);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                if (sm != null) {
                    sm.setDisplayName("§f" + targetName);

                    List<String> lore = new ArrayList<>();
                    lore.add("§7Рейтинг: §f" + NumberUtil.formatScore(plugin, repFinal.getScore()) + " §7(место: §f" + rankStrFinal + "§7)");
                    lore.add("§7Лайки: §a" + repFinal.getLikes() + " §7/ Дизлайки: §c" + repFinal.getDislikes());
                    lore.add("§7Голосов: §f" + repFinal.getVotes());
                    lore.add("");
                    lore.add("§7Теги:");

                    if (topTagsFinal.isEmpty()) {
                        lore.add("§8• §7нет");
                    } else {
                        for (Map.Entry<String, Integer> e : topTagsFinal.entrySet()) {
                            String key = e.getKey();
                            int cnt = e.getValue();
                            String display = plugin.getConfig().getString("reasons.tags." + key, key);
                            lore.add("§8• §f" + display + " §8x§7" + cnt);
                        }
                    }

                    sm.setLore(lore);

                    try {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(target);
                        sm.setOwningPlayer(off);
                    } catch (Throwable ignored) {
                    }

                    head.setItemMeta(sm);
                }

                inventory.setItem(13, head);

                if (historyEnabledFinal) {
                    ItemStack book = new ItemStack(Material.BOOK);
                    ItemMeta bm = book.getItemMeta();
                    if (bm != null) {
                        bm.setDisplayName("§fИстория оценок");
                        List<String> lore = new ArrayList<>();
                        lore.add("§7Последние " + limitFinal + " событий:");
                        lore.add("");

                        SimpleDateFormat df = new SimpleDateFormat("dd.MM HH:mm");
                        if (historyFinal.isEmpty()) {
                            lore.add("§8• §7пусто");
                        } else {
                            for (VoteLogEntry e : historyFinal) {
                                String when = df.format(new Date(e.getTimeMillis()));
                                String sign = (e.getValue() == 1 ? "§a+§7" : "§c-§7");

                                String reason = e.getReason();
                                if (reason != null && !reason.trim().isEmpty()) {
                                    reason = plugin.getConfig().getString("reasons.tags." + reason, reason);
                                } else {
                                    reason = "без причины";
                                }

                                if (includeVoterFinal && e.getVoterName() != null && !e.getVoterName().trim().isEmpty()) {
                                    lore.add("§8• §7" + when + " " + sign + " §f" + e.getVoterName() + " §8— §f" + reason);
                                } else {
                                    lore.add("§8• §7" + when + " " + sign + " §8— §f" + reason);
                                }
                            }
                        }

                        bm.setLore(lore);
                        book.setItemMeta(bm);
                    }

                    inventory.setItem(31, book);
                }
            });
        });
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot == inventory.getSize() - 9) {
            player.closeInventory();
        }
    }
}