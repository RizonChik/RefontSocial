package ru.rizonchik.refontsocial.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.NumberUtil;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ReputationExpansion extends PlaceholderExpansion {

    private final RefontSocial plugin;

    public ReputationExpansion(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "refontsocial";
    }

    @Override
    public String getAuthor() {
        return "rizonchik";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return null;

        String p = params.toLowerCase(Locale.ROOT).trim();
        String notFound = plugin.getConfig().getString("placeholders.notFound", "не найден");

        if (player == null || player.getUniqueId() == null) {
            return notFound;
        }

        UUID uuid = player.getUniqueId();

        if (p.equals("score") || p.equals("likes") || p.equals("dislikes") || p.equals("votes") || p.equals("rank")) {
            PlayerRep rep = plugin.getReputationService().getOrCreate(uuid, player.getName() != null ? player.getName() : "Игрок");

            if (p.equals("score")) return NumberUtil.formatScore(plugin, rep.getScore());
            if (p.equals("likes")) return String.valueOf(rep.getLikes());
            if (p.equals("dislikes")) return String.valueOf(rep.getDislikes());
            if (p.equals("votes")) return String.valueOf(rep.getVotes());

            int rank = plugin.getReputationService().getRankCached(uuid);
            return rank > 0 ? String.valueOf(rank) : notFound;
        }

        TopQuery q = parseTopQuery(p);
        if (q == null) return null;

        int maxN = plugin.getConfig().getInt("placeholders.topMax", 200);
        if (maxN < 1) maxN = 1;
        if (q.place < 1 || q.place > maxN) return notFound;

        List<PlayerRep> top = plugin.getReputationService().getTopCached(TopCategory.SCORE, maxN, 0);
        if (top == null || top.size() < q.place) return notFound;

        PlayerRep rep = top.get(q.place - 1);

        String name = rep.getName();
        if (name == null || name.trim().isEmpty()) {
            name = plugin.getReputationService().getNameCached(rep.getUuid());
        }
        if (name == null || name.trim().isEmpty()) {
            name = notFound;
        }

        switch (q.field) {
            case NAME:
                return name;
            case SCORE:
                return NumberUtil.formatScore(plugin, rep.getScore());
            case LIKES:
                return String.valueOf(rep.getLikes());
            case DISLIKES:
                return String.valueOf(rep.getDislikes());
            case VOTES:
                return String.valueOf(rep.getVotes());
            default:
                return null;
        }
    }

    private static final class TopQuery {
        private final Field field;
        private final int place;

        private TopQuery(Field field, int place) {
            this.field = field;
            this.place = place;
        }
    }

    private enum Field {
        NAME,
        SCORE,
        LIKES,
        DISLIKES,
        VOTES
    }

    private TopQuery parseTopQuery(String p) {
        int idx = p.lastIndexOf('_');
        if (idx <= 0 || idx >= p.length() - 1) return null;

        String left = p.substring(0, idx);
        String right = p.substring(idx + 1);

        int n;
        try {
            n = Integer.parseInt(right);
        } catch (Exception e) {
            return null;
        }

        Field field;

        if (left.equals("nick") || left.equals("name")) {
            field = Field.NAME;
        } else if (left.equals("score")) {
            field = Field.SCORE;
        } else if (left.equals("like") || left.equals("likes")) {
            field = Field.LIKES;
        } else if (left.equals("dislike") || left.equals("dislikes")) {
            field = Field.DISLIKES;
        } else if (left.equals("votes")) {
            field = Field.VOTES;
        } else {
            return null;
        }

        return new TopQuery(field, n);
    }
}
