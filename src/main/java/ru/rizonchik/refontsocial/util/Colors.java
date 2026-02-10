package ru.rizonchik.refontsocial.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Colors {

    private static final Pattern HEX_AMPERSAND = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern HEX_BRACKET = Pattern.compile("(?i)<#([0-9A-F]{6})>");

    private Colors() {
    }

    public static String prefix(JavaPlugin plugin) {
        YamlConfiguration msg = YamlUtil.messages(plugin);
        String p = msg.getString("prefix", "§8[RS] §r");
        return color(p);
    }

    public static String msg(JavaPlugin plugin, String key, String... replace) {
        YamlConfiguration msg = YamlUtil.messages(plugin);
        String s = msg.getString(key, "§cMissing message: " + key);
        s = s.replace("%prefix%", prefix(plugin));
        if (replace != null) {
            for (int i = 0; i + 1 < replace.length; i += 2) {
                s = s.replace(replace[i], replace[i + 1]);
            }
        }
        return color(s);
    }

    public static void sendList(CommandSender sender, JavaPlugin plugin, String key) {
        YamlConfiguration msg = YamlUtil.messages(plugin);
        List<String> list = msg.getStringList(key);
        if (list == null || list.isEmpty()) return;
        for (String s : list) {
            sender.sendMessage(color(s.replace("%prefix%", prefix(plugin))));
        }
    }

    public static String color(String s) {
        if (s == null || s.isEmpty()) return "";

        String withHex = applyHexColors(s);
        return ChatColor.translateAlternateColorCodes('&', withHex);
    }

    private static String applyHexColors(String input) {
        if (input.indexOf('#') < 0) return input;

        String out = replaceHex(input, HEX_BRACKET);
        return replaceHex(out, HEX_AMPERSAND);
    }

    private static String replaceHex(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (int i = 0; i < hex.length(); i++) {
                replacement.append('&').append(hex.charAt(i));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }

        matcher.appendTail(out);
        return out.toString();
    }
}
