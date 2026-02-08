package ru.rizonchik.refontsocial.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.listener.InteractionTracker;
import ru.rizonchik.refontsocial.storage.Storage;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.NumberUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReputationService {

    private final JavaPlugin plugin;
    private final Storage storage;

    private InteractionTracker interactionTracker;

    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldownGlobal = new ConcurrentHashMap<>();
    private final Map<TopKey, TopCacheEntry> topCache = new ConcurrentHashMap<>();
    private final Map<UUID, RankCacheEntry> rankCache = new ConcurrentHashMap<>();
    private final Map<UUID, NameCacheEntry> nameCache = new ConcurrentHashMap<>();

    public ReputationService(JavaPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void setInteractionTracker(InteractionTracker tracker) {
        this.interactionTracker = tracker;
    }

    public void shutdown() {
        cache.clear();
        cooldownGlobal.clear();
        topCache.clear();
        rankCache.clear();
        nameCache.clear();
    }

    private boolean cacheEnabled() {
        return plugin.getConfig().getBoolean("performance.cache.enabled", true);
    }

    private long cacheExpireMs() {
        int expireSeconds = plugin.getConfig().getInt("performance.cache.expireSeconds", 30);
        if (expireSeconds < 1) expireSeconds = 1;
        return expireSeconds * 1000L;
    }

    private void invalidateCaches(UUID target) {
        if (target != null) {
            cache.remove(target);
            nameCache.remove(target);
        }
        topCache.clear();
        rankCache.clear();
    }

    public PlayerRep getOrCreate(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        boolean cacheEnabled = cacheEnabled();
        long expireMs = cacheExpireMs();

        if (cacheEnabled) {
            CacheEntry entry = cache.get(uuid);
            if (entry != null && now - entry.time <= expireMs) {
                return entry.rep;
            }
        }

        PlayerRep rep = storage.getOrCreate(uuid, name);
        if (cacheEnabled) cache.put(uuid, new CacheEntry(rep, now));
        return rep;
    }

    public String getName(UUID uuid) {
        return getNameCached(uuid);
    }

    public List<PlayerRep> getTop(int limit, int offset) {
        return getTopCached(TopCategory.SCORE, limit, offset);
    }

    public String getNameCached(UUID uuid) {
        if (!cacheEnabled()) return storage.getLastKnownName(uuid);

        long now = System.currentTimeMillis();
        long expireMs = cacheExpireMs();

        NameCacheEntry entry = nameCache.get(uuid);
        if (entry != null && now - entry.time <= expireMs) {
            return entry.name;
        }

        String name = storage.getLastKnownName(uuid);
        nameCache.put(uuid, new NameCacheEntry(name, now));
        return name;
    }

    public int getRankCached(UUID uuid) {
        if (!cacheEnabled()) return storage.getRank(uuid);

        long now = System.currentTimeMillis();
        long expireMs = cacheExpireMs();

        RankCacheEntry entry = rankCache.get(uuid);
        if (entry != null && now - entry.time <= expireMs) {
            return entry.rank;
        }

        int rank = storage.getRank(uuid);
        rankCache.put(uuid, new RankCacheEntry(rank, now));
        return rank;
    }

    public List<PlayerRep> getTopCached(TopCategory category, int limit, int offset) {
        if (limit <= 0) return Collections.emptyList();
        if (!cacheEnabled()) return storage.getTop(category, limit, offset);

        long now = System.currentTimeMillis();
        long expireMs = cacheExpireMs();

        int safeOffset = Math.max(0, offset);
        TopKey key = new TopKey(category, limit, safeOffset);

        TopCacheEntry entry = topCache.get(key);
        if (entry != null && now - entry.time <= expireMs) {
            return entry.list;
        }

        List<PlayerRep> list = storage.getTop(category, limit, safeOffset);
        List<PlayerRep> cached = Collections.unmodifiableList(new ArrayList<>(list));
        topCache.put(key, new TopCacheEntry(cached, now));
        return cached;
    }

    public void sendShow(Player viewer, UUID target, String targetName) {
        if (viewer == null || target == null) return;

        UUID viewerId = viewer.getUniqueId();
        String targetDisplay = targetName != null ? targetName : "Игрок";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerRep rep = getOrCreate(target, targetDisplay);
            String score = NumberUtil.formatScore(plugin, rep.getScore());
            String likes = String.valueOf(rep.getLikes());
            String dislikes = String.valueOf(rep.getDislikes());
            String votes = String.valueOf(rep.getVotes());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!viewer.isOnline()) return;
                String key = viewerId.equals(target) ? "showSelf" : "showOther";
                viewer.sendMessage(Colors.msg(plugin, key,
                        "%target%", targetDisplay,
                        "%score%", score,
                        "%likes%", likes,
                        "%dislikes%", dislikes,
                        "%votes%", votes
                ));
            });
        });
    }

    public boolean shouldShowVoterName(Player viewer) {
        String mode = plugin.getConfig().getString("profile.history.showVoterNameMode", "PERMISSION");
        if (mode == null) mode = "PERMISSION";
        mode = mode.toUpperCase(java.util.Locale.ROOT);

        if (mode.equals("ALWAYS")) return true;
        if (mode.equals("ANONYMOUS")) return false;

        String perm = plugin.getConfig().getString("profile.history.showVoterNamePermission", "refontsocial.admin");
        if (perm == null || perm.trim().isEmpty()) perm = "refontsocial.admin";

        return viewer != null && viewer.hasPermission(perm);
    }

    private void sendMessageSync(Player voter, String key, String... placeholders) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!voter.isOnline()) return;
            voter.sendMessage(Colors.msg(plugin, key, placeholders));
        });
    }

    private void voteAsync(Player voter, UUID target, String targetName, boolean like, String reasonTagKey, String fallbackName) {
        if (voter == null || target == null) return;

        boolean preventSelf = plugin.getConfig().getBoolean("antiAbuse.preventSelfVote", true);
        if (preventSelf && voter.getUniqueId().equals(target)) {
            sendMessageSync(voter, "selfVoteDenied");
            return;
        }

        boolean requireHasPlayedBefore = plugin.getConfig().getBoolean("antiAbuse.targetEligibility.requireHasPlayedBefore", true);
        boolean requireTargetOnline = plugin.getConfig().getBoolean("antiAbuse.targetEligibility.requireTargetOnline", false);

        OfflinePlayer off = Bukkit.getOfflinePlayer(target);

        if (requireTargetOnline) {
            if (off == null || !off.isOnline()) {
                sendMessageSync(voter, "targetMustBeOnline");
                return;
            }
        }

        if (requireHasPlayedBefore) {
            boolean played = false;
            try {
                played = (off != null && off.hasPlayedBefore());
            } catch (Throwable ignored) {
            }

            if (!played && (off == null || !off.isOnline())) {
                sendMessageSync(voter, "targetNeverPlayed");
                return;
            }
        }

        boolean bypassCooldown = voter.hasPermission("refontsocial.bypass.cooldown");
        boolean bypassInteraction = voter.hasPermission("refontsocial.bypass.interaction");
        boolean bypassIp = voter.hasPermission("refontsocial.bypass.ip");

        boolean reasonsEnabled = plugin.getConfig().getBoolean("reasons.enabled", true);
        boolean requireReason = plugin.getConfig().getBoolean("reasons.requireReason", false);
        if (reasonTagKey == null && reasonsEnabled && requireReason) {
            sendMessageSync(voter, "reasonRequired");
            return;
        }

        UUID voterId = voter.getUniqueId();
        String repName = targetName != null ? targetName : fallbackName;
        boolean requireInteraction = plugin.getConfig().getBoolean("antiAbuse.requireInteraction.enabled", true);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();

            if (!bypassCooldown) {
                int globalCd = plugin.getConfig().getInt("antiAbuse.cooldowns.voteGlobalSeconds", 20);
                if (globalCd > 0) {
                    String key = voterId.toString();
                    Long last = cooldownGlobal.get(key);
                    if (last != null) {
                        long left = (last + globalCd * 1000L) - now;
                        if (left > 0) {
                            sendMessageSync(voter, "cooldownGlobal", "%seconds%", String.valueOf(left / 1000L + 1));
                            return;
                        }
                    }
                }
            }

            if (!bypassInteraction && requireInteraction && interactionTracker != null) {
                long validSeconds = plugin.getConfig().getLong("antiAbuse.requireInteraction.interactionValidSeconds", 600);
                long validMs = Math.max(1, validSeconds) * 1000L;
                if (!interactionTracker.hasRecentInteraction(voterId, target, validMs)) {
                    sendMessageSync(voter, "interactionRequired");
                    return;
                }
            }

            boolean dailyLimit = plugin.getConfig().getBoolean("antiAbuse.dailyLimit.enabled", true);
            if (!bypassCooldown && dailyLimit) {
                int maxPerDay = plugin.getConfig().getInt("antiAbuse.dailyLimit.maxVotesPerDay", 20);
                if (maxPerDay > 0) {
                    int used = storage.countVotesByVoterSince(voterId, NumberUtil.startOfTodayMillis());
                    if (used >= maxPerDay) {
                        sendMessageSync(voter, "dailyLimit", "%limit%", String.valueOf(maxPerDay));
                        return;
                    }
                }
            }

            Storage.VoteState state = storage.getVoteState(voterId, target);

            boolean ipProtection = plugin.getConfig().getBoolean("antiAbuse.ipProtection.enabled", false);
            if (ipProtection && !bypassIp) {
                String voterIp = storage.getIpHash(voterId);
                String targetIp = storage.getIpHash(target);

                if (voterIp != null && targetIp != null && voterIp.equals(targetIp)) {
                    String mode = plugin.getConfig().getString("antiAbuse.ipProtection.mode", "SAME_IP_DENY");
                    if (mode == null) mode = "SAME_IP_DENY";
                    mode = mode.toUpperCase(java.util.Locale.ROOT);

                    if (mode.equals("SAME_IP_DENY")) {
                        sendMessageSync(voter, "ipDenied");
                        return;
                    }

                    long cd = plugin.getConfig().getLong("antiAbuse.ipProtection.cooldownSeconds", 86400);
                    if (cd < 1) cd = 1;
                    long cdMs = cd * 1000L;

                    if (state != null && state.lastTime != null) {
                        long left = (state.lastTime + cdMs) - now;
                        if (left > 0) {
                            sendMessageSync(voter, "ipCooldown", "%seconds%", String.valueOf(left / 1000L + 1));
                            return;
                        }
                    }
                }
            }

            if (!bypassCooldown && state != null) {
                int sameTargetCd = plugin.getConfig().getInt("antiAbuse.cooldowns.sameTargetSeconds", 600);
                if (sameTargetCd > 0) {
                    long left = (state.lastTime + sameTargetCd * 1000L) - now;
                    if (left > 0) {
                        sendMessageSync(voter, "cooldownTarget", "%seconds%", String.valueOf(left / 1000L + 1));
                        return;
                    }
                }

                if (state.value != null && state.value != (like ? 1 : 0)) {
                    int changeVoteCd = plugin.getConfig().getInt("antiAbuse.cooldowns.changeVoteSeconds", 1800);
                    if (changeVoteCd > 0) {
                        long left = (state.lastTime + changeVoteCd * 1000L) - now;
                        if (left > 0) {
                            sendMessageSync(voter, "cooldownChangeVote", "%seconds%", String.valueOf(left / 1000L + 1));
                            return;
                        }
                    }
                }
            }

            if (!bypassCooldown) {
                int globalCd = plugin.getConfig().getInt("antiAbuse.cooldowns.voteGlobalSeconds", 20);
                if (globalCd > 0) {
                    cooldownGlobal.put(voterId.toString(), now);
                }
            }

            Storage.VoteResult result = storage.applyVote(
                    voterId,
                    target,
                    like ? 1 : 0,
                    now,
                    targetName,
                    reasonTagKey
            );

            invalidateCaches(target);

            PlayerRep rep = getOrCreate(target, repName);
            String score = NumberUtil.formatScore(plugin, rep.getScore());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!voter.isOnline()) return;

                String safeTargetName = safeName(target, targetName);

                if (result == Storage.VoteResult.CREATED) {
                    voter.sendMessage(Colors.msg(plugin, like ? "voteLikeDone" : "voteDislikeDone",
                            "%target%", safeTargetName,
                            "%score%", score
                    ));
                } else if (result == Storage.VoteResult.CHANGED) {
                    voter.sendMessage(Colors.msg(plugin, "voteChanged",
                            "%target%", safeTargetName,
                            "%score%", score
                    ));
                } else {
                    voter.sendMessage(Colors.msg(plugin, "voteRemoved",
                            "%target%", safeTargetName,
                            "%score%", score
                    ));
                }

                if (reasonTagKey != null) {
                    String display = plugin.getConfig().getString("reasons.tags." + reasonTagKey, reasonTagKey);
                    voter.sendMessage(Colors.msg(plugin, "reasonSaved", "%reason%", display));
                }
            });
        });
    }

    public void voteWithReason(Player voter, UUID target, String targetName, boolean like, String reasonTagKey) {
        voteAsync(voter, target, targetName, like, reasonTagKey, "Игрок");
    }

    public void vote(Player voter, UUID target, String targetName, boolean like) {
        voteAsync(voter, target, targetName, like, null, "Игрок");
    }

    private String safeName(UUID uuid, String name) {
        if (name != null && !name.trim().isEmpty()) return name;
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        if (off != null && off.getName() != null) return off.getName();
        return uuid.toString().substring(0, 8);
    }

    private static final class CacheEntry {
        private final PlayerRep rep;
        private final long time;

        private CacheEntry(PlayerRep rep, long time) {
            this.rep = rep;
            this.time = time;
        }
    }

    private static final class NameCacheEntry {
        private final String name;
        private final long time;

        private NameCacheEntry(String name, long time) {
            this.name = name;
            this.time = time;
        }
    }

    private static final class RankCacheEntry {
        private final int rank;
        private final long time;

        private RankCacheEntry(int rank, long time) {
            this.rank = rank;
            this.time = time;
        }
    }

    private static final class TopCacheEntry {
        private final List<PlayerRep> list;
        private final long time;

        private TopCacheEntry(List<PlayerRep> list, long time) {
            this.list = list;
            this.time = time;
        }
    }

    private static final class TopKey {
        private final TopCategory category;
        private final int limit;
        private final int offset;

        private TopKey(TopCategory category, int limit, int offset) {
            this.category = category;
            this.limit = limit;
            this.offset = offset;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopKey topKey = (TopKey) o;
            if (limit != topKey.limit) return false;
            if (offset != topKey.offset) return false;
            return category == topKey.category;
        }

        @Override
        public int hashCode() {
            int result = category != null ? category.hashCode() : 0;
            result = 31 * result + limit;
            result = 31 * result + offset;
            return result;
        }
    }
}
