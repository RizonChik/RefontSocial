package ru.rizonchik.refontsocial.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Colors {

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
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}