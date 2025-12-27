package ru.rizonchik.refontsocial.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static ItemStack fromGui(JavaPlugin plugin, String key, String... replace) {
        YamlConfiguration gui = YamlUtil.gui(plugin);

        String base = "items." + key;
        String matName = gui.getString(base + ".material", "PAPER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.PAPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = gui.getString(base + ".name", " ");
        name = apply(name, replace);
        meta.setDisplayName(Colors.color(name));

        List<String> loreRaw = gui.getStringList(base + ".lore");
        if (loreRaw != null && !loreRaw.isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String s : loreRaw) {
                lore.add(Colors.color(apply(s, replace)));
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static String apply(String s, String... replace) {
        if (s == null) return "";
        if (replace == null) return s;
        for (int i = 0; i + 1 < replace.length; i += 2) {
            s = s.replace(replace[i], replace[i + 1]);
        }
        return s;
    }
}