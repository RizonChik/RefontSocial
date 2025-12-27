package ru.rizonchik.refontsocial.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class SaltStore {

    private SaltStore() {
    }

    public static String getOrCreate(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "ip_salt.txt");
        try {
            if (f.exists()) {
                byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
                String s = new String(b, StandardCharsets.UTF_8).trim();
                if (!s.isEmpty()) return s;
            }

            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            String s = Base64.getEncoder().encodeToString(salt);

            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            java.nio.file.Files.write(f.toPath(), s.getBytes(StandardCharsets.UTF_8));
            return s;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load/create ip salt", e);
        }
    }
}