package ru.rizonchik.refontsocial.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberUtil {

    private NumberUtil() {
    }

    public static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static double defaultScore(JavaPlugin plugin) {
        double d = plugin.getConfig().getDouble("rating.defaultScore", 5.0);
        return d;
    }

    public static double computeScore(JavaPlugin plugin, int likes, int dislikes) {
        double min = plugin.getConfig().getDouble("rating.scale.min", 0.0);
        double max = plugin.getConfig().getDouble("rating.scale.max", 10.0);
        double def = defaultScore(plugin);

        int l = Math.max(0, likes);
        int d = Math.max(0, dislikes);
        int votes = l + d;

        if (votes <= 0) return clamp(def, min, max);

        String algo = plugin.getConfig().getString("rating.algorithm", "BAYESIAN");
        if (algo == null) algo = "BAYESIAN";
        algo = algo.toUpperCase(java.util.Locale.ROOT);

        if (algo.equals("SIMPLE_RATIO")) {
            double ratio = (double) l / (double) votes;
            double score = min + (max - min) * ratio;
            return clamp(score, min, max);
        }

        int priorVotes = plugin.getConfig().getInt("rating.bayesian.priorVotes", 12);
        if (priorVotes < 0) priorVotes = 0;

        double defRatio;
        if (max - min <= 0.0) {
            defRatio = 0.5;
        } else {
            defRatio = (clamp(def, min, max) - min) / (max - min);
        }

        double priorLikes = priorVotes * defRatio;
        double ratio = (l + priorLikes) / (votes + (double) priorVotes);

        double score = min + (max - min) * ratio;
        return clamp(score, min, max);
    }

    public static String formatScore(JavaPlugin plugin, double score) {
        String pattern = plugin.getConfig().getString("rating.format", "#0.0");
        DecimalFormat df = new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.US));
        return df.format(score);
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static long startOfTodayMillis() {
        long now = System.currentTimeMillis();
        long day = 24L * 60L * 60L * 1000L;
        return now - (now % day);
    }
}