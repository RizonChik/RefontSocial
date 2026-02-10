package ru.rizonchik.refontsocial.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class YamlUtil {

    private static YamlConfiguration MESSAGES;
    private static YamlConfiguration GUI;
    private static YamlConfiguration TAGS;

    private YamlUtil() {
    }

    public static void saveResourceIfNotExists(JavaPlugin plugin, String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) {
            plugin.saveResource(name, false);
        }
    }

    public static void reloadMessages(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        MESSAGES = load(f);
    }

    public static void reloadGui(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "gui.yml");
        GUI = load(f);
    }

    public static void reloadTags(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "tags.yml");
        TAGS = load(f);
    }

    public static YamlConfiguration messages(JavaPlugin plugin) {
        if (MESSAGES == null) reloadMessages(plugin);
        return MESSAGES;
    }

    public static YamlConfiguration gui(JavaPlugin plugin) {
        if (GUI == null) reloadGui(plugin);
        return GUI;
    }

    public static YamlConfiguration tags(JavaPlugin plugin) {
        if (TAGS == null) reloadTags(plugin);
        return TAGS;
    }

    public static YamlConfiguration load(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        try {
            if (file.getName().equalsIgnoreCase("messages.yml")) {
                InputStreamReader reader = new InputStreamReader(
                        YamlUtil.class.getClassLoader().getResourceAsStream("messages.yml"),
                        StandardCharsets.UTF_8
                );
                if (reader != null) {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
                    cfg.setDefaults(def);
                }
            }
            if (file.getName().equalsIgnoreCase("gui.yml")) {
                InputStreamReader reader = new InputStreamReader(
                        YamlUtil.class.getClassLoader().getResourceAsStream("gui.yml"),
                        StandardCharsets.UTF_8
                );
                if (reader != null) {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
                    cfg.setDefaults(def);
                }
            }
            if (file.getName().equalsIgnoreCase("tags.yml")) {
                InputStreamReader reader = new InputStreamReader(
                        YamlUtil.class.getClassLoader().getResourceAsStream("tags.yml"),
                        StandardCharsets.UTF_8
                );
                if (reader != null) {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
                    cfg.setDefaults(def);
                }
            }
        } catch (Exception ignored) {
        }

        return cfg;
    }

    public static void save(File file, YamlConfiguration cfg) {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
